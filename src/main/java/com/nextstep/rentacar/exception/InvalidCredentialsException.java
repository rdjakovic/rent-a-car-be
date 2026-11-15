package com.nextstep.rentacar.exception;

/**
 * Sign-in failed. One message for every reason (unknown user, wrong password, disabled account),
 * so the answer doesn't reveal which accounts exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
