package com.hms.billing.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLRestriction;

import com.hms.billing.enums.ServiceCategory;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_items", uniqueConstraints = @UniqueConstraint(name = "uk_service_clinic_code", columnNames = {
		"clinic_id", "code" }))
@SQLRestriction("is_deleted = false")
public class ServiceItem extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;
	@Column(nullable = false, length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private ServiceCategory category = ServiceCategory.THERAPY;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	/** 0 unless the clinic sets it. */
	@Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal taxRatePercent = BigDecimal.ZERO.setScale(2);

	/**
	 * Appointment visit type code (e.g. THERAPY_SESSION): completed visits of this
	 * type are charged at this price.
	 */
	@Column(name = "visit_type_code", length = 40)
	private String visitTypeCode;

	@Column(nullable = false)
	private boolean active = true;
	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;
}