package com.loanmanager.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats amounts with Indian digit grouping (12,34,567.00) and a plain "Rs" prefix
 * that renders on every terminal. Java's DecimalFormat only supports a single grouping
 * size, so the lakh/crore grouping is done by hand.
 */
public final class Money {

    private Money() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        String digits = value.abs().toPlainString();          // e.g. 1234567.89
        String whole = digits.substring(0, digits.indexOf('.'));
        String fraction = digits.substring(digits.indexOf('.'));
        return (value.signum() < 0 ? "-" : "") + "Rs " + groupIndian(whole) + fraction;
    }

    public static String percent(BigDecimal rate) {
        return rate == null ? "-" : rate.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public static String percent(double rate) {
        return percent(BigDecimal.valueOf(rate));
    }

    /** 1234567 -> 12,34,567 : last three digits, then groups of two. */
    static String groupIndian(String whole) {
        if (whole.length() <= 3) {
            return whole;
        }
        String last3 = whole.substring(whole.length() - 3);
        String head = whole.substring(0, whole.length() - 3);
        StringBuilder sb = new StringBuilder();
        int firstGroup = head.length() % 2;
        if (firstGroup == 1) {
            sb.append(head.charAt(0));
        }
        for (int i = firstGroup; i < head.length(); i += 2) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(head, i, i + 2);
        }
        return sb + "," + last3;
    }
}
