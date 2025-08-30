# Reservation Search API Documentation

## Overview

This document describes the enhanced API documentation and error handling for the reservation search functionality. The implementation provides comprehensive OpenAPI documentation with detailed examples and robust error handling for search validation.

## API Endpoint

### GET /api/reservations

**Summary:** List reservations with optional filters, pagination and sorting

**Description:** 
Retrieve reservations with comprehensive filtering and search capabilities.

#### Search Functionality
The search parameter enables unified searching across multiple fields in a single API call:
- Customer information: first name, last name, full name, email address, phone number
- Reservation details: reservation ID (exact match prioritized)
- Car information: display name, model
- Branch information: branch name

#### Search Behavior
- Case-insensitive partial matching across all searchable fields
- Minimum 2 characters required for search terms
- Maximum 100 characters allowed
- Exact reservation ID matches are prioritized in results
- Results ordered by relevance (exact ID matches first, then by creation date)

#### Performance
- Single optimized database query with JOINs
- Database indexes ensure sub-500ms response times
- Supports concurrent search operations

#### Backward Compatibility
- All existing filter parameters remain functional
- When search parameter is provided, it takes precedence over customer-based filtering
- Pagination and sorting work seamlessly with search results

## Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `customerId` | Long | No | Filter by specific customer ID | `123` |
| `carId` | Long | No | Filter by specific car ID | `456` |
| `status` | ReservationStatus | No | Filter by reservation status | `CONFIRMED` |
| `branchId` | Long | No | Filter by pickup/dropoff branch ID | `789` |
| `startDate` | LocalDate | No | Filter reservations starting from this date (inclusive) | `2024-01-01` |
| `endDate` | LocalDate | No | Filter reservations ending before this date (inclusive) | `2024-12-31` |
| `search` | String | No | Search term across multiple fields | See examples below |
| `page` | Integer | No | Page number (0-based) | `0` |
| `size` | Integer | No | Page size | `10` |
| `sort` | String | No | Sort criteria | `id,desc` |

### Search Parameter Examples

| Search Type | Example Value | Description |
|-------------|---------------|-------------|
| Customer name | `John Smith` | Searches customer first/last names |
| Email search | `john@example.com` | Searches customer email addresses |
| Phone search | `555-0123` | Searches customer phone numbers |
| Reservation ID | `12345` | Exact match prioritized for reservation IDs |
| Car model | `Toyota Camry` | Searches car display names and models |
| Branch name | `Downtown` | Searches branch names |

### Search Requirements

- **Minimum length:** 2 characters
- **Maximum length:** 100 characters
- **Allowed characters:** Alphanumeric, spaces, hyphens, dots, @ symbols, underscores, plus signs, parentheses
- **Case sensitivity:** Case-insensitive matching
- **Matching:** Partial matching across all searchable fields

## Response Codes

### Success Responses

| Code | Description | Content Type |
|------|-------------|--------------|
| 200 | Successfully retrieved reservations | `application/json` |

### Error Responses

| Code | Description | Content Type |
|------|-------------|--------------|
| 400 | Invalid request parameters | `application/problem+json` |

## Error Handling

The API provides detailed error responses for search validation failures using the RFC 7807 Problem Details format.

### Search Validation Errors

#### Too Short Search Term

**Request:**
```http
GET /api/reservations?search=x
```

**Response:**
```json
{
  "type": "about:blank",
  "title": "Search Validation Error",
  "status": 400,
  "detail": "Search term must be at least 2 characters long",
  "instance": "/api/reservations",
  "path": "/api/reservations",
  "searchTerm": "x",
  "validationError": "Search term must be at least 2 characters long",
  "searchRequirements": {
    "minLength": 2,
    "maxLength": 100,
    "allowedCharacters": "alphanumeric, spaces, hyphens, dots, @ symbols, underscores, plus signs, parentheses"
  }
}
```

#### Too Long Search Term

**Request:**
```http
GET /api/reservations?search=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
```

