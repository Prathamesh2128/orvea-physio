package com.hms.patient.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.patient.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

	boolean existsByUserId(UUID userId);

	Optional<Patient> findByUserId(UUID userId);

	Optional<Patient> findByUhidIgnoreCaseAndClinicId(String uhid, UUID clinicId);

	@Query("""
			select p from Patient p
			where (:clinicId is null or p.clinicId = :clinicId)
			  and (:phone is null or p.phone = :phone or p.alternatePhone = :phone)
			  and (:uhid is null or p.uhid = :uhid)
			  and (:name is null or lower(concat(p.firstName, ' ', coalesce(p.lastName, ''))) like lower(concat('%', :name, '%')))
			""")
	Page<Patient> search(@Param("phone") String phone, @Param("name") String name, @Param("uhid") String uhid,
			@Param("clinicId") UUID ClinicScope, Pageable pageable);

	@Query("""
			SELECT p FROM Patient p
			WHERE LOWER(p.firstName) = lower(:first) AND LOWER(COALESCE(p.lastName, '')) = LOWER(:last)
			  AND p.dateOfBirth = :dob AND (:clinicId IS null OR p.clinicId = :clinicId)
			""")
	List<Patient> findByNameAndDob(@Param("first") String first, @Param("last") String last,
			@Param("dob") LocalDate dob, @Param("clinicId") UUID clinicId);

	@Query("""
			SELECT p FROM Patient p
			WHERE p.phone = :phone AND LOWER(p.firstName) = LOWER(:first)
			AND (:clinicId IS null OR p.clinicId = :clinicId)
			""")
	List<Patient> findByPhoneAndFirstName(@Param("phone") String phone, @Param("first") String first,
			@Param("clinicId") UUID clinicId);

	@Query("SELECT p FROM Patient p WHERE LOWER(p.email) = LOWER(:email) AND (:clinicId is null or p.clinicId = :clinicId)")
	List<Patient> findByEmail(@Param("email") String email, @Param("clinicId") UUID clinicId);
}
