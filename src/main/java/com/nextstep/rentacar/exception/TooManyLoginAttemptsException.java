package com.nextstep.rentacar.exception;

import lombok.Getter;

import java.time.Duration;

/**
 * Sign-in is locked for a while after too many failed attempts.
 */
@Getter
public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many failed sign-in attempts. Try again in " + Math.max(1, retryAfter.toMinutes()) + " minutes.");
        this.retryAfter = retryAfter;
    }
}
