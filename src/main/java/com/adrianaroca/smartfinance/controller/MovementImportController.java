package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.service.MovementImportService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MovementImportController {
    private final MovementImportService movementImportService;

    public MovementImportController(MovementImportService movementImportService) {
        this.movementImportService = movementImportService;
    }

    @GetMapping("/movements/import")
    public String form() { return "movements/import"; }

    @PostMapping("/movements/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        try {
            MovementImportService.ImportResult result = movementImportService.importCsv(file);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Importación completada: " + result.imported() + " movimientos añadidos; "
                            + result.duplicates() + " duplicados omitidos.");
            return "redirect:/movements";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/movements/import";
        }
    }
}
