package com.hms.billing.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.hms.common.exception.BusinessException;

/**
 * Line maths: tax is added on top of (price x quantity - discount). Everything
 * is rounded to 2 decimals, half-up.
 */
public final class InvoiceCalculator {
	private InvoiceCalculator() {
	}

	public static final BigDecimal MAX_PRICE = new BigDecimal("10000000");
	public static final BigDecimal MAX_TAX = new BigDecimal("40");

	public record LineInput(int quantity, BigDecimal unitPrice, BigDecimal discountAmount, BigDecimal taxRatePercent) {
	}

	public record LineResult(BigDecimal gross, BigDecimal discount, BigDecimal taxable, BigDecimal tax,
			BigDecimal total) {
	}

	public record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxableTotal, BigDecimal taxTotal,
			BigDecimal roundOff, BigDecimal grandTotal, List<LineResult> lines) {
	}

	public static BigDecimal round2(BigDecimal v) {
		return v.setScale(2, RoundingMode.HALF_UP);
	}

	public static Totals compute(List<LineInput> inputs, boolean roundToRupee) {
		BigDecimal subtotal = BigDecimal.ZERO.setScale(2), discountTotal = subtotal, taxableTotal = subtotal,
				taxTotal = subtotal;
		List<LineResult> results = new ArrayList<>();
		int n = 0;
		for (LineInput in : inputs) {
			n++;
			if (in.quantity() < 1 || in.quantity() > 100)
				throw new BusinessException("Item " + n + ": quantity must be 1 to 100");
			if (in.unitPrice() == null || in.unitPrice().signum() < 0 || in.unitPrice().compareTo(MAX_PRICE) > 0) {
				throw new BusinessException("Item " + n + ": the price must be between 0 and 1,00,00,000");
			}
			BigDecimal tax = in.taxRatePercent() == null ? BigDecimal.ZERO : in.taxRatePercent();
			if (tax.signum() < 0 || tax.compareTo(MAX_TAX) > 0)
				throw new BusinessException("Item " + n + ": the tax rate must be between 0 and 40 percent");
			BigDecimal gross = round2(in.unitPrice().multiply(BigDecimal.valueOf(in.quantity())));
			BigDecimal discount = round2(in.discountAmount() == null ? BigDecimal.ZERO : in.discountAmount());
			if (discount.signum() < 0 || discount.compareTo(gross) > 0) {
				throw new BusinessException(
						"Item " + n + ": the discount cannot be negative or more than the item amount");
			}
			BigDecimal taxable = gross.subtract(discount);
			BigDecimal taxAmount = round2(
					taxable.multiply(tax).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
			results.add(new LineResult(gross, discount, taxable, taxAmount, taxable.add(taxAmount)));
			subtotal = subtotal.add(gross);
			discountTotal = discountTotal.add(discount);
			taxableTotal = taxableTotal.add(taxable);
			taxTotal = taxTotal.add(taxAmount);
		}
		BigDecimal exact = taxableTotal.add(taxTotal);
		BigDecimal grand = roundToRupee ? exact.setScale(0, RoundingMode.HALF_UP).setScale(2) : exact;
		return new Totals(subtotal, discountTotal, taxableTotal, taxTotal, grand.subtract(exact), grand, results);
	}
}