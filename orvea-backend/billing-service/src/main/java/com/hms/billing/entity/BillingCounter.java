package com.hms.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Number series per clinic, prefix and financial year, e.g. id =
 * "INV:DEFAULT:2026-27".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "billing_counters")
public class BillingCounter {
	@Id
	@Column(length = 80)
	private String id;

	@Column(name = "last_number", nullable = false)
	private long lastNumber;
}