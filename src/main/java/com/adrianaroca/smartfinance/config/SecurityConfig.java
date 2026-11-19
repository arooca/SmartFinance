package com.adrianaroca.smartfinance.config;

import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.DispatcherType;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            UserRepository userRepository) {

        return email -> {

            String normalizedEmail =
                    email
                            .trim()
                            .toLowerCase(Locale.ROOT);

            User user =
                    userRepository.findByEmail(
                                    normalizedEmail
                            )
                            .orElseThrow(() ->
                                    new UsernameNotFoundException(
                                            "Usuario no encontrado con el email: "
                                                    + normalizedEmail
                                    )
                            );

            return new AuthenticatedUserPrincipal(user.getId(), user.getEmail(),
                    user.getPassword(), user.isEmailVerified(), user.getCredentialVersion());
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserRepository userRepository,
            @Value("${app.security.trust-cloudflare-header:false}") boolean trustCloudflareHeader) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
                                "/verify-email",
                                "/verification-pending",
                                "/verification/resend",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .csrf(Customizer.withDefaults())

                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation ->
                                sessionFixation.changeSessionId()
                        )
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl(
                                "/dashboard",
                                true
                        )
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl(
                                "/login?logout"
                        )
                        .permitAll()
                );

        http.addFilterBefore(new AttemptRateLimitFilter(trustCloudflareHeader),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new CredentialVersionFilter(userRepository), AuthorizationFilter.class);

        return http.build();
    }
}
