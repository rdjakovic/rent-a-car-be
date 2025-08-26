package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import com.nextstep.rentacar.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@Validated
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    public ResponseEntity<BranchResponseDto> create(@Valid @RequestBody BranchRequestDto request) {
        return ResponseEntity.ok(branchService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchResponseDto> update(@PathVariable Long id, @Valid @RequestBody BranchRequestDto request) {
        return ResponseEntity.ok(branchService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<BranchResponseDto>> list(Pageable pageable) {
        return ResponseEntity.ok(branchService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BranchResponseDto>> searchByName(@RequestParam String name, Pageable pageable) {
        return ResponseEntity.ok(branchService.searchByName(name, pageable));
    }

    @GetMapping("/by-city")
    public ResponseEntity<List<BranchResponseDto>> byCity(@RequestParam String city) {
        return ResponseEntity.ok(branchService.findByCity(city));
    }

    @GetMapping("/by-country")
    public ResponseEntity<List<BranchResponseDto>> byCountry(@RequestParam String country) {
        return ResponseEntity.ok(branchService.findByCountry(country));
    }
}
