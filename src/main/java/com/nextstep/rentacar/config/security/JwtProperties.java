package com.nextstep.rentacar.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * JWT secret key for HMAC signing
     */
    private String secret = "mySecretKey12345678901234567890123456789012345678901234567890";

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
