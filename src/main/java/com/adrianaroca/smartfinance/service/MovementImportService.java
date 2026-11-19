package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class MovementImportService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 2000;
    private static final DateTimeFormatter SPANISH_DATE = DateTimeFormatter.ofPattern("d/M/uuuu");

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CategoryService categoryService;

    public MovementImportService(IncomeRepository incomeRepository,
                                 ExpenseRepository expenseRepository,
                                 CurrentUserProvider currentUserProvider,
                                 CategoryService categoryService) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.currentUserProvider = currentUserProvider;
        this.categoryService = categoryService;
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo CSV.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El CSV no puede superar los 5 MB.");
        }
        try {
            String csvText = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (csvText.startsWith("\uFEFF")) csvText = csvText.substring(1);
            String headerLine = csvText.lines().findFirst().orElse("");
            if (headerLine.isBlank()) throw new IllegalArgumentException("El CSV está vacío.");
            char delimiter = detectDelimiter(headerLine);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true)
                    .get();

            try (CSVParser parser = CSVParser.parse(csvText, format)) {
                java.util.Iterator<CSVRecord> records = parser.iterator();
                if (!records.hasNext()) throw new IllegalArgumentException("El CSV está vacío.");
                List<String> headers = records.next().stream()
                    .map(MovementImportService::normalize)
                    .toList();
                int dateColumn = findColumn(headers, "fecha", "date", "bookingdate", "valuedate", "transactiondate");
                int conceptColumn = findColumn(headers, "concepto", "concept", "descripcion", "description", "merchant", "beneficiario", "comercio", "detalle");
                int amountColumn = findColumn(headers, "importe", "amount", "monto", "importeur", "amounteur", "importeeur", "valor");
                int typeColumn = findColumn(headers, "tipo", "type", "movimiento", "movement");
                int categoryColumn = findColumn(headers, "categoria", "category");
                int debitColumn = findColumn(headers, "cargo", "debe", "debit", "withdrawal");
                int creditColumn = findColumn(headers, "abono", "haber", "credit", "deposit");
                if (dateColumn < 0 || conceptColumn < 0
                        || (amountColumn < 0 && debitColumn < 0 && creditColumn < 0)) {
                    throw new IllegalArgumentException("El CSV debe incluir fecha, concepto/descripción e importe (o columnas de cargo/abono).");
                }

                List<Row> rows = new ArrayList<>();
                while (records.hasNext()) {
                    CSVRecord record = records.next();
                    if (rows.size() >= MAX_ROWS) throw new IllegalArgumentException("El CSV no puede superar 2.000 movimientos.");
                    List<String> cells = record.toList();
                    try {
                        String concept = cell(cells, conceptColumn).trim();
                        if (concept.isBlank() || concept.length() > 100) throw new IllegalArgumentException("concepto vacío o demasiado largo");
                        LocalDate date = parseDate(cell(cells, dateColumn).trim());
                        BigDecimal signedAmount;
                        Boolean separateColumnType = null;
                        if (amountColumn >= 0) {
                            signedAmount = parseAmount(cell(cells, amountColumn));
                        } else if (!cell(cells, creditColumn).isBlank() && parseAmount(cell(cells, creditColumn)).signum() != 0) {
                            signedAmount = parseAmount(cell(cells, creditColumn));
                            separateColumnType = true;
                        } else {
                            signedAmount = parseAmount(cell(cells, debitColumn));
                            separateColumnType = false;
                        }
                        if (signedAmount.signum() == 0) throw new IllegalArgumentException("importe igual a cero");
                        BigDecimal absoluteAmount = signedAmount.abs();
                        if (absoluteAmount.scale() > 2 || absoluteAmount.precision() - absoluteAmount.scale() > 10) {
                            throw new IllegalArgumentException("importe fuera del rango admitido");
                        }
                        String rawType = typeColumn < 0 ? "" : normalize(cell(cells, typeColumn));
                        boolean income = rawType.isBlank() && separateColumnType != null
                                ? separateColumnType : resolveIncome(rawType, signedAmount);
                        String category = categoryColumn < 0 ? "" : cell(cells, categoryColumn).trim();
                        rows.add(new Row(date, concept, absoluteAmount, income, category));
                    } catch (RuntimeException exception) {
                        throw new IllegalArgumentException("Registro " + record.getRecordNumber() + ": " + exception.getMessage());
                    }
                }
                if (rows.isEmpty()) throw new IllegalArgumentException("El CSV no contiene movimientos.");
                return saveRows(rows, currentUserProvider.getCurrentUser());
            }
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el archivo CSV.", exception);
        }
    }

    private ImportResult saveRows(List<Row> rows, User user) {
        int imported = 0;
        int duplicates = 0;
        for (Row row : rows) {
            if (row.income()) {
                if (incomeRepository.existsByUserAndDateAndConceptIgnoreCaseAndAmount(
                        user, row.date(), row.concept(), row.amount())) {
                    duplicates++;
                    continue;
                }
                String category = availableCategory(row.category(), CategoryType.INCOME);
                Income income = new Income();
                income.setUser(user);
                income.setDate(row.date());
                income.setConcept(row.concept());
                income.setAmount(row.amount());
                income.setCategory(category);
                incomeRepository.save(income);
            } else {
                if (expenseRepository.existsByUserAndDateAndConceptIgnoreCaseAndAmount(
                        user, row.date(), row.concept(), row.amount())) {
                    duplicates++;
                    continue;
                }
                String category = availableCategory(row.category(), CategoryType.EXPENSE);
                Expense expense = new Expense();
                expense.setUser(user);
                expense.setDate(row.date());
                expense.setConcept(row.concept());
                expense.setAmount(row.amount());
                expense.setCategory(category);
                expenseRepository.save(expense);
            }
            imported++;
        }
        return new ImportResult(imported, duplicates);
    }

    private String availableCategory(String requested, CategoryType type) {
        if (requested == null || requested.isBlank()) return "Otros";
        String existing = categoryService.getAvailableCategories(type).stream()
                .map(com.adrianaroca.smartfinance.model.Category::getName)
                .filter(name -> name.equalsIgnoreCase(requested))
                .findFirst().orElse(null);
        if (existing != null) return existing;
        try {
            return categoryService.createCustomCategory(requested, type).getName();
        } catch (IllegalArgumentException exception) {
            return "Otros";
        }
    }

    private boolean resolveIncome(String type, BigDecimal amount) {
        if (type.isBlank()) return amount.signum() > 0;
        if (type.contains("ingreso") || type.equals("income") || type.equals("credit") || type.equals("abono")) return true;
        if (type.contains("gasto") || type.equals("expense") || type.equals("debit") || type.equals("cargo")) return false;
        throw new IllegalArgumentException("tipo no reconocido; usa Ingreso/Gasto o deja que se deduzca por el signo del importe");
    }

    private LocalDate parseDate(String value) {
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ignored) {
            try { return LocalDate.parse(value, SPANISH_DATE); }
            catch (DateTimeParseException exception) { throw new IllegalArgumentException("fecha no válida (usa AAAA-MM-DD o DD/MM/AAAA)"); }
        }
    }

    private BigDecimal parseAmount(String value) {
        String normalized = value == null ? "" : value.trim().replace("€", "").replace(" ", "").replace("'", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("importe vacío");
        int comma = normalized.lastIndexOf(',');
        int dot = normalized.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            normalized = comma > dot ? normalized.replace(".", "").replace(',', '.') : normalized.replace(",", "");
        } else if (comma >= 0) {
            normalized = normalized.replace(',', '.');
        }
        try { return new BigDecimal(normalized); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("importe no válido"); }
    }

    private int findColumn(List<String> headers, String... aliases) {
        for (int index = 0; index < headers.size(); index++) {
            for (String alias : aliases) if (headers.get(index).equals(alias)) return index;
        }
        return -1;
    }

    private char detectDelimiter(String line) {
        char selected = ';';
        int max = -1;
        for (char candidate : new char[]{';', ',', '\t'}) {
            int count = 0;
            boolean quoted = false;
            for (char value : line.toCharArray()) {
                if (value == '"') quoted = !quoted;
                else if (!quoted && value == candidate) count++;
            }
            if (count > max) { max = count; selected = candidate; }
        }
        return selected;
    }

    private String cell(List<String> values, int column) {
        return column >= 0 && column < values.size() ? values.get(column) : "";
    }

    private static String normalize(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_()€-]", "");
    }

    private record Row(LocalDate date, String concept, BigDecimal amount, boolean income, String category) { }

    public record ImportResult(int imported, int duplicates) { }
}
