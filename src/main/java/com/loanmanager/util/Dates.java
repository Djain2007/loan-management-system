package com.loanmanager.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One date style for the whole console: {@code 15 Oct 2026 23:40}. */
public final class Dates {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private Dates() {
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "-" : FORMAT.format(dateTime);
    }
}
