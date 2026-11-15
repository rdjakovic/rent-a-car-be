package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.config.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The refresh token cookie: httpOnly (scripts can't read it), SameSite=Strict (not sent by other sites)
 * and limited to the auth endpoints, so ordinary API calls never carry it.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookies {

    public static final String NAME = "refresh_token";
    static final String PATH = "/api/v1/auth";

    private final JwtProperties jwtProperties;

    public ResponseCookie create(String token, OffsetDateTime expiresAt) {
        Duration maxAge = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), expiresAt);
        return builder(token).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    /** Tells the browser to delete the cookie */
    public ResponseCookie clear() {
        return builder("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
