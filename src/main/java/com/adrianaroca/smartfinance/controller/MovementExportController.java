package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.DashboardMovement;
import com.adrianaroca.smartfinance.service.MovementExportService;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class MovementExportController {

    private static final MediaType CSV_MEDIA_TYPE =
            new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final MovementService movementService;
    private final MovementExportService movementExportService;

    public MovementExportController(
            MovementService movementService,
            MovementExportService movementExportService) {
        this.movementService = movementService;
        this.movementExportService = movementExportService;
    }

    @GetMapping("/movements/export")
    public ResponseEntity<byte[]> exportMovements(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        String normalizedFormat = format.trim().toLowerCase(Locale.ROOT);
        if (!normalizedFormat.equals("csv") && !normalizedFormat.equals("xlsx")) {
            throw new ResponseStatusException(BAD_REQUEST, "Formato de exportación no válido");
        }

        List<DashboardMovement> movements = movementService.getFilteredMovements(
                type, search, category, startDate, endDate);
        boolean xlsx = normalizedFormat.equals("xlsx");
        byte[] file = xlsx
                ? movementExportService.toXlsx(movements)
                : movementExportService.toCsv(movements);

        String extension = xlsx ? "xlsx" : "csv";
        String fileName = "smartfinance-movimientos-"
                + LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                + "." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(xlsx ? XLSX_MEDIA_TYPE : CSV_MEDIA_TYPE);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(file);
    }
}
