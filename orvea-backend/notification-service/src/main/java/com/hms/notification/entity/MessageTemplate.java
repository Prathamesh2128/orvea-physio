package com.hms.notification.entity;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;
import com.hms.notification.enums.MessageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "message_templates", uniqueConstraints = @UniqueConstraint(name = "uk_template_clinic_type", columnNames = {
		"clinic_id", "message_type" }))
@SQLRestriction("is_deleted = false")
public class MessageTemplate extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false, length = 25)
	private MessageType messageType;

	/**
	 * Free text used by MANUAL_LINK and LOG_ONLY. Placeholders: {patientName}
	 * {date} {time} {doctor} {visit} {clinicName} {clinicPhone}.
	 */
	@Column(nullable = false, length = 1000)
	private String body;

	/** CLOUD_API only: the template name approved in Meta Business Manager. */
	@Column(name = "cloud_template_name", length = 100)
	private String cloudTemplateName;

	@Column(name = "cloud_language_code", length = 10)
	private String cloudLanguageCode = "en";

	/**
	 * CLOUD_API only: order of the {{1}}, {{2}}... variables, comma separated
	 * placeholder names.
	 */
	@Column(name = "cloud_param_order", length = 200)
	private String cloudParamOrder = "patientName,date,time,doctor,clinicName";

	@Column(nullable = false)
	private boolean active = true;
}