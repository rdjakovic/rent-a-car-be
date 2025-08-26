package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.config.security.JwtProperties;
import com.nextstep.rentacar.domain.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generate a JWT token for the given user
     */
    public String generateToken(User user) {
        try {
            JWSSigner signer = new MACSigner(jwtProperties.getSecret());

            Instant now = Instant.now();
            Instant expiration = now.plus(jwtProperties.getExpiration());

            Set<String> roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issuer(jwtProperties.getIssuer())
                    .audience(jwtProperties.getAudience())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .claim("username", user.getUsername())
                    .claim("email", user.getEmail())
                    .claim("roles", roles)
                    .claim("enabled", user.getEnabled())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.parse(jwtProperties.getAlgorithm())).build(),
                    claimsSet);

            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Generate a JWT token from authentication
     */
    public String generateToken(Authentication authentication) {
        try {
            JWSSigner signer = new MACSigner(jwtProperties.getSecret());

            Instant now = Instant.now();
            Instant expiration = now.plus(jwtProperties.getExpiration());

            Set<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(authentication.getName())
                    .issuer(jwtProperties.getIssuer())
                    .audience(jwtProperties.getAudience())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .claim("authorities", authorities)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.parse(jwtProperties.getAlgorithm())).build(),
                    claimsSet);

            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Validate and parse JWT token
     */
    public JWTClaimsSet validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtProperties.getSecret());

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                throw new RuntimeException("JWT token has expired");
            }

            // Check issuer
            if (!jwtProperties.getIssuer().equals(claims.getIssuer())) {
                throw new RuntimeException("Invalid JWT issuer");
            }

            return claims;

        } catch (ParseException | JOSEException e) {
            log.error("Error validating JWT token", e);
            throw new RuntimeException("Failed to validate JWT token", e);
        }
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        JWTClaimsSet claims = validateToken(token);
        try {
            String username = claims.getStringClaim("username");
            return username != null ? username : claims.getSubject();
        } catch (ParseException e) {
            log.error("Error extracting username from JWT token", e);
            return claims.getSubject();
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            JWTClaimsSet claims = validateToken(token);
            Date expirationTime = claims.getExpirationTime();
            return expirationTime != null && expirationTime.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get token expiration time
     */
    public OffsetDateTime getTokenExpiration(String token) {
        JWTClaimsSet claims = validateToken(token);
        Date expirationTime = claims.getExpirationTime();
        return expirationTime != null ? 
            expirationTime.toInstant().atOffset(ZoneOffset.UTC) : 
            OffsetDateTime.now().plusSeconds(jwtProperties.getExpiration().toSeconds());
    }

    /**
     * Get token expiration duration in seconds
     */
    public long getTokenExpirationSeconds() {
        return jwtProperties.getExpiration().toSeconds();
    }
}
