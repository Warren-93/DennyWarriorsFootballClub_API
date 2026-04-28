package mark.warren93.dev.DennyWarriorsAPI.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every incoming HTTP request once it has been handled, including the
 * response status and duration. Output looks like:
 *
 *   GET    /api/dennywarriors/fixtures -> 200 (43 ms)
 *   POST   /api/dennywarriors/auth/login -> 401 (12 ms)
 *
 * Toggle verbosity at runtime via {@code logging.level.HTTP} (e.g. set the
 * env var {@code LOGGING_LEVEL_HTTP=WARN} on Render to silence it).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("HTTP");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String query = request.getQueryString();
            String path  = request.getRequestURI() + (query != null ? "?" + query : "");
            int status   = response.getStatus();

            log.info("{} {} -> {} ({} ms)",
                     String.format("%-6s", request.getMethod()),
                     path,
                     status,
                     durationMs);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip Render's health probes so they don't spam the console
        return request.getRequestURI().contains("/actuator/health");
    }
}
