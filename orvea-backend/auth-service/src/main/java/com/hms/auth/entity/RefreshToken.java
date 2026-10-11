package com.hms.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "token_hash"))
@SQLRestriction("is_deleted = false")
public class RefreshToken extends BaseEntity {

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private UserSession session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(nullable = false)
	private boolean revoked = false;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "replaced_by_hash", length = 64)
	private String replacedByHash;
}
