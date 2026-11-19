package com.adrianaroca.smartfinance.config;

import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class CredentialVersionFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    public CredentialVersionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            User user = userRepository.findById(principal.getUserId()).orElse(null);
            if (user == null || !user.isEmailVerified()
                    || user.getCredentialVersion() != principal.getCredentialVersion()) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?sessionExpired");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
