package com.loanmanager.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Loan products offered by the system, each with an annual interest-rate band.
 * The applicant's CIBIL score decides where in the band the rate lands:
 * a score of {@value #MIN_SCORE} gets the maximum rate, {@value #MAX_SCORE} gets the minimum.
 */
public enum LoanType {
    HOME("Home Loan", 6.5, 8.5),
    PERSONAL("Personal Loan", 9.0, 12.0),
    EDUCATION("Education Loan", 4.0, 6.0),
    VEHICLE("Vehicle Loan", 7.0, 9.0);

    public static final int MIN_SCORE = 600;
    public static final int MAX_SCORE = 900;

    private final String label;
    private final double minRate;
    private final double maxRate;

    LoanType(String label, double minRate, double maxRate) {
        this.label = label;
        this.minRate = minRate;
        this.maxRate = maxRate;
    }

    public String label() {
        return label;
    }

    public double minRate() {
        return minRate;
    }

    public double maxRate() {
        return maxRate;
    }

    /** Annual interest rate (percent, 2 decimals) for the given CIBIL score. */
    public BigDecimal rateFor(int cibilScore) {
        int score = Math.max(MIN_SCORE, Math.min(MAX_SCORE, cibilScore));
        double position = (score - MIN_SCORE) / (double) (MAX_SCORE - MIN_SCORE); // 0.0 .. 1.0
        double rate = maxRate - position * (maxRate - minRate);
        return BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** Maps a 1-based menu choice (1 = HOME, 2 = PERSONAL, ...) to a loan type. */
    public static LoanType fromMenuChoice(int choice) {
        LoanType[] types = values();
        if (choice < 1 || choice > types.length) {
            throw new IllegalArgumentException("Invalid loan type selection: " + choice);
        }
        return types[choice - 1];
    }
}
