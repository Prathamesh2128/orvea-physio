package com.hms.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.auth.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

}
