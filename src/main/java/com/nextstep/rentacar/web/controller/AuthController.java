package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.dto.request.LoginRequestDto;
import com.nextstep.rentacar.dto.request.RegisterRequestDto;
import com.nextstep.rentacar.dto.response.LoginResponseDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import com.nextstep.rentacar.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user management operations")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new customer account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto registerRequest) {
        log.info("Registration request for username: {}", registerRequest.getUsername());
        
        try {
            UserResponseDto userResponse = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (RuntimeException e) {
            log.error("Registration failed for username: {}, error: {}", registerRequest.getUsername(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid login credentials"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        log.info("Login request for: {}", loginRequest.getEmail());
        
        try {
            LoginResponseDto loginResponse = authService.login(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            log.error("Login failed for: {}, error: {}", loginRequest.getEmail(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        log.debug("Get current user request for: {}", authentication.getName());
        
        UserResponseDto userResponse = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Generate a new JWT token for authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<LoginResponseDto> refreshToken(Authentication authentication) {
        log.debug("Token refresh request for: {}", authentication.getName());
        
        // For simplicity, we'll just return a new token with current user data
        // In a more sophisticated implementation, you might want to validate the refresh token
        UserResponseDto userResponse = authService.getCurrentUser(authentication.getName());
        
        // Create a mock login response with refreshed token
        // In real implementation, you would generate a new JWT token here
        LoginResponseDto refreshResponse = new LoginResponseDto();
        refreshResponse.setUser(userResponse);
        // Note: In real implementation, generate a new JWT token here
        
        return ResponseEntity.ok(refreshResponse);
    }
}
