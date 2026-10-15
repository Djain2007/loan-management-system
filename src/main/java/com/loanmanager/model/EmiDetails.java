package com.loanmanager.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of an EMI calculation.
 *
 * @param emi           fixed monthly instalment
 * @param totalInterest interest paid over the full tenure
 * @param totalPayable  principal + total interest
 * @param schedule      month-by-month repayment breakdown
 */
public record EmiDetails(
        BigDecimal emi,
        BigDecimal totalInterest,
        BigDecimal totalPayable,
        List<Instalment> schedule) {

    /** One row of the repayment schedule. */
    public record Instalment(
            int month,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal balance) {
    }
}
