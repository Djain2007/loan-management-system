package com.loanmanager.service;

import com.loanmanager.model.EmiDetails;
import com.loanmanager.model.EmiDetails.Instalment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmiCalculatorTest {

    @Test
    void matchesTheTextbookExample() {
        // 1,00,000 at 10% p.a. for 12 months is the standard worked example: EMI 8,791.59
        EmiDetails d = EmiCalculator.calculate(new BigDecimal("100000"), new BigDecimal("10"), 12);

        assertEquals(new BigDecimal("8791.59"), d.emi());
        assertEquals(12, d.schedule().size());
        assertEquals(0, d.schedule().get(11).balance().signum(), "balance must end at zero");
        assertEquals(d.totalPayable(), new BigDecimal("100000").add(d.totalInterest()));
    }

    @Test
    void principalPartsAddUpToThePrincipal() {
        BigDecimal principal = new BigDecimal("2500000");
        EmiDetails d = EmiCalculator.calculate(principal, new BigDecimal("7.25"), 240);

        BigDecimal paidPrincipal = d.schedule().stream()
                .map(Instalment::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidInterest = d.schedule().stream()
                .map(Instalment::interest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, principal.compareTo(paidPrincipal));
        assertEquals(0, d.totalInterest().compareTo(paidInterest));
        assertTrue(d.totalInterest().signum() > 0);
    }

    @Test
    void zeroInterestSplitsPrincipalEvenly() {
        EmiDetails d = EmiCalculator.calculate(new BigDecimal("12000"), BigDecimal.ZERO, 12);

        assertEquals(new BigDecimal("1000.00"), d.emi());
        assertEquals(0, d.totalInterest().signum());
        assertEquals(new BigDecimal("12000.00"), d.totalPayable().setScale(2));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> EmiCalculator.calculate(BigDecimal.ZERO, BigDecimal.TEN, 12));
        assertThrows(IllegalArgumentException.class,
                () -> EmiCalculator.calculate(BigDecimal.TEN, BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> EmiCalculator.calculate(BigDecimal.TEN, new BigDecimal("-1"), 12));
    }
}
