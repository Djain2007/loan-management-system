package com.loanmanager.util;

import com.loanmanager.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void passwordPolicy() {
        assertTrue(Validator.isStrongPassword("Admin@123"));
        assertFalse(Validator.isStrongPassword("admin@123"), "needs an uppercase letter");
        assertFalse(Validator.isStrongPassword("ADMIN@123"), "needs a lowercase letter");
        assertFalse(Validator.isStrongPassword("Admin@abc"), "needs a digit");
        assertFalse(Validator.isStrongPassword("Admin1234"), "needs a special character");
        assertFalse(Validator.isStrongPassword("Ab@1"), "too short");
        assertFalse(Validator.isStrongPassword(null));
    }

    @Test
    void panIsNormalisedToUpperCase() {
        assertEquals("ABCDE1234F", Validator.requirePan(" abcde1234f "));
        assertThrows(AppException.class, () -> Validator.requirePan("ABCD1234F"));
        assertThrows(AppException.class, () -> Validator.requirePan("ABCDE12345"));
    }

    @Test
    void emailIsNormalisedToLowerCase() {
        assertEquals("asha@example.com", Validator.requireEmail("Asha@Example.COM"));
        assertThrows(AppException.class, () -> Validator.requireEmail("asha@example"));
        assertThrows(AppException.class, () -> Validator.requireEmail("not-an-email"));
        assertThrows(AppException.class, () -> Validator.requireEmail(""));
    }

    @Test
    void mobileMustBeTenDigits() {
        assertEquals("9876543210", Validator.requireMobile("9876543210"));
        assertThrows(AppException.class, () -> Validator.requireMobile("98765"));
        assertThrows(AppException.class, () -> Validator.requireMobile("98765432101"));
        assertThrows(AppException.class, () -> Validator.requireMobile("98765-4321"));
    }

    @Test
    void namesAreLettersOnly() {
        assertEquals("Asha", Validator.requireName("  Asha ", "First name"));
        assertThrows(AppException.class, () -> Validator.requireName("A", "First name"));
        assertThrows(AppException.class, () -> Validator.requireName("Asha2", "First name"));
        assertThrows(AppException.class, () -> Validator.requireName(null, "First name"));
    }
}
