package mark.warren93.dev.DennyWarriorsAPI.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory sliding-window limiter for /auth/login. Traffic on a
 * single-club site is modest, so a real distributed limiter (Bucket4j, Redis)
 * would be overkill — this just needs to blunt brute-force attempts.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300; // 5 minutes

    private final boolean enabled;
    private final ConcurrentHashMap<String, Window> attemptsByUsername = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${dwfc.security.login-rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void checkAllowed(String username) {
        if (!enabled) return;

        Window window = attemptsByUsername.get(normalise(username));
        if (window == null) {
            return;
        }
        if (window.isExpired()) {
            attemptsByUsername.remove(normalise(username));
            return;
        }
        if (window.count >= MAX_ATTEMPTS) {
            throw new TooManyAttemptsException("Too many login attempts, try again later");
        }
    }

    public void recordFailure(String username) {
        if (!enabled) return;

        attemptsByUsername.compute(normalise(username), (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new Window(Instant.now(), 1);
            }
            existing.count++;
            return existing;
        });
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(normalise(username));
    }

    private static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class Window {
        private final Instant startedAt;
        private int count;

        private Window(Instant startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(startedAt.plusSeconds(WINDOW_SECONDS));
        }
    }

    public static class TooManyAttemptsException extends RuntimeException {
        public TooManyAttemptsException(String message) {
            super(message);
        }
    }
}
