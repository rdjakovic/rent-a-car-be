package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.enums.UserRole;
import com.nextstep.rentacar.dto.request.ResetPasswordRequestDto;
import com.nextstep.rentacar.dto.request.UserCreateRequestDto;
import com.nextstep.rentacar.dto.request.UserUpdateRequestDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import com.nextstep.rentacar.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User accounts. Managers can look at them and create STAFF accounts; everything else needs an administrator.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User accounts: roles, branch, disabling, passwords")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Optional search (username, email or name; partial, case-insensitive) and role")
    public ResponseEntity<Page<UserResponseDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return ResponseEntity.ok(userService.search(search, role, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a user", description = "Managers can create STAFF; managers and administrators need an administrator")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data, or no branch for staff or managers"),
            @ApiResponse(responseCode = "403", description = "A manager tried to create a manager or administrator"),
            @ApiResponse(responseCode = "404", description = "Branch not found"),
            @ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserCreateRequestDto request,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request, authentication));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user", description = "Replaces email, name, phone, role, branch and enabled. "
            + "Disabling ends the user's sessions at once")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid data, or no branch for staff or managers"),
            @ApiResponse(responseCode = "404", description = "User or branch not found"),
            @ApiResponse(responseCode = "409", description = "Email already in use, or an administrator tried to disable or demote themselves")
    })
    public ResponseEntity<UserResponseDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody UserUpdateRequestDto request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(userService.update(id, request, authentication));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Set a new password for a user", description = "Ends all of the user's sessions and lifts a sign-in lock")
    @ApiResponse(responseCode = "204", description = "Password set")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequestDto request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
