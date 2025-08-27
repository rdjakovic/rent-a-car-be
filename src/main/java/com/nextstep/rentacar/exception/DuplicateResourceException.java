package com.nextstep.rentacar.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * This should result in HTTP 409 Conflict status.
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
