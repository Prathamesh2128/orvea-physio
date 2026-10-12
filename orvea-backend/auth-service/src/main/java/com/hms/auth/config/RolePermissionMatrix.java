package com.hms.auth.config;

import static com.hms.common.enums.PermissionType.APPOINTMENT_CANCEL;
import static com.hms.common.enums.PermissionType.APPOINTMENT_CREATE;
import static com.hms.common.enums.PermissionType.APPOINTMENT_READ;
import static com.hms.common.enums.PermissionType.APPOINTMENT_UPDATE;
import static com.hms.common.enums.PermissionType.BILLING_CREATE;
import static com.hms.common.enums.PermissionType.BILLING_READ;
import static com.hms.common.enums.PermissionType.BILLING_UPDATE;
import static com.hms.common.enums.PermissionType.LYMPHEDEMA_COMPARE;
import static com.hms.common.enums.PermissionType.LYMPHEDEMA_CREATE;
import static com.hms.common.enums.PermissionType.LYMPHEDEMA_READ;
import static com.hms.common.enums.PermissionType.LYMPHEDEMA_UPDATE;
import static com.hms.common.enums.PermissionType.NOTIFICATION_SEND;
import static com.hms.common.enums.PermissionType.PATIENT_CREATE;
import static com.hms.common.enums.PermissionType.PATIENT_READ;
import static com.hms.common.enums.PermissionType.PATIENT_UPDATE;
import static com.hms.common.enums.PermissionType.PAYMENT_COLLECT;
import static com.hms.common.enums.PermissionType.PERMISSION_MANAGE;
import static com.hms.common.enums.PermissionType.REPORT_VIEW;
import static com.hms.common.enums.PermissionType.ROLE_MANAGE;
import static com.hms.common.enums.PermissionType.TREATMENT_CREATE;
import static com.hms.common.enums.PermissionType.TREATMENT_READ;
import static com.hms.common.enums.PermissionType.TREATMENT_UPDATE;

import java.util.EnumSet;
import java.util.Set;

import com.hms.common.enums.PermissionType;
import com.hms.common.enums.RoleType;

/**
 * Default role -> permission mapping. Seeded on first start. Per-clinic
 * customisation later: move this to an admin screen / clinic settings. PATIENT
 * permissions only allow own records; ownership is enforced in each downstream
 * service.
 */
public class RolePermissionMatrix {

	private RolePermissionMatrix() {
	}

	public static Set<PermissionType> forRole(RoleType role) {
		return switch (role) {
		case SUPER_ADMIN -> EnumSet.allOf(PermissionType.class);

		case ADMIN -> {
			Set<PermissionType> s = EnumSet.allOf(PermissionType.class);
			s.remove(PERMISSION_MANAGE);
			s.remove(ROLE_MANAGE);
			yield s;
		}

		case DOCTOR -> EnumSet.of(PATIENT_READ, PATIENT_UPDATE, APPOINTMENT_READ, APPOINTMENT_UPDATE, TREATMENT_CREATE,
				TREATMENT_READ, TREATMENT_UPDATE, LYMPHEDEMA_CREATE, LYMPHEDEMA_READ, LYMPHEDEMA_UPDATE,
				LYMPHEDEMA_COMPARE, REPORT_VIEW);

		case PHYSIOTHERAPIST -> EnumSet.of(PATIENT_READ, APPOINTMENT_READ, TREATMENT_CREATE, TREATMENT_READ,
				TREATMENT_UPDATE, LYMPHEDEMA_CREATE, LYMPHEDEMA_READ, LYMPHEDEMA_UPDATE, LYMPHEDEMA_COMPARE);

		case NURSE -> EnumSet.of(PATIENT_READ, APPOINTMENT_READ, TREATMENT_READ, LYMPHEDEMA_CREATE, LYMPHEDEMA_READ);

		case RECEPTIONIST -> EnumSet.of(PATIENT_CREATE, PATIENT_READ, PATIENT_UPDATE, APPOINTMENT_CREATE,
				APPOINTMENT_READ, APPOINTMENT_UPDATE, APPOINTMENT_CANCEL, BILLING_READ, NOTIFICATION_SEND);

		case BILLING_STAFF ->
			EnumSet.of(PATIENT_READ, BILLING_CREATE, BILLING_READ, BILLING_UPDATE, PAYMENT_COLLECT, REPORT_VIEW);

		case CLINIC_ASSISTANT -> EnumSet.of(PATIENT_READ, APPOINTMENT_READ);

		case PATIENT -> EnumSet.of(APPOINTMENT_CREATE, APPOINTMENT_READ, APPOINTMENT_CANCEL, TREATMENT_READ,
				LYMPHEDEMA_READ, BILLING_READ);

		};
	}
}
