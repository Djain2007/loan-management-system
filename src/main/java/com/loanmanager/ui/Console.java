package com.loanmanager.ui;

import com.loanmanager.exception.AppException;
import com.loanmanager.exception.DataAccessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

/**
 * All keyboard input and the handful of output styles the menus use.
 * Prompts re-ask until the input is valid, so callers never see malformed values.
 */
public final class Console {

    private static final int WIDTH = 64;
    private final Scanner in = new Scanner(System.in);

    // ---- output ------------------------------------------------------------------------------

    public void header(String title) {
        String bar = "=".repeat(WIDTH);
        System.out.println();
        System.out.println(bar);
        System.out.println(center(title));
        System.out.println(bar);
    }

    public void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " " + "-".repeat(Math.max(0, WIDTH - title.length() - 5)));
    }

    public void menu(String... options) {
        System.out.println();
        for (String option : options) {
            System.out.println("  " + option);
        }
        System.out.println();
    }

    public void info(String message) {
        System.out.println("  " + message);
    }

    public void field(String label, Object value) {
        System.out.printf("  %-18s: %s%n", label, value == null ? "-" : value);
    }

    public void success(String message) {
        System.out.println();
        System.out.println("  [OK] " + message);
    }

    public void error(String message) {
        System.out.println();
        System.out.println("  [!] " + message);
    }

    public void blank() {
        System.out.println();
    }

    /**
     * Runs a menu action and turns expected failures into a friendly message,
     * so one bad input never kills the whole session.
     */
    public void attempt(Runnable action) {
        try {
            action.run();
        } catch (AppException e) {
            error(e.getMessage());
        } catch (DataAccessException e) {
            error(e.getMessage());
            info("Check that MySQL is running and the credentials in config.properties are correct.");
        }
    }

    // ---- input -------------------------------------------------------------------------------

    public String readRequired(String prompt) {
        while (true) {
            String value = readOptional(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            error("This field is required.");
        }
    }

    public String readOptional(String prompt) {
        System.out.print("  " + prompt + ": ");
        System.out.flush();
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    /** Reads a password without echo when a real terminal is attached; falls back to plain input in IDEs. */
    public String readPassword(String prompt) {
        java.io.Console sys = System.console();
        if (sys != null) {
            while (true) {
                char[] chars = sys.readPassword("  " + prompt + ": ");
                if (chars != null && chars.length > 0) {
                    return new String(chars).trim();
                }
                error("This field is required.");
            }
        }
        return readRequired(prompt);
    }

    public int readInt(String prompt) {
        while (true) {
            String raw = readRequired(prompt);
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                error("Please enter a whole number.");
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt + " (" + min + "-" + max + ")");
            if (value >= min && value <= max) {
                return value;
            }
            error("Please enter a number between " + min + " and " + max + ".");
        }
    }

    public BigDecimal readAmount(String prompt) {
        while (true) {
            String raw = readRequired(prompt).replace(",", "");
            try {
                return new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                error("Please enter a valid amount, e.g. 250000 or 250000.50");
            }
        }
    }

    /** Menu choice: any integer from 0 to {@code maxOption}. */
    public int readChoice(int maxOption) {
        return readInt("Choose", 0, maxOption);
    }

    public boolean confirm(String prompt) {
        String answer = readRequired(prompt + " (yes/no)").toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }

    private static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(pad) + text;
    }
}
