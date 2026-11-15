package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks sign-in for an account after {@value #MAX_FAILURES} failed attempts in a row, for {@link #LOCK_DURATION}.
 * Kept in memory: a restart clears it, and with several instances each counts on its own.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /** Above this many tracked keys, finished entries are dropped so unknown names can't grow the map forever */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /** The key sign-in failures of a user account are counted under */
    public static String userKey(Long userId) {
        return "user:" + userId;
    }

    /** Throws if the key is locked. */
    public void checkNotLocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null || current.failures() < MAX_FAILURES) {
            return;
        }
        Duration left = Duration.between(clock.instant(), current.lastFailure().plus(LOCK_DURATION));
        if (left.isNegative() || left.isZero()) {
            attempts.remove(key);
            return;
        }
        throw new TooManyLoginAttemptsException(left);
    }

    public void failed(String key) {
        Instant now = clock.instant();
        attempts.merge(key, new Attempts(1, now), (old, ignored) -> expired(old, now)
                ? new Attempts(1, now)
                : new Attempts(old.failures() + 1, now));
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(a -> expired(a, now));
        }
    }

    public void succeeded(String key) {
        attempts.remove(key);
    }

    /** Failures older than the lock duration no longer count */
    private static boolean expired(Attempts attempts, Instant now) {
        return attempts.lastFailure().plus(LOCK_DURATION).isBefore(now);
    }
}
