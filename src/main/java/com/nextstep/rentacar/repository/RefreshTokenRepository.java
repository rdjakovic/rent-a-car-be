package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Ends all of a user's sessions */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    /** Ends all of a user's sessions except one (e.g. the one that changed the password) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now"
            + " WHERE t.user.id = :userId AND t.revokedAt IS NULL AND t.id <> :keepId")
    int revokeAllForUserExcept(@Param("userId") Long userId, @Param("keepId") Long keepId,
                               @Param("now") OffsetDateTime now);

    /**
     * Housekeeping on sign-in: a user's sessions that have run out are no longer needed.
     * No clearAutomatically: sign-in still uses the loaded user (and its lazy branch) afterwards.
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId AND t.expiresAt < :now")
    int deleteExpiredForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
