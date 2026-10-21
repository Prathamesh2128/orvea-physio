package com.hms.patient.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.CareTeamMember;

public interface CareTeamMemberRepository extends JpaRepository<CareTeamMember, UUID>{

}
