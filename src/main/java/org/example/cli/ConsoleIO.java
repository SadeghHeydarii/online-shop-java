package org.example.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

/**
 * Robust console input/output utility class for the CLI presentation layer.
 *
 * <p>Encapsulates terminal interactions, standard input parsing, input validation guards,
 * formatted console banners, and tabular data presentation. Follows the Fail-Fast and
 * Retry design principle to prevent invalid inputs from propagating into service layers.
 *
 * <p>Supports dependency injection of {@link Scanner} and {@link PrintStream} to allow
 * 100% deterministic, automated unit and integration testing via simulated streams.
 */
public class ConsoleIO {

    private final Scanner scanner;
    private final PrintStream out;

    /**
     * Default constructor connecting to standard input and standard output streams.
     */
    public ConsoleIO() {
        this(System.in, System.out);
    }

    /**
     * Constructs a {@code ConsoleIO} instance backed by custom input and output streams.
     *
     * @param in  the source input stream for user commands
     * @param out the destination print stream for console messages
     */
    public ConsoleIO(InputStream in, PrintStream out) {
        this(new Scanner(Objects.requireNonNull(in, "in stream cannot be null")), out);
    }

    /**
     * Constructs a {@code ConsoleIO} instance with an explicit {@link Scanner} and {@link PrintStream}.
     *
     * @param scanner the scanner reading user input tokens
     * @param out     the print stream receiving formatted console output
     */
    public ConsoleIO(Scanner scanner, PrintStream out) {
        this.scanner = Objects.requireNonNull(scanner, "scanner cannot be null");
        this.out = Objects.requireNonNull(out, "out stream cannot be null");
    }

    // =========================================================================
    // OUTPUT METHODS
    // =========================================================================

    /**
     * Prints a raw message followed by a platform-specific line separator.
     *
     * @param message the message to print
     */
    public void println(String message) {
        out.println(message != null ? message : "");
    }

    /**
     * Prints an empty line.
     */
    public void println() {
        out.println();
    }

    /**
     * Prints a raw message without a trailing line separator.
     *
     * @param message the message to print
     */
    public void print(String message) {
        if (message != null) {
            out.print(message);
        }
    }

    /**
     * Prints a standardized section header banner.
     *
     * @param title the title of the header section
     */
    public void printHeader(String title) {
        String effectiveTitle = title != null ? title.trim() : "";
        int width = Math.max(50, effectiveTitle.length() + 8);
        String border = "=".repeat(width);

        out.println();
        out.println(border);
        int padding = Math.max(0, (width - effectiveTitle.length()) / 2);
        out.println(" ".repeat(padding) + effectiveTitle);
        out.println(border);
    }

    /**
     * Prints a visual divider line.
     */
    public void printDivider() {
        out.println("-".repeat(50));
    }

    /**
     * Prints a standardized success notification message.
     *
     * @param message the success message to display
     */
    public void printSuccess(String message) {
        out.println("[SUCCESS] " + (message != null ? message : ""));
    }

    /**
     * Prints a standardized error notification message.
     *
     * @param message the error explanation to display
     */
    public void printError(String message) {
        out.println("[ERROR] " + (message != null ? message : ""));
    }

    /**
     * Prints a standardized warning notification message.
     *
     * @param message the warning explanation to display
     */
    public void printWarning(String message) {
        out.println("[WARNING] " + (message != null ? message : ""));
    }

    /**
     * Prints a standardized informational notification message.
     *
     * @param message the informative message to display
     */
    public void printInfo(String message) {
        out.println("[INFO] " + (message != null ? message : ""));
    }

    /**
     * Formats and prints tabular data with borders and automatic column sizing.
     *
     * @param headers the column header labels
     * @param rows    the data rows containing cell strings
     */
    public void printTable(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty()) {
            return;
        }

        int colCount = headers.size();
        int[] colWidths = new int[colCount];

        for (int i = 0; i < colCount; i++) {
            colWidths[i] = headers.get(i) != null ? headers.get(i).length() : 0;
        }

        if (rows != null) {
            for (List<String> row : rows) {
                if (row == null) {
                    continue;
                }
                for (int i = 0; i < Math.min(colCount, row.size()); i++) {
                    String cell = row.get(i);
                    int len = cell != null ? cell.length() : 0;
                    if (len > colWidths[i]) {
                        colWidths[i] = len;
                    }
                }
            }
        }

        // Build horizontal border
        StringBuilder borderSb = new StringBuilder("+");
        for (int w : colWidths) {
            borderSb.append("-".repeat(w + 2)).append("+");
        }
        String border = borderSb.toString();

        out.println(border);

        // Print header row
        StringBuilder headerSb = new StringBuilder("|");
        for (int i = 0; i < colCount; i++) {
            String h = headers.get(i) != null ? headers.get(i) : "";
            headerSb.append(" ").append(padRight(h, colWidths[i])).append(" |");
        }
        out.println(headerSb);
        out.println(border);

