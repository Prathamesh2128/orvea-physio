package com.hms.patient.repository;

import java.time.LocalDateTime;

public interface DeletedPatientView {
    String getId();
    String getClinicId();
    String getUserId();
    String getUhid();
    String getFirstName();
    String getLastName();
    String getPhone();
    LocalDateTime getDeletedAt();
    String getDeletedBy();
}
