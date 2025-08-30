package com.nextstep.rentacar.exception;

/**
 * Exception thrown when search parameter validation fails.
 * This provides more specific error handling for search-related validation issues.
 */
public class SearchValidationException extends IllegalArgumentException {
    
    private final String searchTerm;
    private final String validationError;
    
    public SearchValidationException(String searchTerm, String validationError) {
        super(validationError);
        this.searchTerm = searchTerm;
        this.validationError = validationError;
    }
    
    public SearchValidationException(String validationError) {
        this(null, validationError);
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }
    
    public String getValidationError() {
        return validationError;
    }
}