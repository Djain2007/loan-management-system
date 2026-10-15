package com.loanmanager.model;

/** Lifecycle of a loan application: PENDING until an admin approves or rejects it. */
public enum LoanStatus {
    PENDING,
    APPROVED,
    REJECTED
}
