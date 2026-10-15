package com.loanmanager.service;

/**
 * Outbound e-mail transport. The default implementation prints to the console;
 * swap in an SMTP implementation (e.g. Jakarta Mail) without touching the rest of the app.
 */
public interface EmailService {

    void send(String to, String subject, String body);
}
