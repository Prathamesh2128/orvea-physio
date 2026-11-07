package com.hms.staff.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.common.enums.RoleType;
import com.hms.staff.entity.Staff;
import com.hms.staff.enums.StaffStatus;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    boolean existsByUserId(UUID userId);

    @Query("""
            select s from Staff s
            where (:clinicId is null or s.clinicId = :clinicId)
              and (:type is null or s.staffType = :type)
              and (:status is null or s.status = :status)
              and (:q is null or lower(s.displayName) like lower(concat('%', :q, '%')))
            """)
    Page<Staff> search(@Param("type") RoleType type, @Param("status") StaffStatus status,
                       @Param("q") String q, @Param("clinicId") UUID clinicId, Pageable pageable);

    @Query("""
            select s from Staff s
            where (:clinicId is null or s.clinicId = :clinicId)
              and s.staffType in :types and s.status = :status
            order by s.displayName
            """)
    List<Staff> findTreating(@Param("types") Collection<RoleType> types, @Param("status") StaffStatus status,
                             @Param("clinicId") UUID clinicId);
}
