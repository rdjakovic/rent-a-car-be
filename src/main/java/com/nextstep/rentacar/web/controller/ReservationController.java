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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservations")
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created", content = @Content(schema = @Schema(implementation = ReservationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation/Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<ReservationResponseDto> create(@Valid @RequestBody ReservationRequestDto request) {
        return ResponseEntity.ok(reservationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> update(@PathVariable Long id, @Valid @RequestBody ReservationRequestDto request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDto> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponseDto> confirm(@PathVariable Long id) {
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
    public ResponseEntity<Page<ReservationResponseDto>> listWithFilters(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(reservationService.listWithFilters(customerId, carId, status, branchId, startDate, endDate, pageable));
    }
}
