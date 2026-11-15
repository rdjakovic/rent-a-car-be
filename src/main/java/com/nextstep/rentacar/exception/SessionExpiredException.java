package com.nextstep.rentacar.exception;

/**
 * The sign-in session can't be continued (no, unknown, expired, revoked or reused refresh token):
 * the user has to sign in again.
 */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException() {
        super("Your session has ended. Please sign in again.");
    }
}
