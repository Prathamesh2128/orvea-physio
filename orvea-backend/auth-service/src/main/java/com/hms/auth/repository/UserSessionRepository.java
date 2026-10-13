package com.hms.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.auth.entity.UserSession;
import com.hms.auth.enums.SessionStatus;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
	List<UserSession> findByUserIdAndStatus(UUID userId, SessionStatus status);

	Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);
}
