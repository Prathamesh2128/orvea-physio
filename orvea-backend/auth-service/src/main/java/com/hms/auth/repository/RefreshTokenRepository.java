package com.hms.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.session.Id = :sessionId AND r.revoked = false")
	int revokeAllBySession(@Param("sessionId") UUID sessionId, @Param("now") LocalDateTime now);
}
