package com.loanmanager.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal fixed-width table renderer for console listings.
 * <pre>
 * new Table("ID", "Name").row(1, "Asha").row(2, "Ravi").print();
 * </pre>
 */
public final class Table {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public Table(String... headers) {
        this.headers = headers;
    }

    public Table row(Object... cells) {
        String[] row = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            row[i] = i < cells.length && cells[i] != null ? String.valueOf(cells[i]) : "-";
        }
        rows.add(row);
        return this;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void print() {
        if (rows.isEmpty()) {
            System.out.println("  (no records)");
            return;
        }
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
            for (String[] row : rows) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        System.out.println();
        System.out.println(line(headers, widths));
        System.out.println(rule(widths));
        for (String[] row : rows) {
            System.out.println(line(row, widths));
        }
        System.out.println();
        System.out.println("  " + rows.size() + " record" + (rows.size() == 1 ? "" : "s"));
    }

    private static String line(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < cells.length; i++) {
            sb.append(pad(cells[i], widths[i]));
            if (i < cells.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString();
    }

    private static String rule(int[] widths) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < widths.length; i++) {
            sb.append("-".repeat(widths[i]));
            if (i < widths.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString();
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(width - text.length());
    }
}
