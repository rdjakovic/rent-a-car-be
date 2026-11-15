package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.config.security.JwtProperties;
import com.nextstep.rentacar.domain.entity.RefreshToken;
import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.exception.SessionExpiredException;
import com.nextstep.rentacar.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Sign-in sessions. A session lasts {@link JwtProperties#getRefreshExpiration()} from sign-in; every refresh
 * replaces its token (rotation) but keeps that end. A token that was already replaced by a refresh coming back
 * means it was copied, so all of that user's sessions end. Tokens ended otherwise (sign-out, password change,
 * expiry) are simply refused.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    /** A refresh token together with the user it belongs to and when its session ends */
    public record Session(User user, String token, OffsetDateTime expiresAt) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /** Starts a new session for the user (on sign-in). */
    public Session start(User user) {
        OffsetDateTime now = now();
        refreshTokenRepository.deleteExpiredForUser(user.getId(), now);
        return issue(user, now.plus(jwtProperties.getRefreshExpiration()));
    }

    /**
     * Replaces the token with a new one for the same session.
     * Rolls back nothing on failure: a detected reuse must stay revoked.
     */
    @Transactional(noRollbackFor = SessionExpiredException.class)
    public Session rotate(String token) {
        RefreshToken current = find(token);
        OffsetDateTime now = now();
        User user = current.getUser();

        if (current.isRotated()) {
            log.warn("Reused refresh token for user {}: ending all of the user's sessions", user.getUsername());
            refreshTokenRepository.revokeAllForUser(user.getId(), now);
            throw new SessionExpiredException();
        }
        if (current.isRevoked()) {
            throw new SessionExpiredException();
        }
        if (!current.getExpiresAt().isAfter(now) || !Boolean.TRUE.equals(user.getEnabled())) {
            current.setRevokedAt(now);
            throw new SessionExpiredException();
        }

        current.setRevokedAt(now);
        current.setRotated(true);
        return issue(user, current.getExpiresAt());
    }

    /** Ends the session of this token (sign-out); unknown tokens are ignored. */
    public void end(String token) {
        refreshTokenRepository.findByTokenHash(hash(token))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.setRevokedAt(now()));
    }

    /** Ends all of the user's sessions (account disabled, password reset by an admin). */
    public void endAllSessions(User user) {
        refreshTokenRepository.revokeAllForUser(user.getId(), now());
    }

    /** Ends all of the user's sessions except the one of this token (if it is the user's). */
    public void endOtherSessions(User user, String keepToken) {
        Long keepId = keepToken == null ? null : refreshTokenRepository.findByTokenHash(hash(keepToken))
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(RefreshToken::getId)
                .orElse(null);
        if (keepId == null) {
            refreshTokenRepository.revokeAllForUser(user.getId(), now());
        } else {
            refreshTokenRepository.revokeAllForUserExcept(user.getId(), keepId, now());
        }
    }

    private RefreshToken find(String token) {
        if (token == null || token.isBlank()) {
            throw new SessionExpiredException();
        }
        return refreshTokenRepository.findByTokenHash(hash(token)).orElseThrow(SessionExpiredException::new);
    }

    private Session issue(User user, OffsetDateTime expiresAt) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshToken(user, hash(token), expiresAt));
        return new Session(user, token, expiresAt);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
