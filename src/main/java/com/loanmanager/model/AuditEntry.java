package com.loanmanager.model;

import java.time.LocalDateTime;

/** One line in the audit trail: who did what, to which record, and when. */
public record AuditEntry(
        int id,
        String actorName,
        String action,
        String target,
        String details,
        LocalDateTime createdAt) {
}
