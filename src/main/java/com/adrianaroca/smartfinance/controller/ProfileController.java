package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.exception.DuplicateEmailException;
import com.adrianaroca.smartfinance.model.ProfileForm;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.service.UserService;
import com.adrianaroca.smartfinance.service.EmailAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

@Controller
public class ProfileController {

    private final UserService userService;
    private final EmailAccountService emailAccountService;
    private final boolean verificationRequired;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public ProfileController(
            UserService userService,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            EmailAccountService emailAccountService,
            @Value("${app.email-verification.required:false}") boolean verificationRequired) {

        this.userService = userService;
        this.emailAccountService = emailAccountService;
        this.verificationRequired = verificationRequired;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository =
                new HttpSessionSecurityContextRepository();
    }

    @GetMapping("/profile")
    public String profile(Model model) {

        User user = userService.getCurrentUser();

        ProfileForm profileForm =
                new ProfileForm(
                        user.getName(),
                        user.getEmail()
                );

        model.addAttribute(
                "profileForm",
                profileForm
        );

        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") ProfileForm profileForm,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        User user = userService.getCurrentUser();

        if (bindingResult.hasErrors()) {
            return "profile";
        }

        String newEmail =
                profileForm.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        String currentEmail =
                user.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (!newEmail.equals(currentEmail)) {

            if (userService.findByEmail(newEmail).isPresent()) {

                bindingResult.rejectValue(
                        "email",
                        "error.profileForm",
                        "Este email ya está registrado"
                );

                return "profile";
            }
        }

        boolean changingPassword =
                (profileForm.getCurrentPassword() != null
                        && !profileForm.getCurrentPassword().isBlank())
                        || (profileForm.getNewPassword() != null
                        && !profileForm.getNewPassword().isBlank())
                        || (profileForm.getConfirmPassword() != null
                        && !profileForm.getConfirmPassword().isBlank());

        if (changingPassword) {

            if (profileForm.getCurrentPassword() == null
                    || profileForm.getCurrentPassword().isBlank()) {

                model.addAttribute(
                        "passwordError",
                        "Debes introducir tu contraseña actual"
                );

                return "profile";
            }

            if (!passwordEncoder.matches(
                    profileForm.getCurrentPassword(),
                    user.getPassword())) {

                model.addAttribute(
                        "passwordError",
                        "La contraseña actual no es correcta"
                );

                return "profile";
            }

            if (profileForm.getNewPassword() == null
                    || profileForm.getNewPassword().isBlank()) {

                model.addAttribute(
                        "passwordError",
                        "Debes introducir una nueva contraseña"
                );

                return "profile";
            }

            if (profileForm.getNewPassword().length() < 8) {

                model.addAttribute(
                        "passwordError",
                        "La nueva contraseña debe tener al menos 8 caracteres"
                );

                return "profile";
            }

            if (!profileForm.getNewPassword().equals(
                    profileForm.getConfirmPassword())) {

                model.addAttribute(
                        "passwordError",
                        "Las nuevas contraseñas no coinciden"
                );

                return "profile";
            }
        }

        boolean emailChanged =
                !newEmail.equals(currentEmail);

        try {

            userService.updateProfile(
                    user,
                    profileForm.getName().trim(),
                    newEmail,
                    changingPassword
                            ? profileForm.getNewPassword()
                            : null
            );

        } catch (DuplicateEmailException exception) {

            bindingResult.rejectValue(
                    "email",
                    "error.profileForm",
                    exception.getMessage()
            );

            return "profile";
        }

        if (emailChanged) {

            if (verificationRequired) {
                boolean emailSent = emailAccountService.changeEmailAndRequireVerification(user);
                if (!emailSent) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "El nuevo correo necesita verificación. No se pudo enviar el enlace; solicita el reenvío cuando SMTP esté disponible.");
                }
                invalidateCurrentSession(request);
                if (emailSent) {
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Correo actualizado. Verifica la nueva dirección antes de volver a iniciar sesión.");
                }
                return "redirect:/login";
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(
                            newEmail
                    );
            if (userDetails instanceof CredentialsContainer credentialsContainer) {
                credentialsContainer.eraseCredentials();
            }

            Authentication newAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(
                    newAuthentication
            );

            SecurityContextHolder.setContext(
                    context
            );

            securityContextRepository.saveContext(
                    context,
                    request,
                    response
            );
        }

        if (changingPassword) {
            invalidateCurrentSession(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Contraseña actualizada. Inicia sesión de nuevo con la nueva contraseña.");
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado correctamente.");
        return "redirect:/profile";
    }

    private void invalidateCurrentSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(
            @ModelAttribute("profileForm") ProfileForm profileForm,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User user =
                userService.getCurrentUser();

        if (profileForm.getDeletePassword() == null
                || profileForm.getDeletePassword().isBlank()) {

            model.addAttribute(
                    "deleteError",
                    "Debes introducir tu contraseña para eliminar la cuenta"
            );

            profileForm.setName(
                    user.getName()
            );

            profileForm.setEmail(
                    user.getEmail()
            );

            return "profile";
        }

        if (!passwordEncoder.matches(
                profileForm.getDeletePassword(),
                user.getPassword())) {

            model.addAttribute(
                    "deleteError",
                    "La contraseña no es correcta"
            );

            profileForm.setName(
                    user.getName()
            );

            profileForm.setEmail(
                    user.getEmail()
            );

            return "profile";
        }

        userService.deleteCurrentUser();

        SecurityContextHolder.clearContext();

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tu cuenta se ha eliminado correctamente.");
        return "redirect:/login";
    }
}
