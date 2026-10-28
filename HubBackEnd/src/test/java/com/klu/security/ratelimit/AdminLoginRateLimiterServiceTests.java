package com.klu.security.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class AdminLoginRateLimiterServiceTests {

    @Test
    void locksAfterFiveFailuresAndUnlocksAfterFiveMinutes() {
        MutableClock clock = new MutableClock();
        AdminLoginRateLimiterService limiter = new AdminLoginRateLimiterService(clock);

        for (int attempt = 1; attempt <= 4; attempt++) {
            limiter.recordFailure("Admin@Example.com");
            assertFalse(limiter.isBlocked("admin@example.com"));
        }
        limiter.recordFailure("admin@example.com");

        assertTrue(limiter.isBlocked(" ADMIN@example.com "));
        assertEquals(300L, limiter.retryAfterSeconds("admin@example.com"));
        limiter.recordFailure("admin@example.com");
        assertTrue(limiter.isBlocked("admin@example.com"));

        clock.advance(Duration.ofMinutes(5));
        assertFalse(limiter.isBlocked("admin@example.com"));
        assertEquals(0L, limiter.retryAfterSeconds("admin@example.com"));
        limiter.recordFailure("admin@example.com");
        assertFalse(limiter.isBlocked("admin@example.com"));
    }

    @Test
    void successResetsFailedAttemptsWithoutAffectingOtherAccounts() {
        AdminLoginRateLimiterService limiter = new AdminLoginRateLimiterService();
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("admin@example.com");
            limiter.recordFailure("another@example.com");
        }

        limiter.recordSuccess(" ADMIN@EXAMPLE.COM ");

        assertFalse(limiter.isBlocked("admin@example.com"));
        assertTrue(limiter.isBlocked("another@example.com"));
    }

    @Test
    void concurrentFailuresCannotBypassAtomicIncrement() {
        AdminLoginRateLimiterService limiter = new AdminLoginRateLimiterService();
        IntStream.range(0, 100).parallel()
                .forEach(i -> limiter.recordFailure("admin@example.com"));
        assertTrue(limiter.isBlocked("admin@example.com"));
        assertTrue(limiter.retryAfterSeconds("admin@example.com") > 0);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-28T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
