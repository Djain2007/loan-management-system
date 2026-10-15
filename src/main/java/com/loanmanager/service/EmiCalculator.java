package com.loanmanager.service;

import com.loanmanager.model.EmiDetails;
import com.loanmanager.model.EmiDetails.Instalment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard reducing-balance EMI:
 * <pre>EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)</pre>
 * where {@code r} is the monthly rate and {@code n} the tenure in months.
 * The last instalment absorbs any rounding so the balance ends at exactly zero.
 */
public final class EmiCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);

    private EmiCalculator() {
    }

    public static EmiDetails calculate(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (annualRatePercent == null || annualRatePercent.signum() < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Tenure must be at least 1 month");
        }

        BigDecimal monthlyRate = annualRatePercent.divide(MONTHS_TIMES_PERCENT, 12, RoundingMode.HALF_UP);
        BigDecimal emi = monthlyInstalment(principal, monthlyRate, months);

        List<Instalment> schedule = new ArrayList<>(months);
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        for (int month = 1; month <= months; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal principalPart = month == months
                    ? balance
                    : emi.subtract(interest).min(balance);
            balance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
            schedule.add(new Instalment(month, principalPart, interest, balance));
        }

        return new EmiDetails(emi, totalInterest, principal.add(totalInterest), List.copyOf(schedule));
    }

    private static BigDecimal monthlyInstalment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        double r = monthlyRate.doubleValue();
        double factor = Math.pow(1 + r, months);
        double emi = principal.doubleValue() * r * factor / (factor - 1);
        return BigDecimal.valueOf(emi).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
