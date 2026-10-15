package com.loanmanager.util;

import com.loanmanager.exception.AppException;

import java.util.regex.Pattern;

/**
 * Input rules shared by registration, profile editing and password changes.
 * Each {@code require*} method returns the cleaned value or throws an {@link AppException}
 * with a message that tells the user exactly what to fix.
 */
public final class Validator {

    private static final Pattern NAME = Pattern.compile("[A-Za-z]{2,50}");
    private static final Pattern MOBILE = Pattern.compile("\\d{10}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+");
    private static final Pattern PAN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String PASSWORD_RULES =
            "at least 8 characters with an uppercase letter, a lowercase letter, a digit and a special character";

    private Validator() {
    }

    public static String requireName(String value, String field) {
        String v = clean(value, field);
        if (!NAME.matcher(v).matches()) {
            throw new AppException(field + " must contain only letters (2-50 characters).");
        }
        return v;
    }

    public static String requireMobile(String value) {
        String v = clean(value, "Mobile number");
        if (!MOBILE.matcher(v).matches()) {
            throw new AppException("Mobile number must be exactly 10 digits.");
        }
        return v;
    }

    public static String requireEmail(String value) {
        String v = clean(value, "Email").toLowerCase();
        if (!EMAIL.matcher(v).matches()) {
            throw new AppException("Invalid email format.");
        }
        return v;
    }

    public static String requirePan(String value) {
        String v = clean(value, "PAN").toUpperCase();
        if (!PAN.matcher(v).matches()) {
            throw new AppException("Invalid PAN format. Expected 5 letters, 4 digits, 1 letter (e.g. ABCDE1234F).");
        }
        return v;
    }

    public static String requireStrongPassword(String value) {
        String v = value == null ? "" : value;
        if (!isStrongPassword(v)) {
            throw new AppException("Password must be " + PASSWORD_RULES + ".");
        }
        return v;
    }

    public static boolean isStrongPassword(String p) {
        return p != null
                && p.length() >= MIN_PASSWORD_LENGTH
                && p.chars().anyMatch(Character::isUpperCase)
                && p.chars().anyMatch(Character::isLowerCase)
                && p.chars().anyMatch(Character::isDigit)
                && p.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    }

    /** Optional free text: trimmed, or {@code null} when blank so the database stores NULL rather than "". */
    public static String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    public static String requireText(String value, String field, int maxLength) {
        String v = clean(value, field);
        if (v.length() > maxLength) {
            throw new AppException(field + " must be at most " + maxLength + " characters.");
        }
        return v;
    }

    private static String clean(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new AppException(field + " is required.");
        }
        return value.trim();
    }
}
