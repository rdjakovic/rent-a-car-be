package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.dto.request.SettingsRequestDto;
import com.nextstep.rentacar.dto.response.SettingsResponseDto;
import com.nextstep.rentacar.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @Operation(summary = "Get application settings")
    public ResponseEntity<SettingsResponseDto> get() {
        return ResponseEntity.ok(settingsService.get());
    }

    @PutMapping
    @Operation(summary = "Update application settings (admin only outside local/docker)")
    public ResponseEntity<SettingsResponseDto> update(@Valid @RequestBody SettingsRequestDto request) {
        return ResponseEntity.ok(settingsService.update(request));
    }
}
