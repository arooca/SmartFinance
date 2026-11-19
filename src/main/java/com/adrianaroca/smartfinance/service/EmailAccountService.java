package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.EmailActionToken;
import com.adrianaroca.smartfinance.model.EmailActionType;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.EmailActionTokenRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class EmailAccountService {
    private final EmailActionTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String publicUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailAccountService(EmailActionTokenRepository tokenRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${app.mail.enabled:false}") boolean mailEnabled,
                               @Value("${app.mail.from:${spring.mail.username:no-reply@smartfinance.local}}") String fromAddress,
                               @Value("${app.public-url:http://localhost:8080}") String publicUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.publicUrl = publicUrl.replaceAll("/$", "");
    }

    public boolean isMailEnabled() { return mailEnabled; }

    public boolean canSendMail() { return mailEnabled && mailSender != null; }

    @Async("emailTaskExecutor")
    @Transactional
    public void sendVerificationAsync(Long userId) {
        userRepository.findById(userId).ifPresent(this::sendVerification);
    }

    @Async("emailTaskExecutor")
    @Transactional
    public void resendVerificationAsync(String email) {
        userRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);
    }

    @Async("emailTaskExecutor")
    @Transactional
    public void requestPasswordResetAsync(String email) {
        requestPasswordReset(email);
    }

    @Transactional
    public void sendVerification(User user) {
        createAndSend(user, EmailActionType.VERIFY_EMAIL,
                "Verifica tu correo de SmartFinance",
                token -> "Confirma tu correo en este enlace (válido 24 horas):\n\n"
                        + publicUrl + "/verify-email?token=" + token);
    }

    public boolean isVerificationTokenValid(String rawToken) {
        return findValid(rawToken, EmailActionType.VERIFY_EMAIL).isPresent();
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .ifPresent(user -> createAndSend(user, EmailActionType.RESET_PASSWORD,
                        "Restablece tu contraseña de SmartFinance",
                        token -> "Si solicitaste cambiar tu contraseña, usa este enlace (válido 30 minutos):\n\n"
                                + publicUrl + "/reset-password?token=" + token
                                + "\n\nSi no fuiste tú, ignora este correo."));
    }

    @Transactional
    public boolean verifyEmail(String rawToken) {
        Optional<EmailActionToken> action = findValid(rawToken, EmailActionType.VERIFY_EMAIL);
        if (action.isEmpty()) return false;
        User user = action.get().getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteAllByUserAndType(user, EmailActionType.VERIFY_EMAIL);
        return true;
    }

    public boolean isResetTokenValid(String rawToken) {
        return findValid(rawToken, EmailActionType.RESET_PASSWORD).isPresent();
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        Optional<EmailActionToken> action = findValid(rawToken, EmailActionType.RESET_PASSWORD);
        if (action.isEmpty()) return false;
        User user = action.get().getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.incrementCredentialVersion();
        userRepository.save(user);
        tokenRepository.deleteAllByUserAndType(user, EmailActionType.RESET_PASSWORD);
        return true;
    }

    @Transactional
    public boolean changeEmailAndRequireVerification(User user) {
        user.setEmailVerified(false);
        user.incrementCredentialVersion();
        userRepository.save(user);
        try {
            sendVerification(user);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void createAndSend(User user, EmailActionType type, String subject,
                               java.util.function.Function<String, String> body) {
        if (!mailEnabled || mailSender == null) {
            if (type == EmailActionType.VERIFY_EMAIL) {
                throw new IllegalStateException("No existe un JavaMailSender. Activa y configura el servicio SMTP para enviar correos.");
            }
            return;
        }
        tokenRepository.deleteAllByUserAndType(user, type);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(type == EmailActionType.VERIFY_EMAIL
                ? Duration.ofHours(24) : Duration.ofMinutes(30));
        tokenRepository.save(new EmailActionToken(hash(rawToken), type, user, expires));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body.apply(rawToken));
        mailSender.send(message);
    }

    private Optional<EmailActionToken> findValid(String rawToken, EmailActionType type) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return tokenRepository.findByTokenHashAndType(hash(rawToken), type)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()));
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
