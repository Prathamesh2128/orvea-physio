package com.hms.appointment.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import com.hms.appointment.enums.ReminderChannel;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinic_scheduling_settings")
@SQLRestriction("is_deleted = false")
public class ClinicSchedulingSettings extends BaseEntity {

	@Column(name = "default_slot_minutes", nullable = false)
	private int defaultSlotMinutes = 15;

	@Column(name = "default_max_parallel", nullable = false)
	private int defaultMaxParallel = 1;

	@Column(name = "max_advance_booking_days", nullable = false)
	private int maxAdvanceBookingDays = 90;

	@Column(name = "reminder_enabled", nullable = false)
	private boolean reminderEnabled = true;

	/** Minutes before the visit, e.g. 1440 (24 h) and 120 (2 h). */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "clinic_reminder_offsets", joinColumns = @JoinColumn(name = "settings_id"))
	@OrderColumn(name = "position")
	@Column(name = "offset_minutes")
	private List<Integer> reminderOffsetsMinutes = new ArrayList<>();

	/** Preference order: first channel is tried first, the next is the fallback. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "clinic_reminder_channels", joinColumns = @JoinColumn(name = "settings_id"))
	@OrderColumn(name = "position")
	@Enumerated(EnumType.STRING)
	@Column(name = "channel", length = 15)
	private List<ReminderChannel> reminderChannels = new ArrayList<>();
}
