package com.hms.billing.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "10 therapy sessions for ₹X, valid 90 days". */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "package_templates")
@SQLRestriction("is_deleted = false")
public class PackageTemplate extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "service_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID serviceId;

	@Column(name = "session_count", nullable = false)
	private int sessionCount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "validity_days", nullable = false)
	private int validityDays;

	@Column(nullable = false)
	private boolean active = true;
}