package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.DashboardMovement;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MovementExportService {

    private static final List<String> HEADERS =
            List.of("Fecha", "Tipo", "Concepto", "Categoría", "Importe (€)");

    public byte[] toCsv(List<DashboardMovement> movements) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, HEADERS);

        for (DashboardMovement movement : movements) {
            appendCsvRow(csv, List.of(
                    safeText(movement.getDate() == null ? "" : movement.getDate().toString()),
                    movementTypeLabel(movement.getType()),
                    safeText(movement.getConcept()),
                    safeText(movement.getCategory()),
                    movement.getAmount() == null
                            ? ""
                            : movement.getAmount().toPlainString().replace('.', ',')
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toXlsx(List<DashboardMovement> movements) {
        StringBuilder rows = new StringBuilder();
        appendXlsxRow(rows, 1, HEADERS, null);

        int rowNumber = 2;
        for (DashboardMovement movement : movements) {
            List<String> values = List.of(
                    movement.getDate() == null ? "" : movement.getDate().toString(),
                    movementTypeLabel(movement.getType()),
                    safeText(movement.getConcept()),
                    safeText(movement.getCategory()),
                    ""
            );
            appendXlsxRow(rows, rowNumber++, values, movement.getAmount() == null
                    ? null
                    : movement.getAmount().toPlainString());
        }

        Map<String, String> workbookParts = new LinkedHashMap<>();
        workbookParts.put("[Content_Types].xml", contentTypesXml());
        workbookParts.put("_rels/.rels", rootRelationshipsXml());
        workbookParts.put("xl/workbook.xml", workbookXml());
        workbookParts.put("xl/_rels/workbook.xml.rels", workbookRelationshipsXml());
        workbookParts.put("xl/worksheets/sheet1.xml", worksheetXml(rows.toString()));

        return zip(workbookParts);
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append('"')
                    .append(values.get(i).replace("\"", "\"\""))
                    .append('"');
        }
        csv.append("\r\n");
    }

    private String movementTypeLabel(String type) {
        return switch (type.toUpperCase(java.util.Locale.ROOT)) {
            case "INCOME" -> "Ingreso";
            case "SAVING" -> "Ahorro apartado";
            default -> "Gasto";
        };
    }

    private void appendXlsxRow(
            StringBuilder rows,
            int rowNumber,
            List<String> values,
            String amount) {

        rows.append("<row r=\"").append(rowNumber).append("\">");
        for (int i = 0; i < 4; i++) {
            rows.append(inlineStringCell(columnName(i) + rowNumber, values.get(i)));
        }

        String amountReference = "E" + rowNumber;
        if (rowNumber == 1) {
            rows.append(inlineStringCell(amountReference, values.get(4)));
        } else if (amount == null) {
            rows.append("<c r=\"").append(amountReference).append("\"/>");
        } else {
            rows.append("<c r=\"").append(amountReference).append("\"><v>")
                    .append(xmlEscape(amount)).append("</v></c>");
        }
        rows.append("</row>");
    }

    private String inlineStringCell(String reference, String value) {
        return "<c r=\"" + reference + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                + xmlEscape(value) + "</t></is></c>";
    }

    private String columnName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        String firstVisibleCharacters = value.stripLeading();
        if (!firstVisibleCharacters.isEmpty()
                && "=+-@".indexOf(firstVisibleCharacters.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private String xmlEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        value.codePoints().forEach(codePoint -> {
            if (codePoint == '&') {
                escaped.append("&amp;");
            } else if (codePoint == '<') {
                escaped.append("&lt;");
            } else if (codePoint == '>') {
                escaped.append("&gt;");
            } else if (codePoint == '"') {
                escaped.append("&quot;");
            } else if (codePoint == '\'') {
                escaped.append("&apos;");
            } else if (isValidXmlCharacter(codePoint)) {
                escaped.appendCodePoint(codePoint);
            }
        });
        return escaped.toString();
    }

    private boolean isValidXmlCharacter(int codePoint) {
        return codePoint == 0x9
                || codePoint == 0xA
                || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }

    private byte[] zip(Map<String, String> parts) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo generar el archivo XLSX", exception);
        }
    }

    private String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private String rootRelationshipsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"Movimientos\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private String workbookRelationshipsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    private String worksheetXml(String rows) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetData>" + rows + "</sheetData></worksheet>";
    }
}
