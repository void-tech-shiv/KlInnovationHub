package com.klu.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Per-identity admin login protection. Failed requests for unknown usernames
 * count as well, and the key is normalized so case variants cannot bypass it.
 *
 * This limiter is per application instance. A shared store (e.g. Redis) is
 * required before deploying multiple backend instances.
 */
@Service
public class AdminLoginRateLimiterService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);
    private static final Duration RETENTION = Duration.ofHours(24);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(30);

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private Instant lastCleanup = Instant.EPOCH;

    public AdminLoginRateLimiterService() {
        this(Clock.systemUTC());
    }

    // Package-private constructor makes expiration tests deterministic.
    AdminLoginRateLimiterService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String identity) {
        if (identity == null || identity.isBlank()) {
            return false;
        }
        Instant now = clock.instant();
        cleanup(now);
        String key = normalize(identity);
        AttemptState state = attempts.get(key);
        if (state == null || state.lockUntil == null) {
            return false;
        }
        if (!now.isBefore(state.lockUntil)) {
            attempts.remove(key, state);
            return false;
        }
        return true;
    }

    /**
     * Record failures for existing and nonexistent admin identities alike.
     * Concurrent increments are atomic within a JVM.
     */
    public void recordFailure(String identity) {
        if (identity == null || identity.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        cleanup(now);
        attempts.compute(normalize(identity), (key, current) -> {
            if (current != null && current.lockUntil != null) {
                if (now.isBefore(current.lockUntil)) {
                    return current;
                }
                current = null;
            }
            int count = current == null ? 1 : current.failedAttempts + 1;
            Instant lockUntil = count >= MAX_FAILED_ATTEMPTS
                    ? now.plus(LOCK_DURATION) : null;
            return new AttemptState(count, lockUntil, now);
        });
    }

    public void recordSuccess(String identity) {
        if (identity != null && !identity.isBlank()) {
            attempts.remove(normalize(identity));
        }
    }

    public long retryAfterSeconds(String identity) {
        if (identity == null || identity.isBlank()) {
            return 0L;
        }
        AttemptState state = attempts.get(normalize(identity));
        if (state == null || state.lockUntil == null) {
            return 0L;
        }
        long remainingMillis = Duration.between(clock.instant(), state.lockUntil).toMillis();
        return remainingMillis <= 0L ? 0L : (remainingMillis + 999L) / 1000L;
    }

    private String normalize(String identity) {
        return identity.trim().toLowerCase(Locale.ROOT);
    }

    private synchronized void cleanup(Instant now) {
        if (Duration.between(lastCleanup, now).compareTo(CLEANUP_INTERVAL) < 0) {
            return;
        }
        attempts.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().lastFailedAt.plus(RETENTION)));
        lastCleanup = now;
    }

    private static final class AttemptState {
        private final int failedAttempts;
        private final Instant lockUntil;
        private final Instant lastFailedAt;

        private AttemptState(int failedAttempts, Instant lockUntil, Instant lastFailedAt) {
            this.failedAttempts = failedAttempts;
            this.lockUntil = lockUntil;
            this.lastFailedAt = lastFailedAt;
        }
    }
}
