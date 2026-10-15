package com.loanmanager.service;

import com.loanmanager.model.LoanType;

/**
 * Simulated credit bureau. A real deployment would call the CIBIL / credit-bureau API here;
 * this implementation derives a stable score from the PAN so the same person always gets
 * the same score, which keeps demos and tests reproducible.
 */
public class CreditBureauService {

    /** Returns a CIBIL score in the {@link LoanType#MIN_SCORE}..{@link LoanType#MAX_SCORE} range for the PAN. */
    public int fetchScore(String pan) {
        int h = pan.hashCode();
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        int range = LoanType.MAX_SCORE - LoanType.MIN_SCORE + 1;
        return LoanType.MIN_SCORE + Math.floorMod(h, range);
    }

    /** Plain-language band shown next to the score. */
    public static String band(int score) {
        if (score >= 800) {
            return "Excellent";
        }
        if (score >= 750) {
            return "Very Good";
        }
        if (score >= 700) {
            return "Good";
        }
        if (score >= 650) {
            return "Fair";
        }
        return "Needs Improvement";
    }
}
