package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import com.nextstep.rentacar.dto.request.CarFilterDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.service.CarService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/cars")
@Validated
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping
    @Operation(summary = "Create a car")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created", content = @Content(schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation/Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<CarResponseDto> create(@Valid @RequestBody CarRequestDto request) {
        return ResponseEntity.ok(carService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponseDto> update(@PathVariable Long id, @Valid @RequestBody CarRequestDto request) {
        return ResponseEntity.ok(carService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<CarResponseDto> restore(@PathVariable Long id) {
        carService.restore(id);
        return ResponseEntity.ok(carService.getById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a car by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<CarResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getById(id));
    }

    @GetMapping
    @Operation(
        summary = "List cars with pagination, sorting, and filtering",
        description = "Returns a paginated, sorted, and filtered list of cars. " +
                      "Filter by any combination of fields (vin, make, model, year, category, transmission, fuelType, minSeats, maxPrice). " +
                      "Sorting and pagination are supported via standard Spring Data parameters."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CarListResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Page<CarListResponseDto>> list(
            @ParameterObject @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Car filter parameters. All fields are optional.",
                content = @Content(schema = @Schema(implementation = CarFilterDto.class))
            ) CarFilterDto filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(carService.list(filter, pageable));
    }

    @GetMapping("/deleted")
    @Operation(summary = "List deleted cars with pagination and sorting")
    public ResponseEntity<Page<CarListResponseDto>> listDeleted(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(carService.listDeleted(pageable));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "List cars by branch with pagination and sorting")
    public ResponseEntity<Page<CarListResponseDto>> listByBranch(@PathVariable Long branchId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(carService.listByBranch(branchId, pageable));
    }

    @GetMapping("/available")
    @Operation(summary = "Find available cars by date range with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Page<CarListResponseDto>> findAvailable(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) CarCategory category,
            @RequestParam(required = false) TransmissionType transmission,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(required = false) BigDecimal maxPrice,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(
                carService.findAvailable(branchId, startDate, endDate, category, transmission, fuelType, minSeats, maxPrice, pageable)
        );
    }
}
