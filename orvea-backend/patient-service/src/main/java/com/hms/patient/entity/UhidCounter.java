package com.hms.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per clinic+year, e.g. id = "DEFAULT:2026". Locked with SELECT ... FOR
 * UPDATE.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "uhid_counters")
public class UhidCounter {

	@Id
	@Column(length = 60)
	private String id;

	// "last_value" is a reserved word in MySQL 8, so a different column name is
	// used
	@Column(name = "last_number", nullable = false)
	private long lastNumber;
}
