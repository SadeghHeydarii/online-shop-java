package org.example.cli.io;

import java.math.BigDecimal;

/**
 * Narrow input port for reading validated primitives and lines from a user interface (ISP).
 */
public interface InputReader {

    /**
     * Prompts for and reads a non-blank text token or line.
     *
     * @param prompt user-facing prompt
     * @return non-blank trimmed input
     */
    String readString(String prompt);

    /**
     * Prompts for and reads a line of input, allowing blank values.
     *
     * @param prompt user-facing prompt
     * @return trimmed line (possibly empty)
     */
    String readLine(String prompt);

    /**
     * Prompts for an integer within inclusive bounds {@code [min, max]}.
     *
     * @param prompt user-facing prompt
     * @param min    inclusive minimum value
     * @param max    inclusive maximum value
     * @return validated integer within bounds
     */
    int readInt(String prompt, int min, int max);

    /**
     * Prompts for a valid decimal monetary or numeric value.
     *
     * @param prompt user-facing prompt
     * @return parsed non-null {@link BigDecimal}
     */
    BigDecimal readBigDecimal(String prompt);

    /**
     * Prompts for and reads a non-blank password string.
     *
     * @param prompt user-facing prompt
     * @return password input
     */
    String readPassword(String prompt);

    /**
     * Prompts for a yes/no confirmation.
     *
     * @param prompt user-facing prompt
     * @return {@code true} for yes, {@code false} for no
     */
    boolean readBoolean(String prompt);
}
