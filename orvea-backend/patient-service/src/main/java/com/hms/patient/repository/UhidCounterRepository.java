package com.hms.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.UhidCounter;

public interface UhidCounterRepository extends JpaRepository<UhidCounter, String> {

}
