package com.loanmanager.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanTypeTest {

    @Test
    void bestScoreGetsTheLowestRateInTheBand() {
        assertEquals(new BigDecimal("6.50"), LoanType.HOME.rateFor(900));
        assertEquals(new BigDecimal("4.00"), LoanType.EDUCATION.rateFor(900));
    }

    @Test
    void worstScoreGetsTheHighestRateInTheBand() {
        assertEquals(new BigDecimal("8.50"), LoanType.HOME.rateFor(600));
        assertEquals(new BigDecimal("12.00"), LoanType.PERSONAL.rateFor(600));
    }

    @Test
    void midScoreLandsInTheMiddle() {
        assertEquals(new BigDecimal("8.00"), LoanType.VEHICLE.rateFor(750)); // band 7 - 9
    }

    @Test
    void scoresOutsideTheRangeAreClamped() {
        assertEquals(LoanType.HOME.rateFor(600), LoanType.HOME.rateFor(300));
        assertEquals(LoanType.HOME.rateFor(900), LoanType.HOME.rateFor(999));
    }

    @Test
    void menuChoiceMapsInOrder() {
        assertEquals(LoanType.HOME, LoanType.fromMenuChoice(1));
        assertEquals(LoanType.VEHICLE, LoanType.fromMenuChoice(4));
        assertThrows(IllegalArgumentException.class, () -> LoanType.fromMenuChoice(5));
    }
}
