package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "No hay un usuario autenticado"
            );
        }

        return userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "No se ha encontrado el usuario autenticado"
                )
        );
    }
}