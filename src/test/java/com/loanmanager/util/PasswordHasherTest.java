package com.loanmanager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void verifiesTheOriginalPasswordOnly() {
        String stored = PasswordHasher.hash("Admin@123");

        assertTrue(PasswordHasher.verify("Admin@123", stored));
        assertFalse(PasswordHasher.verify("admin@123", stored));
        assertFalse(PasswordHasher.verify("", stored));
        assertFalse(PasswordHasher.verify(null, stored));
    }

    @Test
    void neverStoresThePlainPasswordAndSaltsEveryHash() {
        String first = PasswordHasher.hash("Secret@1");
        String second = PasswordHasher.hash("Secret@1");

        assertFalse(first.contains("Secret@1"));
        assertNotEquals(first, second, "same password must hash differently thanks to the random salt");
        assertTrue(first.startsWith("pbkdf2$"));
    }

    @Test
    void rejectsMalformedStoredValues() {
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.verify("x", "plain-text-password"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2$notanumber$abc$def"));
    }
}
