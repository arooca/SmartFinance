package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.service.EmailAccountService;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailAccountController {
    private final EmailAccountService emailAccountService;

    public EmailAccountController(EmailAccountService emailAccountService) {
        this.emailAccountService = emailAccountService;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("mailEnabled", emailAccountService.canSendMail());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        if (emailAccountService.canSendMail()) {
            emailAccountService.requestPasswordResetAsync(email);
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Si existe una cuenta asociada, recibirás un enlace para restablecer la contraseña.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        if (!emailAccountService.isResetTokenValid(token)) {
            model.addAttribute("invalidToken", true);
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token,
                        @RequestParam @Size(min = 8, max = 72) String password,
                        @RequestParam String confirmPassword,
                        RedirectAttributes redirectAttributes) {
        if (password.length() < 8 || password.length() > 72) {
            redirectAttributes.addFlashAttribute("errorMessage", "La contraseña debe tener entre 8 y 72 caracteres.");
            return "redirect:/reset-password?token=" + token;
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Las contraseñas no coinciden.");
            return "redirect:/reset-password?token=" + token;
        }
        if (!emailAccountService.resetPassword(token, password)) {
            redirectAttributes.addFlashAttribute("errorMessage", "El enlace ha caducado o ya se utilizó.");
            return "redirect:/forgot-password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Contraseña actualizada. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @GetMapping("/verify-email")
    public String verifyForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("invalidToken", !emailAccountService.isVerificationTokenValid(token));
        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String verify(@RequestParam(required = false) String token, RedirectAttributes redirectAttributes) {
        if (emailAccountService.verifyEmail(token)) {
            redirectAttributes.addFlashAttribute("successMessage", "Correo verificado. Ya puedes iniciar sesión.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "El enlace de verificación ha caducado o ya se utilizó.");
        }
        return "redirect:/login";
    }

    @PostMapping("/verification/resend")
    public String resendVerification(@RequestParam String email, RedirectAttributes redirectAttributes) {
        if (!emailAccountService.canSendMail()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El envío de correo aún no está configurado. Define primero los datos SMTP.");
            redirectAttributes.addFlashAttribute("pendingEmail", email);
            return "redirect:/verification-pending";
        } else {
            emailAccountService.resendVerificationAsync(email);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Si la cuenta existe y necesita verificación, recibirás un correo. Puede tardar unos minutos.");
        }
        return "redirect:/login";
    }
}
