package com.nextstep.rentacar.web.exception;

import com.nextstep.rentacar.exception.DuplicateResourceException;
import com.nextstep.rentacar.exception.InvalidCredentialsException;
import com.nextstep.rentacar.exception.SessionExpiredException;
import com.nextstep.rentacar.exception.TooManyLoginAttemptsException;
import com.nextstep.rentacar.web.controller.RefreshCookies;
import com.nextstep.rentacar.exception.SearchValidationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Spring 7 / Jackson 3 omit the default type; keep it in the response body as before
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    // Optional so controller slice tests don't need it
    private final ObjectProvider<RefreshCookies> refreshCookies;

    public GlobalExceptionHandler(ObjectProvider<RefreshCookies> refreshCookies) {
        this.refreshCookies = refreshCookies;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex, request));
    }

    /** The session can't continue: also delete the (useless) refresh cookie */
    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ProblemDetail> handleSessionExpired(SessionExpiredException ex, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        refreshCookies.ifAvailable(cookies -> headers.add(HttpHeaders.SET_COOKIE, cookies.clear().toString()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers)
                .body(problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex, request));
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, ex.getRetryAfter().toSeconds())))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex, request));
    }

    private static ProblemDetail problem(HttpStatus status, String title, Exception ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle(title);
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Not Found");
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(SearchValidationException.class)
    public ResponseEntity<ProblemDetail> handleSearchValidation(SearchValidationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Search Validation Error");
        pd.setProperty("path", request.getRequestURI());
        
        // Add search-specific properties for better debugging
        if (ex.getSearchTerm() != null) {
            pd.setProperty("searchTerm", ex.getSearchTerm());
        }
        pd.setProperty("validationError", ex.getValidationError());
        pd.setProperty("searchRequirements", Map.of(
            "minLength", 2,
            "maxLength", 100,
            "allowedCharacters", "alphanumeric, spaces, hyphens, dots, @ symbols, underscores, plus signs, parentheses"
        ));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Each image must be 5 MB or smaller");
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Payload Too Large");
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Bad Request");
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleConflict(IllegalStateException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Conflict");
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Conflict");
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Validation Failed");
        pd.setDetail("Request validation failed");
        pd.setProperty("path", request.getRequestURI());

        List<Map<String, Object>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorMap)
                .collect(Collectors.toList());

        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    private Map<String, Object> toFieldErrorMap(FieldError fe) {
        Map<String, Object> m = new HashMap<>();
        m.put("field", fe.getField());
        m.put("message", fe.getDefaultMessage());
        Object rejected = fe.getRejectedValue();
        if (rejected != null) m.put("rejectedValue", rejected);
        return m;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(ABOUT_BLANK);
        pd.setTitle("Validation Failed");
        pd.setDetail("Constraint violations occurred");
        pd.setProperty("path", request.getRequestURI());

        List<Map<String, Object>> errors = ex.getConstraintViolations()
                .stream()
                .map(this::toViolationMap)
                .collect(Collectors.toList());

        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    private Map<String, Object> toViolationMap(ConstraintViolation<?> v) {
        Map<String, Object> m = new HashMap<>();
        m.put("field", v.getPropertyPath() != null ? v.getPropertyPath().toString() : null);
        m.put("message", v.getMessage());
        Object invalid = v.getInvalidValue();
        if (invalid != null) m.put("rejectedValue", invalid);
        return m;
    }
}
