package com.hms.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.notification.entity.Notification;
import com.hms.notification.enums.MessageType;
import com.hms.notification.enums.NotificationStatus;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	boolean existsByDedupeKey(String dedupeKey);

	@Query("""
			select n from Notification n
			where (:clinicId is null or n.clinicId = :clinicId)
			  and (:status is null or n.status = :status)
			  and (:type is null or n.messageType = :type)
			""")
	Page<Notification> search(@Param("status") NotificationStatus status, @Param("type") MessageType type,
			@Param("clinicId") UUID clinicId, Pageable pageable);

	@Query("select n.id from Notification n where n.status = :status and n.attempts < :maxAttempts order by n.createdAt")
	List<UUID> findSendableIds(@Param("status") NotificationStatus status, @Param("maxAttempts") int maxAttempts,
			Pageable page);

	/** Atomic claim so two nodes never send the same message. */
	@Modifying(clearAutomatically = true)
	@Query("update Notification n set n.status = :to, n.attempts = n.attempts + 1 where n.id = :id and n.status = :from")
	int claim(@Param("id") UUID id, @Param("from") NotificationStatus from, @Param("to") NotificationStatus to);

	@Modifying(clearAutomatically = true)
	@Query("""
			update Notification n set n.status = :to, n.statusReason = :reason
			where n.appointmentId = :appointmentId and n.messageType in :types and n.status in :from
			""")
	int withdraw(@Param("appointmentId") UUID appointmentId, @Param("types") Collection<MessageType> types,
			@Param("from") Collection<NotificationStatus> from, @Param("to") NotificationStatus to,
			@Param("reason") String reason);
}
