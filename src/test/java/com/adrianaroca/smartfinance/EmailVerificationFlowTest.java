package com.adrianaroca.smartfinance;

import com.adrianaroca.smartfinance.model.EmailActionToken;
import com.adrianaroca.smartfinance.model.EmailActionType;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.EmailActionTokenRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "app.email-verification.required=true",
        "APP_MAIL_ENABLED=false"
})
@AutoConfigureMockMvc
class EmailVerificationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailActionTokenRepository tokenRepository;

    @Test
    void registrationRequiresExplicitPostConfirmationBeforeLogin() throws Exception {
        String email = "verification-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Verification Test")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verification-pending"));

        User pendingUser = userRepository.findByEmail(email).orElseThrow();
        assertFalse(pendingUser.isEmailVerified());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        String rawToken = "one-time-confirmation-token";
        tokenRepository.save(new EmailActionToken(
                sha256(rawToken),
                EmailActionType.VERIFY_EMAIL,
                pendingUser,
                Instant.now().plusSeconds(3600)));

        mockMvc.perform(get("/verify-email").param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email"));
        assertFalse(userRepository.findByEmail(email).orElseThrow().isEmailVerified());

        mockMvc.perform(post("/verify-email")
                        .with(csrf())
                        .param("token", rawToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertTrue(userRepository.findByEmail(email).orElseThrow().isEmailVerified());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    private String sha256(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
