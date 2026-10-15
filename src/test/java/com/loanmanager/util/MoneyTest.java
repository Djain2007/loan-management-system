package com.loanmanager.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void usesIndianDigitGrouping() {
        assertEquals("Rs 0.00", Money.format(BigDecimal.ZERO));
        assertEquals("Rs 999.00", Money.format(new BigDecimal("999")));
        assertEquals("Rs 1,000.00", Money.format(new BigDecimal("1000")));
        assertEquals("Rs 12,345.50", Money.format(new BigDecimal("12345.5")));
        assertEquals("Rs 1,00,000.00", Money.format(new BigDecimal("100000")));
        assertEquals("Rs 25,00,000.00", Money.format(new BigDecimal("2500000")));
        assertEquals("Rs 1,23,45,678.90", Money.format(new BigDecimal("12345678.9")));
        assertEquals("-Rs 1,500.25", Money.format(new BigDecimal("-1500.25")));
        assertEquals("-", Money.format(null));
    }

    @Test
    void percentAlwaysShowsTwoDecimals() {
        assertEquals("8.20%", Money.percent(new BigDecimal("8.2")));
        assertEquals("11.55%", Money.percent(new BigDecimal("11.55")));
        assertEquals("6.50%", Money.percent(6.5));
        assertEquals("-", Money.percent((BigDecimal) null));
    }
}
