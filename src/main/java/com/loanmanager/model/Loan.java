package com.loanmanager.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A loan application. EMI fields are {@code null} until the loan is approved;
 * {@code rejectionReason} is {@code null} unless it was rejected.
 * {@code applicantName} / {@code applicantEmail} are joined from the users table for display.
 */
public record Loan(
        int id,
        int userId,
        String applicantName,
        String applicantEmail,
        LoanType type,
        BigDecimal amount,
        int tenureMonths,
        BigDecimal interestRate,
        BigDecimal annualIncome,
        String purpose,
        LoanStatus status,
        BigDecimal emiAmount,
        BigDecimal totalInterest,
        BigDecimal totalPayable,
        String rejectionReason,
        LocalDateTime appliedAt,
        LocalDateTime decidedAt) {

    public boolean isPending() {
        return status == LoanStatus.PENDING;
    }

    public boolean isApproved() {
        return status == LoanStatus.APPROVED;
    }
}
