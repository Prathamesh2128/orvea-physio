package com.hms.patient.entity;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "condition_master", uniqueConstraints = @UniqueConstraint(name = "uk_condition_code", columnNames = "code"))
@SQLRestriction("is_deleted = false")
public class ConditionMaster extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 40)
	private String category;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;
}
