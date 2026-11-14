package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.dto.request.MaintenanceCompleteRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceUpdateRequestDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.dto.response.MaintenanceResponseDto;
import com.nextstep.rentacar.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@Validated
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/schedule")
    @Operation(summary = "Schedule a maintenance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scheduled", content = @Content(schema = @Schema(implementation = MaintenanceResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation/Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<MaintenanceResponseDto> schedule(@Valid @RequestBody MaintenanceScheduleRequestDto request) {
        return ResponseEntity.ok(maintenanceService.schedule(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a maintenance record")
    public ResponseEntity<MaintenanceResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a maintenance record that is still SCHEDULED")
    public ResponseEntity<MaintenanceResponseDto> update(@PathVariable Long id,
                                                         @Valid @RequestBody MaintenanceUpdateRequestDto request) {
        return ResponseEntity.ok(maintenanceService.update(id, request));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<MaintenanceResponseDto> start(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.start(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a maintenance in progress, optionally recording cost, odometer, notes and the next service date")
    public ResponseEntity<MaintenanceResponseDto> complete(@PathVariable Long id,
                                                           @Valid @RequestBody(required = false) MaintenanceCompleteRequestDto details) {
        return ResponseEntity.ok(maintenanceService.complete(id, details));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MaintenanceResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.cancel(id));
    }

    @GetMapping
    @Operation(summary = "List maintenance records with optional filters, pagination and sorting")
    public ResponseEntity<Page<MaintenanceResponseDto>> listWithFilters(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenanceType maintenanceType,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.listByFilters(carId, employeeId, status, maintenanceType, branchId, startDate, endDate, pageable));
    }

    @GetMapping("/due-cars")
    @Operation(summary = "Cars whose next service is due within the given days (or overdue) and that have no open maintenance")
    public ResponseEntity<List<CarResponseDto>> carsDueForService(
            @RequestParam(defaultValue = "14") @Min(0) @Max(365) int withinDays) {
        return ResponseEntity.ok(maintenanceService.findCarsDueForService(withinDays));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<MaintenanceResponseDto>> scheduledForDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(maintenanceService.findScheduledForDate(date));
    }
}
