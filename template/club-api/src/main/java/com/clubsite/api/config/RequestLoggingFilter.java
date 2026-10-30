package com.clubsite.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every request once handled, with status and duration:
 *
 *   GET    /api/v1/fixtures?status=upcoming -> 200 (43 ms)
 *
 * The query string is left out deliberately so tokens or keys passed as
 * parameters never end up in logs. Silence with LOGGING_LEVEL_HTTP=WARN.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("HTTP");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} -> {} ({} ms)",
                    String.format("%-6s", request.getMethod()),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - start);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health probes would otherwise drown everything else out.
        return request.getRequestURI().startsWith("/actuator/health");
    }
}
