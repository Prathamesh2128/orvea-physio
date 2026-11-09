package com.hms.measurement.repository;

import com.hms.measurement.entity.ProtocolTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProtocolTemplateRepository extends JpaRepository<ProtocolTemplate, UUID> {

	@Query("""
			select t from ProtocolTemplate t
			where t.active = true and (t.clinicId = :clinicId or t.clinicId is null)
			  and (t.ownerUserId is null or t.ownerUserId = :userId)
			order by t.name
			""")
	List<ProtocolTemplate> findVisible(@Param("clinicId") UUID clinicId, @Param("userId") UUID userId);
}