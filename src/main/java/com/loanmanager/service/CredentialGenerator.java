package com.loanmanager.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the auto-issued login credentials sent to a newly registered customer.
 * Username: first 3 letters of first name + first 3 of last name + last 4 digits of mobile.
 * Password: random, and always satisfies the password policy.
 */
public final class CredentialGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%!&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CredentialGenerator() {
    }

    public static String username(String firstName, String lastName, String mobile) {
        String first = firstName.substring(0, Math.min(3, firstName.length()));
        String last = lastName.substring(0, Math.min(3, lastName.length()));
        String digits = mobile.substring(Math.max(0, mobile.length() - 4));
        return (first + last + digits).toLowerCase();
    }

    public static String password() {
        List<Character> chars = new ArrayList<>(PASSWORD_LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIAL));
        while (chars.size() < PASSWORD_LENGTH) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private static char pick(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }
}
