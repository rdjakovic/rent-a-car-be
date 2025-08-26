package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.entity.Maintenance;
import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.service.MaintenanceService;
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
import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@Validated
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/schedule")
    @Operation(summary = "Schedule a maintenance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scheduled", content = @Content(schema = @Schema(implementation = com.nextstep.rentacar.domain.entity.Maintenance.class))),
            @ApiResponse(responseCode = "400", description = "Validation/Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Maintenance> schedule(@Valid @RequestBody MaintenanceScheduleRequestDto request) {
        Maintenance m = maintenanceService.schedule(request.getCarId(), request.getType(), request.getDescription(), request.getScheduledDate());
        return ResponseEntity.ok(m);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Maintenance> start(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.start(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Maintenance> complete(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Maintenance> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.cancel(id));
    }

    @GetMapping
    public ResponseEntity<Page<Maintenance>> listWithFilters(
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenanceType maintenanceType,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.listByFilters(carId, employeeId, status, maintenanceType, branchId, startDate, endDate, pageable));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<Maintenance>> scheduledForDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(maintenanceService.findScheduledForDate(date));
    }
}
