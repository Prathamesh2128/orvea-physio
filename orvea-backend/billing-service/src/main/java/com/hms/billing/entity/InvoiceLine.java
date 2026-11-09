package com.hms.billing.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.billing.enums.LineType;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_lines", indexes = @Index(name = "idx_line_invoice", columnList = "invoice_id"))
@SQLRestriction("is_deleted = false")
public class InvoiceLine extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invoice_id", nullable = false)
	private Invoice invoice;

	@Column(name = "line_no", nullable = false)
	private int lineNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private LineType type;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "service_id", columnDefinition = "CHAR(36)")
	private UUID serviceId;
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "package_template_id", columnDefinition = "CHAR(36)")
	private UUID packageTemplateId;
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "charge_id", columnDefinition = "CHAR(36)")
	private UUID chargeId;

	/** Name used for revenue reports (service / package / product). */
	@Column(name = "item_name", nullable = false, length = 120)
	private String itemName;
	@Column(nullable = false, length = 255)
	private String description;

	@Column(nullable = false)
	private int quantity = 1;
	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;
	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);
	@Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal taxRatePercent = BigDecimal.ZERO.setScale(2);
	@Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal taxableAmount = BigDecimal.ZERO.setScale(2);
	@Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2);
	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal = BigDecimal.ZERO.setScale(2);
}