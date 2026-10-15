package com.loanmanager.service;

import com.loanmanager.model.LoanType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditBureauServiceTest {

    private final CreditBureauService bureau = new CreditBureauService();

    @Test
    void samePanAlwaysGetsTheSameScore() {
        assertEquals(bureau.fetchScore("ABCDE1234F"), bureau.fetchScore("ABCDE1234F"));
    }

    @Test
    void scoresStayInsideTheSupportedRange() {
        String[] pans = {"ABCDE1234F", "PQRSX9876Z", "AAAAA0000A", "ZZZZZ9999Z", "KLMNO4321B"};
        for (String pan : pans) {
            int score = bureau.fetchScore(pan);
            assertTrue(score >= LoanType.MIN_SCORE && score <= LoanType.MAX_SCORE, pan + " -> " + score);
        }
    }

    @Test
    void bandsDescribeTheScore() {
        assertEquals("Excellent", CreditBureauService.band(850));
        assertEquals("Very Good", CreditBureauService.band(760));
        assertEquals("Good", CreditBureauService.band(700));
        assertEquals("Fair", CreditBureauService.band(660));
        assertEquals("Needs Improvement", CreditBureauService.band(610));
    }
}
