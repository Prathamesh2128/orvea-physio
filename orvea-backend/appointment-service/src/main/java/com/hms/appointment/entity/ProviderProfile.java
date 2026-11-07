package com.hms.appointment.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "provider_profiles", indexes = @Index(name = "idx_provider_profile", columnList = "provider_id"))
@SQLRestriction("is_deleted = false")
public class ProviderProfile extends BaseEntity {

	/** staff-service id (no cross-database FK). */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "provider_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID providerId;

	@Column(name = "provider_name", nullable = false, length = 200)
	private String providerName;

	@Column(name = "slot_minutes", nullable = false)
	private int slotMinutes = 15;

	/** How many patients this provider may treat at the same time. Default 1. */
	@Column(name = "max_parallel", nullable = false)
	private int maxParallel = 1;

	@Column(nullable = false)
	private boolean active = true;
}
