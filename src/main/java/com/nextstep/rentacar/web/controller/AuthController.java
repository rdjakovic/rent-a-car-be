package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.dto.request.ChangePasswordRequestDto;
import com.nextstep.rentacar.dto.request.LoginRequestDto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Sign-in sessions: a short-lived access token (JWT, sent as "Authorization: Bearer ...") plus a refresh token
 * in an httpOnly cookie that renews it. The cookie only goes to these endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Sign-in, sessions and the current user")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookies refreshCookies;

    @PostMapping("/login")
    @Operation(summary = "Sign in", description = "Sign in with username or email. Returns an access token "
            + "and sets the refresh cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signed in"),
            @ApiResponse(responseCode = "400", description = "Username or password missing"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts; try again later")
    })
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        return signedIn(authService.login(loginRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renew the access token", description = "Uses the refresh cookie (no access token "
            + "needed). Returns a new access token and replaces the cookie; the session still ends 12 hours after sign-in")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session renewed"),
            @ApiResponse(responseCode = "401", description = "No valid session: sign in again")
    })
    public ResponseEntity<LoginResponseDto> refresh(
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        return signedIn(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out", description = "Ends the session of the refresh cookie and deletes the cookie")
    @ApiResponse(responseCode = "204", description = "Signed out (also when there was no session)")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change my password", description = "Needs the current password. Ends the user's "
            + "other sessions; this one stays")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Current password incorrect, or new password invalid"),
            @ApiResponse(responseCode = "401", description = "Not signed in")
    })
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequestDto request,
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        authService.changePassword(authentication.getName(), request, refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "The signed-in user with roles, permissions and branch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        log.debug("Get current user request for: {}", authentication.getName());
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    private ResponseEntity<LoginResponseDto> signedIn(AuthService.SignedIn signedIn) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookies.create(signedIn.refreshToken(), signedIn.sessionEndsAt()).toString())
                .body(signedIn.response());
    }
}
