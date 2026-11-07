package com.hms.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per provider-day ("providerId:date"). Locked FOR UPDATE to serialise
 * bookings.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "slot_locks")
public class SlotLock {
	@Id
	@Column(length = 80)
	private String id;
}
