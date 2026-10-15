package com.loanmanager.exception;

/**
 * A business-rule or validation failure whose message is safe to show to the user,
 * e.g. "Account is locked" or "Only pending loans can be approved".
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }
}
