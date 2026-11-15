package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    /** A clock the test moves forward */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-15T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    @DisplayName("Locked after five failures, open again 15 minutes after the last one")
    void locksAndUnlocks() {
        for (int i = 0; i < 4; i++) {
            service.failed("user:1");
        }
        assertThatCode(() -> service.checkNotLocked("user:1")).doesNotThrowAnyException();

        service.failed("user:1");
        assertThatThrownBy(() -> service.checkNotLocked("user:1"))
                .isInstanceOf(TooManyLoginAttemptsException.class)
                .hasMessageContaining("15 minutes");

        clock.advance(Duration.ofMinutes(14));
        assertThatThrownBy(() -> service.checkNotLocked("user:1")).isInstanceOf(TooManyLoginAttemptsException.class);

        clock.advance(Duration.ofMinutes(1));
        assertThatCode(() -> service.checkNotLocked("user:1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A successful sign-in resets the count, and old failures stop counting")
    void resetAndExpiry() {
        for (int i = 0; i < 4; i++) {
            service.failed("user:2");
        }
        service.succeeded("user:2");
        service.failed("user:2");
        assertThatCode(() -> service.checkNotLocked("user:2")).doesNotThrowAnyException();

        for (int i = 0; i < 3; i++) {
            service.failed("user:3");
        }
        clock.advance(Duration.ofMinutes(16));
        service.failed("user:3");
        service.failed("user:3");
        assertThatCode(() -> service.checkNotLocked("user:3")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Accounts are counted separately")
    void separateKeys() {
        for (int i = 0; i < 5; i++) {
            service.failed("user:4");
        }
        assertThatCode(() -> service.checkNotLocked("user:5")).doesNotThrowAnyException();
    }
}
