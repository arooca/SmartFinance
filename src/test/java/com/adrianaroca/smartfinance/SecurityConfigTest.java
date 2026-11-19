package com.adrianaroca.smartfinance;

import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.CategoryRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void ensureAuthenticatedTestUserExists() {
        userRepository.findByEmail("adrian@test.com")
                .orElseGet(() -> userRepository.save(
                        new User("Adrian", "adrian@test.com", "test-password")));
    }

    @Test
    void unauthenticatedUserCannotAccessProtectedPage()
            throws Exception {

        mockMvc.perform(
                        get("/income/new")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedUserCanAccessProtectedPage()
            throws Exception {

        mockMvc.perform(
                        get("/income/new")
                                .with(user("adrian@test.com")
                                        .roles("USER"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserCanCreateCustomCategoryFromAForm()
            throws Exception {

        mockMvc.perform(
                        post("/categories/custom")
                                .with(user("adrian@test.com").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Mascotas\",\"type\":\"EXPENSE\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mascotas"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        assertTrue(categoryRepository.findAll().stream()
                .anyMatch(category -> category.getName().equals("Mascotas")
                        && category.getType().name().equals("EXPENSE")
                        && category.getUser().getEmail().equals("adrian@test.com")));
    }

    @Test
    void loginPageContainsCsrfToken()
            throws Exception {

        mockMvc.perform(
                        get("/login")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"_csrf\""
                                )
                        )
                );
    }

    @Test
    void postWithoutCsrfTokenIsRejected()
            throws Exception {

        mockMvc.perform(
                        post("/logout")
                                .with(user("adrian@test.com")
                                        .roles("USER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithCsrfTokenIsAccepted()
            throws Exception {

        mockMvc.perform(
                        post("/logout")
                                .with(user("adrian@test.com")
                                        .roles("USER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/login?logout")
                );
    }

    @Test
    void logoutInvalidatesSession()
            throws Exception {

        MockHttpSession session =
                new MockHttpSession();

        session.setAttribute(
                "test",
                "value"
        );

        mockMvc.perform(
                        post("/logout")
                                .session(session)
                                .with(user("adrian@test.com")
                                        .roles("USER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection());

        assertTrue(
                session.isInvalid()
        );
    }
}
