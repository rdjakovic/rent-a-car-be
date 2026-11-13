package com.nextstep.rentacar.config.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    /**
     * JWT secret key for HMAC signing (set via JWT_SECRET). No default: the app must not
     * start with a well-known key. HS256 needs at least 256 bits.
     */
    @NotBlank(message = "app.jwt.secret must be set (JWT_SECRET)")
    @Size(min = 32, message = "app.jwt.secret must be at least 32 characters (256 bits) for HS256")
    private String secret;

    /**
     * JWT token expiration duration
     */
    private Duration expiration = Duration.ofHours(24);

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
}
