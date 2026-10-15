package com.loanmanager.service;

import com.loanmanager.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialGeneratorTest {

    @Test
    void usernameCombinesNameFragmentsAndMobileSuffix() {
        assertEquals("ashsha3210", CredentialGenerator.username("Asha", "Sharma", "9876543210"));
        assertEquals("rakha7777", CredentialGenerator.username("Ra", "Khan", "9999997777"));
    }

    @Test
    void generatedPasswordsAlwaysPassThePolicy() {
        for (int i = 0; i < 500; i++) {
            String password = CredentialGenerator.password();
            assertEquals(10, password.length());
            assertTrue(Validator.isStrongPassword(password), "weak password generated: " + password);
        }
    }
}
