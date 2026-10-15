package com.loanmanager.model;

/**
 * Account state. An account becomes INACTIVE either when an admin deactivates it
 * or automatically after too many failed login attempts; only an admin can reactivate it.
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE
}
