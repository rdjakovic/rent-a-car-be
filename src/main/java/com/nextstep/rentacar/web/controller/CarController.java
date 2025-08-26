package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.service.CarService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
    public ResponseEntity<CarResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CarListResponseDto>> list(Pageable pageable) {
        return ResponseEntity.ok(carService.list(pageable));
    }

    @GetMapping("/deleted")
    public ResponseEntity<Page<CarListResponseDto>> listDeleted(Pageable pageable) {
        return ResponseEntity.ok(carService.listDeleted(pageable));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Page<CarListResponseDto>> listByBranch(@PathVariable Long branchId, Pageable pageable) {
        return ResponseEntity.ok(carService.listByBranch(branchId, pageable));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<CarListResponseDto>> findAvailable(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) CarCategory category,
            @RequestParam(required = false) TransmissionType transmission,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(
                carService.findAvailable(branchId, startDate, endDate, category, transmission, fuelType, minSeats, maxPrice, pageable)
        );
    }
}
