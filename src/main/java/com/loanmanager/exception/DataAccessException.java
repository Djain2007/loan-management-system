package com.loanmanager.exception;

import java.sql.SQLException;

/** Wraps a checked {@link SQLException} so the rest of the app is not littered with throws clauses. */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }
}
