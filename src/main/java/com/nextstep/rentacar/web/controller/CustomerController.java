package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.response.CustomerResponseDto;
import com.nextstep.rentacar.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/customers")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Create a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created", content = @Content(schema = @Schema(implementation = CustomerResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation/Bad Request", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<CustomerResponseDto> create(@Valid @RequestBody CustomerRequestDto request) {
        return ResponseEntity.ok(customerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> update(@PathVariable Long id, @Valid @RequestBody CustomerRequestDto request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponseDto>> list(Pageable pageable) {
        return ResponseEntity.ok(customerService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponseDto>> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.search(email, firstName, lastName, city, pageable));
    }
}
