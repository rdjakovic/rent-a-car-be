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
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
    @Operation(summary = "Create a branch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created", content = @Content(schema = @Schema(implementation = BranchResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation/Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
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
    @Operation(summary = "List branches with pagination and sorting")
    public ResponseEntity<Page<BranchResponseDto>> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(branchService.list(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search branches by name with pagination and sorting")
    public ResponseEntity<Page<BranchResponseDto>> searchByName(@RequestParam String name, @ParameterObject Pageable pageable) {
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

    @Operation(
            summary = "Find branches by name and city",
            description = "Returns a list of branches filtered by both name and city, with optional pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = BranchResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    @GetMapping("/by-name-and-city")
    public ResponseEntity<List<BranchResponseDto>> byNameAndCity(
            @RequestParam String name,
            @RequestParam String city,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(branchService.findByNameAndCity(name, city, pageable));
    }

}
