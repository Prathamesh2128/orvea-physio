package com.hms.patient.util;

import java.time.LocalDate;
import java.time.Period;

import com.hms.patient.dto.CareTeamResponse;
import com.hms.patient.dto.CaseResponse;
import com.hms.patient.dto.ConditionMasterResponse;
import com.hms.patient.dto.ConditionResponse;
import com.hms.patient.dto.ConsentResponse;
import com.hms.patient.dto.PatientResponse;
import com.hms.patient.dto.PatientSummary;
import com.hms.patient.entity.CareTeamMember;
import com.hms.patient.entity.CaseCondition;
import com.hms.patient.entity.ConditionMaster;
import com.hms.patient.entity.Patient;
import com.hms.patient.entity.PatientConsent;
import com.hms.patient.entity.TreatmentCase;

public class PatientMapper {
	private PatientMapper() {
	}

	public static int age(LocalDate dob) {
		return Period.between(dob, LocalDate.now()).getYears();
	}

	public static PatientResponse toResponse(Patient p) {
		return new PatientResponse(p.getId(), p.getUhid(), p.getFirstName(), p.getLastName(), p.getFullName(),
				p.getGender(), p.getDateOfBirth(), age(p.getDateOfBirth()), p.isDobApproximate(), p.getBloodGroup(),
				p.getCountryCode(), p.getPhone(), p.getAlternatePhone(), p.getEmail(), p.getAddressLine(), p.getCity(),
				p.getState(), p.getPincode(), p.getOccupation(), p.getReferredBy(), p.getEmergencyContactName(),
				p.getEmergencyContactPhone(), p.getEmergencyContactRelation(), p.getUserId(), p.getStatus(),
				p.getRegisteredOn(), p.getNotes(), p.getCreatedAt());
	}

	public static PatientSummary toSummary(Patient p) {
		return new PatientSummary(p.getId(), p.getUhid(), p.getFullName(), p.getGender(), age(p.getDateOfBirth()),
				p.getPhone(), p.getStatus(), p.getRegisteredOn());
	}

	public static ConditionResponse toResponse(CaseCondition c) {
		return new ConditionResponse(c.getId(), c.getConditionCode(), c.getConditionName(), c.getNotes(),
				c.getDiagnosedOn(), c.isActive());
	}

	public static CaseResponse toResponse(TreatmentCase c) {
		return new CaseResponse(c.getId(), c.getPatient().getId(), c.getCaseNumber(), c.getDiagnosis(), c.getIcdCode(),
				c.getBodyRegion(), c.getLaterality(), c.getPrimaryTherapistId(), c.getPlannedSessions(),
				c.getCompletedSessions(), c.getStatus(), c.getStartDate(), c.getExpectedEndDate(), c.getClosedOn(),
				c.getClosureReason(), c.getNotes(), c.getConditions().stream().map(PatientMapper::toResponse).toList());
	}

	public static CareTeamResponse toResponse(CareTeamMember m) {
		return new CareTeamResponse(m.getId(), m.getTreatmentCase() == null ? null : m.getTreatmentCase().getId(),
				m.getRole(), m.getName(), m.getPhone(), m.getEmail(), m.getOrganization(), m.getRelationship(),
				m.isPrimaryContact(), m.getNotes());
	}

	public static ConsentResponse toResponse(PatientConsent c) {
		return new ConsentResponse(c.getId(), c.getConsentType(), c.getStatus(), c.getGivenBy(), c.getSignerName(),
				c.getConsentVersion(), c.getDocumentRef(), c.getGrantedAt(), c.getWithdrawnAt(),
				c.getWithdrawalReason(), c.getNotes());
	}

	public static ConditionMasterResponse toResponse(ConditionMaster m) {
		return new ConditionMasterResponse(m.getId(), m.getCode(), m.getName(), m.getCategory(), m.getDescription(),
				m.isActive(), m.getSortOrder());
	}
}
