package com.hms.staff.utill;

import java.util.EnumSet;
import java.util.Set;

import com.hms.common.enums.RoleType;
import com.hms.staff.dto.StaffRef;
import com.hms.staff.dto.StaffResponse;
import com.hms.staff.entity.Staff;
import com.hms.staff.enums.StaffStatus;

public class StaffMapper {
	private StaffMapper() {
	}

	public static final Set<RoleType> TREATING_TYPES = EnumSet.of(RoleType.PHYSIOTHERAPIST, RoleType.DOCTOR);

	public static boolean bookable(Staff s) {
		return TREATING_TYPES.contains(s.getStaffType());
	}

	public static StaffResponse toResponse(Staff s) {
		return new StaffResponse(s.getId(), s.getUserId(), s.getFirstName(), s.getMiddleName(), s.getLastName(),
				s.getDisplayName(), s.getGender(), s.getStaffType(), s.getPhone(), s.getEmail(), s.getQualification(),
				s.getRegistrationNumber(), s.getSpecialization(), s.getExperienceYears(), s.getJoiningDate(),
				s.getStatus(), bookable(s), s.getCreatedAt());
	}

	public static StaffRef toRef(Staff s) {
		return new StaffRef(s.getId(), s.getDisplayName(), s.getStaffType().name(), s.getStatus() == StaffStatus.ACTIVE,
				bookable(s), s.getClinicId());
	}
}
