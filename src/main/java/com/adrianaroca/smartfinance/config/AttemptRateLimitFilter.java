package com.adrianaroca.smartfinance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AttemptRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 5_000;
    private final boolean trustCloudflareHeader;
    private final Map<String, ArrayDeque<Instant>> attempts = new LinkedHashMap<>();

    public AttemptRateLimitFilter(boolean trustCloudflareHeader) {
        this.trustCloudflareHeader = trustCloudflareHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (!"POST".equalsIgnoreCase(request.getMethod()) || limitFor(path) == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = limitFor(path);
        String key = path + "|" + clientAddress(request);
        if (!allow(key, limit)) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Espera unos minutos e inténtalo de nuevo.");
            return;
        }
        filterChain.doFilter(request, response);
        if ("/login".equals(path)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal) {
                clear(key);
            }
        }
    }

    private int limitFor(String path) {
        return switch (path) {
            case "/login" -> 10;
            case "/forgot-password", "/verification/resend" -> 5;
            default -> 0;
        };
    }

    private String clientAddress(HttpServletRequest request) {
        if (trustCloudflareHeader) {
            String cloudflareAddress = request.getHeader("CF-Connecting-IP");
            if (cloudflareAddress != null && !cloudflareAddress.isBlank()) {
                return cloudflareAddress.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private synchronized boolean allow(String key, int limit) {
        Instant now = Instant.now();
        ArrayDeque<Instant> recent = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(WINDOW))) {
            recent.removeFirst();
        }
        if (recent.size() >= limit) return false;
        recent.addLast(now);
        while (attempts.size() > MAX_TRACKED_KEYS) {
            String eldest = attempts.keySet().iterator().next();
            attempts.remove(eldest);
        }
        return true;
    }

    private synchronized void clear(String key) {
        attempts.remove(key);
    }
}