        // Print data rows
        if (rows != null && !rows.isEmpty()) {
            for (List<String> row : rows) {
                if (row == null) {
                    continue;
                }
                StringBuilder rowSb = new StringBuilder("|");
                for (int i = 0; i < colCount; i++) {
                    String cell = (i < row.size() && row.get(i) != null) ? row.get(i) : "";
                    rowSb.append(" ").append(padRight(cell, colWidths[i])).append(" |");
                }
                out.println(rowSb);
            }
            out.println(border);
        } else {
            int totalInnerWidth = border.length() - 2;
            String emptyMsg = "No records to display";
            int pad = Math.max(0, (totalInnerWidth - emptyMsg.length()) / 2);
            out.println("|" + " ".repeat(pad) + emptyMsg + " ".repeat(Math.max(0, totalInnerWidth - pad - emptyMsg.length())) + "|");
            out.println(border);
        }
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    // =========================================================================
    // INPUT METHODS (With Fail-Fast & Retry)
    // =========================================================================

    /**
     * Prompts the user and reads a non-blank string line. Retries until non-blank input is supplied.
     *
     * @param prompt the prompt message presented to the user
     * @return the trimmed non-empty string entered by the user
     * @throws NoSuchElementException if the input stream terminates prematurely
     */
    public String readString(String prompt) {
        while (true) {
            print(prompt + ": ");
            if (!scanner.hasNextLine()) {
                throw new NoSuchElementException("End of input stream reached while reading string");
            }
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            printError("Input cannot be empty. Please try again.");
        }
    }

    /**
     * Prompts the user and reads a string line, allowing blank input.
     *
     * @param prompt the prompt message presented to the user
     * @return the trimmed string entered by the user, or empty string if left blank
     * @throws NoSuchElementException if the input stream terminates prematurely
     */
    public String readLine(String prompt) {
        print(prompt + ": ");
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("End of input stream reached while reading line");
        }
        return scanner.nextLine().trim();
    }

    /**
     * Prompts the user and reads an integer within the specified closed range {@code [min, max]}.
     * Retries automatically if the input is not a valid integer or outside the bounds.
     *
     * @param prompt the prompt message
     * @param min    minimum allowable integer value
     * @param max    maximum allowable integer value
     * @return the validated integer in range
     * @throws NoSuchElementException if the input stream terminates prematurely
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            print(prompt + " [" + min + " - " + max + "]: ");
            if (!scanner.hasNextLine()) {
                throw new NoSuchElementException("End of input stream reached while reading integer");
            }
            String raw = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) {
                    return value;
                }
                printError("Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Invalid integer format. Please enter a valid number.");
            }
        }
    }

    /**
     * Prompts the user and reads a positive or zero {@link BigDecimal}.
     * Retries automatically if the input format is invalid or negative.
     *
     * @param prompt the prompt message
     * @return the validated non-negative {@link BigDecimal}
     * @throws NoSuchElementException if the input stream terminates prematurely
     */
    public BigDecimal readBigDecimal(String prompt) {
        while (true) {
            print(prompt + ": ");
            if (!scanner.hasNextLine()) {
                throw new NoSuchElementException("End of input stream reached while reading decimal");
            }
            String raw = scanner.nextLine().trim();
            try {
                BigDecimal value = new BigDecimal(raw);
                if (value.compareTo(BigDecimal.ZERO) >= 0) {
                    return value;
                }
                printError("Value cannot be negative. Please try again.");
            } catch (NumberFormatException e) {
                printError("Invalid decimal format. Please enter a valid numerical value.");
            }
        }
    }

    /**
     * Prompts the user and securely reads a password.
     * Uses {@link java.io.Console#readPassword()} when running in a real interactive terminal,
     * or falls back to line input in automated test / IDE environments.
     *
     * @param prompt the prompt message
     * @return the password string entered by the user
     * @throws NoSuchElementException if the input stream terminates prematurely
     */
    public String readPassword(String prompt) {
        print(prompt + ": ");
        if (System.console() != null) {
            char[] chars = System.console().readPassword();
            if (chars == null) {
                return "";
            }
            return new String(chars).trim();
        }
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("End of input stream reached while reading password");
        }
        return scanner.nextLine().trim();
    }

    /**
     * Prompts the user with a yes/no question and returns {@code true} for 'y'/'yes', {@code false} for 'n'/'no'.
     *
     * @param prompt the prompt message
     * @return {@code true} if confirmed, {@code false} otherwise
     * @throws NoSuchElementException if the input stream terminates prematurely
     */
    public boolean readBoolean(String prompt) {
        while (true) {
            print(prompt + " (y/n): ");
            if (!scanner.hasNextLine()) {
                throw new NoSuchElementException("End of input stream reached while reading confirmation");
            }
            String raw = scanner.nextLine().trim().toLowerCase();
            if (raw.equals("y") || raw.equals("yes")) {
                return true;
            }
            if (raw.equals("n") || raw.equals("no")) {
                return false;
            }
            printError("Please enter 'y' for yes or 'n' for no.");
        }
    }
}
