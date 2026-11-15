package com.nextstep.rentacar.config.security;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties implements InitializingBean {

    static final int MIN_SECRET_LENGTH = 32;

    /**
     * JWT secret key for HMAC signing (set via JWT_SECRET). No default: the app must not
     * start with a well-known key. HS256 needs at least 256 bits.
     */
    @ToString.Exclude
    private String secret;

    /**
     * Access token lifetime: short, because an access token can't be revoked (the refresh token renews it)
     */
    private Duration expiration = Duration.ofMinutes(15);

    /**
     * Sign-in session length (refresh token): one working shift from sign-in, not extended by refreshing
     */
    private Duration refreshExpiration = Duration.ofHours(12);

    /**
     * Send the refresh cookie only over HTTPS. Off for local development over plain http.
     */
    private boolean refreshCookieSecure = true;

    /**
     * JWT issuer
     */
    private String issuer = "rent-a-car-app";

    /**
     * JWT audience
     */
    private String audience = "rent-a-car-users";

    /**
     * Algorithm used for signing
     */
    private String algorithm = "HS256";

    /**
     * Validated here instead of with bean-validation annotations: Spring Boot's startup failure
     * report prints the rejected property value, which would leak the secret into the logs.
     */
    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.jwt.secret must be set (JWT_SECRET)");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_LENGTH
                    + " characters (256 bits) for HS256, but has " + secret.length());
        }
    }
}
