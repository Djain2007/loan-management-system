package com.loanmanager.service;

/** Simulates e-mail delivery by printing the message to the console. */
public class ConsoleEmailService implements EmailService {

    private static final String RULE = "-".repeat(60);

    @Override
    public void send(String to, String subject, String body) {
        System.out.println();
        System.out.println("  [EMAIL] " + RULE);
        System.out.println("  To:      " + to);
        System.out.println("  Subject: " + subject);
        System.out.println();
        body.lines().forEach(line -> System.out.println("  " + line));
        System.out.println("  " + RULE);
    }
}
