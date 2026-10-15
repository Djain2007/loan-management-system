package com.loanmanager.model;

import java.time.LocalDateTime;

/** A registered account (customer or admin). Immutable snapshot of a row in {@code users}. */
public record User(
        int id,
        String firstName,
        String lastName,
        String mobile,
        String email,
        String location,
        String panNumber,
        Integer cibilScore,
        String username,
        String passwordHash,
        Role role,
        AccountStatus status,
        int failedAttempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}
