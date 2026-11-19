package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.exception.DuplicateEmailException;
import com.adrianaroca.smartfinance.model.RegisterForm;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.service.UserService;
import com.adrianaroca.smartfinance.service.EmailAccountService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final EmailAccountService emailAccountService;
    private final boolean verificationRequired;

    public AuthController(UserService userService,
                          EmailAccountService emailAccountService,
                          @Value("${app.email-verification.required:false}") boolean verificationRequired) {
        this.userService = userService;
        this.emailAccountService = emailAccountService;
        this.verificationRequired = verificationRequired;
        logger.info("Configuración de registro: verificación de correo obligatoria={}, envío SMTP disponible={}",
                verificationRequired, emailAccountService.canSendMail());
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {

        model.addAttribute(
                "registerForm",
                new RegisterForm()
        );

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid
            @ModelAttribute("registerForm")
            RegisterForm registerForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        String normalizedEmail =
                registerForm.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        registerForm.setEmail(normalizedEmail);

        if (userService.findByEmail(normalizedEmail).isPresent()) {

            bindingResult.rejectValue(
                    "email",
                    "error.registerForm",
                    "Este email ya está registrado"
            );

            return "register";
        }

        User user =
                new User(
                        registerForm.getName().trim(),
                        normalizedEmail,
                        registerForm.getPassword()
                );
        user.setEmailVerified(!verificationRequired);

        try {

            userService.save(user);
            if (verificationRequired) {
                if (emailAccountService.canSendMail()) {
                    emailAccountService.sendVerificationAsync(user.getId());
                    redirectAttributes.addFlashAttribute("successMessage",
                            "La cuenta está creada. Estamos enviando el correo de verificación; puede tardar unos minutos. Si no llega, podrás solicitar otro desde esta página.");
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "La cuenta está creada, pero el correo no está configurado. Configura SMTP y solicita el reenvío desde esta página.");
                }
                redirectAttributes.addFlashAttribute("pendingEmail", normalizedEmail);
                return "redirect:/verification-pending";
            }

        } catch (DuplicateEmailException exception) {

            bindingResult.rejectValue(
                    "email",
                    "error.registerForm",
                    exception.getMessage()
            );

            return "register";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Cuenta creada correctamente. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @GetMapping("/verification-pending")
    public String verificationPending() {
        return "verification-pending";
    }
}
