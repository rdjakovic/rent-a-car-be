package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.enums.ReservationStatus;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import com.nextstep.rentacar.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservations")
@Validated
@Tag(name = "Reservations", description = "Reservation management operations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(
        summary = "Create a new reservation",
        description = "Creates a new car reservation with automatic price calculation and availability validation"
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "Reservation created successfully", 
                content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Invalid request data or validation errors", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "409", 
                description = "Car not available for selected dates", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            )
    })
    public ResponseEntity<ReservationResponseDto> create(@Valid @RequestBody ReservationRequestDto request) {
        return ResponseEntity.ok(reservationService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing reservation",
        description = "Updates a reservation. Only PENDING reservations can be modified."
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "Reservation updated successfully", 
                content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Invalid request data", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Reservation not found", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "409", 
                description = "Reservation cannot be updated (not PENDING) or car not available", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            )
    })
    public ResponseEntity<ReservationResponseDto> update(
            @Parameter(description = "Reservation ID", example = "123")
            @PathVariable Long id, 
            @Valid @RequestBody ReservationRequestDto request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get reservation by ID",
        description = "Retrieves a specific reservation by its unique identifier"
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "Reservation found", 
                content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Reservation not found", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            )
    })
    public ResponseEntity<ReservationResponseDto> getById(
            @Parameter(description = "Reservation ID", example = "123")
            @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(
        summary = "Cancel a reservation",
        description = "Cancels a reservation, changing its status to CANCELLED"
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "Reservation cancelled successfully", 
                content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Reservation not found", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "409", 
                description = "Reservation cannot be cancelled", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            )
    })
    public ResponseEntity<ReservationResponseDto> cancel(
            @Parameter(description = "Reservation ID", example = "123")
            @PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(
        summary = "Confirm a reservation",
        description = "Confirms a PENDING reservation, changing its status to CONFIRMED"
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "Reservation confirmed successfully", 
                content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Reservation not found", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "409", 
                description = "Reservation cannot be confirmed", 
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            )
    })
    public ResponseEntity<ReservationResponseDto> confirm(
            @Parameter(description = "Reservation ID", example = "123")
            @PathVariable Long id) {
        reservationService.confirm(id);
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completed", content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<ReservationResponseDto> complete(@PathVariable Long id) {
        reservationService.complete(id);
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping
    @Operation(
        summary = "List reservations with optional filters, pagination and sorting", 
        description = """
            Retrieve reservations with comprehensive filtering and search capabilities.
            
            **Search Functionality:**
            The search parameter enables unified searching across multiple fields in a single API call:
            - Customer information: first name, last name, full name, email address, phone number
            - Reservation details: reservation ID (exact match prioritized)
            - Car information: display name, model
            - Branch information: branch name
            
            **Search Behavior:**
            - Case-insensitive partial matching across all searchable fields
            - Minimum 2 characters required for search terms
            - Maximum 100 characters allowed
            - Exact reservation ID matches are prioritized in results
            - Results ordered by relevance (exact ID matches first, then by creation date)
            
            **Performance:**
            - Single optimized database query with JOINs
            - Database indexes ensure sub-500ms response times
            - Supports concurrent search operations
            
            **Backward Compatibility:**
            - All existing filter parameters remain functional
            - When search parameter is provided, it takes precedence over customer-based filtering
            - Pagination and sorting work seamlessly with search results
            """,
        tags = {"Reservations"}
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "Successfully retrieved reservations",
                content = @Content(
                    schema = @Schema(implementation = Page.class),
                    mediaType = "application/json"
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Invalid request parameters",
                content = @Content(
                    schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                    mediaType = "application/json",
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Search term too short",
                            summary = "Search term validation error",
                            value = """
                                {
                                  "type": "about:blank",
                                  "title": "Bad Request",
                                  "status": 400,
                                  "detail": "Search term must be at least 2 characters long",
                                  "path": "/api/reservations"
                                }
                                """
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Search term too long",
                            summary = "Search term length validation error",
                            value = """
                                {
                                  "type": "about:blank",
                                  "title": "Bad Request",
                                  "status": 400,
                                  "detail": "Search term cannot exceed 100 characters",
                                  "path": "/api/reservations"
                                }
                                """
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Invalid characters",
                            summary = "Search term contains invalid characters",
                            value = """
                                {
                                  "type": "about:blank",
                                  "title": "Bad Request",
                                  "status": 400,
                                  "detail": "Search term contains only invalid characters",
                                  "path": "/api/reservations"
                                }
                                """
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Invalid date range",
                            summary = "Date range validation error",
                            value = """
                                {
                                  "type": "about:blank",
                                  "title": "Bad Request",
                                  "status": 400,
                                  "detail": "Invalid date range: endDate must be on/after startDate",
                                  "path": "/api/reservations"
                                }
                                """
                        )
                    }
                )
            )
    })
    public ResponseEntity<Page<ReservationResponseDto>> listWithFilters(
            @Parameter(
                description = "Filter by specific customer ID",
                example = "123"
            )
            @RequestParam(required = false) Long customerId,
            
            @Parameter(
                description = "Filter by specific car ID", 
                example = "456"
            )
            @RequestParam(required = false) Long carId,
            
            @Parameter(
                description = "Filter by reservation status",
                example = "CONFIRMED"
            )
            @RequestParam(required = false) ReservationStatus status,
            
            @Parameter(
                description = "Filter by pickup/dropoff branch ID",
                example = "789"
            )
            @RequestParam(required = false) Long branchId,
            
            @Parameter(
                description = "Filter reservations starting from this date (inclusive)",
                example = "2024-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(
                description = "Filter reservations ending before this date (inclusive)",
                example = "2024-12-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(
                description = """
                    Search term to find reservations across multiple fields. Searches:
                    - Customer: first name, last name, email, phone number
                    - Reservation: ID (exact matches prioritized)
                    - Car: display name, model
                    - Branch: name
                    
                    Requirements:
                    - Minimum 2 characters
                    - Maximum 100 characters
                    - Case-insensitive partial matching
                    - Alphanumeric characters, spaces, hyphens, dots, @ symbols allowed
                    """,
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Customer name", value = "John Smith"),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Email search", value = "john@example.com"),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Phone search", value = "555-0123"),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Reservation ID", value = "12345"),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Car model", value = "Toyota Camry"),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Branch name", value = "Downtown")
                }
            )
            @RequestParam(required = false) String search,
            
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(reservationService.listWithFilters(customerId, carId, status, branchId, startDate, endDate, search, pageable));
    }
}