**Response:**
```json
{
  "type": "about:blank",
  "title": "Search Validation Error",
  "status": 400,
  "detail": "Search term cannot exceed 100 characters",
  "instance": "/api/reservations",
  "path": "/api/reservations",
  "searchTerm": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "validationError": "Search term cannot exceed 100 characters",
  "searchRequirements": {
    "minLength": 2,
    "maxLength": 100,
    "allowedCharacters": "alphanumeric, spaces, hyphens, dots, @ symbols, underscores, plus signs, parentheses"
  }
}
```

#### Invalid Characters

**Request:**
```http
GET /api/reservations?search=!@#$%^&*
```

**Response:**
```json
{
  "type": "about:blank",
  "title": "Search Validation Error",
  "status": 400,
  "detail": "Search term contains only invalid characters",
  "instance": "/api/reservations",
  "path": "/api/reservations",
  "searchTerm": "!@#$%^&*",
  "validationError": "Search term contains only invalid characters",
  "searchRequirements": {
    "minLength": 2,
    "maxLength": 100,
    "allowedCharacters": "alphanumeric, spaces, hyphens, dots, @ symbols, underscores, plus signs, parentheses"
  }
}
```

#### Invalid Date Range

**Request:**
```http
GET /api/reservations?startDate=2024-12-31&endDate=2024-01-01
```

**Response:**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid date range: endDate must be on/after startDate",
  "instance": "/api/reservations",
  "path": "/api/reservations"
}
```

### Error Response Properties

All search validation errors include the following additional properties:

- `searchTerm`: The original search term that caused the validation error
- `validationError`: Detailed description of the validation failure
- `searchRequirements`: Object containing validation requirements:
  - `minLength`: Minimum allowed search term length (2)
  - `maxLength`: Maximum allowed search term length (100)
  - `allowedCharacters`: Description of allowed character types

## Implementation Details

### Custom Exception Handling

The API uses a custom `SearchValidationException` class that extends `IllegalArgumentException` to provide more specific error handling for search-related validation issues.

### Global Exception Handler

The `GlobalExceptionHandler` includes specific handling for search validation errors:

```java
@ExceptionHandler(SearchValidationException.class)
public ResponseEntity<ProblemDetail> handleSearchValidation(SearchValidationException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
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
```

### Input Sanitization

The search parameter undergoes validation and sanitization:

1. **Length validation:** Ensures search terms are between 2-100 characters
2. **Character filtering:** Removes potentially dangerous characters while preserving valid ones
3. **Empty result handling:** Throws appropriate exceptions for invalid inputs

### OpenAPI Documentation

The endpoint is fully documented with OpenAPI annotations including:

- Comprehensive parameter descriptions with examples
- Detailed response schemas
- Error response examples with realistic scenarios
- Search behavior documentation
- Performance characteristics

## Testing

The implementation includes comprehensive test coverage:

### Unit Tests
- `ReservationControllerTest`: Tests controller layer with mocked services
- Parameter validation and error handling
- Search functionality with various input scenarios

### Integration Tests
- `ReservationSearchErrorHandlingTest`: End-to-end error handling validation
- Complete request/response cycle testing
- Proper error response format validation

### Test Scenarios Covered
- Valid search terms with various patterns
- Search term too short (< 2 characters)
- Search term too long (> 100 characters)
- Search terms with invalid characters
- Empty and null search parameters
- Backward compatibility with existing parameters
- Pagination and sorting with search
- Error response format validation

## Security Considerations

- Input sanitization prevents injection attacks
- Character filtering removes potentially malicious content
- Length limits prevent abuse and DoS attacks
- Proper error messages don't expose sensitive information
- Consistent error format aids in client-side error handling

## Performance Impact

- Single database query with optimized JOINs
- Database indexes on searchable fields
- Input validation occurs before database queries
- Consistent sub-500ms response times for typical datasets
- Efficient error handling without performance degradation