package com.hms.measurement.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable limb protocol. ownerUserId null = shared by the clinic; otherwise
 * a therapist's personal preset.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "protocol_templates", indexes = @Index(name = "idx_template_owner", columnList = "owner_user_id"))
@SQLRestriction("is_deleted = false")
public class ProtocolTemplate extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "limb_type", nullable = false, length = 10)
	private LimbType limbType;

	@Column(name = "step_cm", nullable = false, precision = 5, scale = 2)
	private BigDecimal stepCm;

	@Enumerated(EnumType.STRING)
	@Column(name = "start_landmark", nullable = false, length = 20)
	private Landmark startLandmark;

	@Column(name = "point_count", nullable = false)
	private int pointCount;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "owner_user_id", columnDefinition = "CHAR(36)")
	private UUID ownerUserId;

	@Column(nullable = false)
	private boolean active = true;
}