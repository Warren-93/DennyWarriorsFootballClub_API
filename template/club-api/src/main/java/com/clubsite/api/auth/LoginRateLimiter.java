package com.clubsite.api.auth;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window limiter for /auth/login: 5 failures per username per
 * 5 minutes. A club site's traffic doesn't justify Redis/Bucket4j; this just
 * needs to blunt brute-force attempts. Note it is per-instance, so running
 * several replicas multiplies the allowance.
 */
@Component
public class LoginRateLimiter {

    static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300;

    private final boolean enabled;
    private final ConcurrentHashMap<String, Window> failuresByUsername = new ConcurrentHashMap<>();

    public LoginRateLimiter(ClubProperties properties) {
        this.enabled = properties.security().loginRateLimitEnabled();
    }

    public void checkAllowed(String username) {
        if (!enabled) return;

        Window window = failuresByUsername.get(key(username));
        if (window != null && !window.isExpired() && window.count >= MAX_ATTEMPTS) {
            throw ApiException.tooManyRequests("Too many login attempts, try again later");
        }
    }

    public void recordFailure(String username) {
        if (!enabled) return;

        failuresByUsername.compute(key(username), (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new Window(Instant.now(), 1);
            }
            existing.count++;
            return existing;
        });
    }

    public void recordSuccess(String username) {
        failuresByUsername.remove(key(username));
    }

    /** Stops attempts against many made-up usernames from growing the map forever. */
    @Scheduled(fixedRate = WINDOW_SECONDS * 1000)
    void purgeExpired() {
        failuresByUsername.values().removeIf(Window::isExpired);
    }

    private static String key(String username) {
        return username == null ? "" : Text.normalise(username);
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
}
