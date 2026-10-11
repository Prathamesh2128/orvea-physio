package com.hms.auth.entity;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;
import com.hms.common.enums.PermissionType;

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
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@SQLRestriction("is_deleted = false")
public class Permission extends BaseEntity{
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 60)
	private PermissionType name;
	
	@Column(length = 40)
	private String module;
	
	@Column(length = 225)
	private String description;
}
