package org.example.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsoleIO Tests")
class ConsoleIOTest {

    private ConsoleIO createConsoleIO(String simulatedInput, ByteArrayOutputStream outContent) {
        ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, out);
    }

    @Nested
    @DisplayName("Input Reading Tests")
    class InputReadingTests {

        @Test
        @DisplayName("readString returns trimmed line on valid input")
        void readStringSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("  hello world  \n", out);

            String result = io.readString("Enter text");
            assertEquals("hello world", result);
            assertTrue(out.toString().contains("Enter text: "));
        }

        @Test
        @DisplayName("readString retries on empty or blank lines until valid input is given")
        void readStringRetriesOnBlank() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("   \n\n  final text  \n", out);

            String result = io.readString("Prompt");
            assertEquals("final text", result);
            assertTrue(out.toString().contains("[ERROR] Input cannot be empty."));
        }

        @Test
        @DisplayName("readString throws NoSuchElementException on EOF")
        void readStringThrowsOnEof() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);

            assertThrows(NoSuchElementException.class, () -> io.readString("Prompt"));
        }

        @Test
        @DisplayName("readLine allows empty lines and returns trimmed result")
        void readLineAllowsEmpty() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("   \n", out);

            String result = io.readLine("Optional prompt");
            assertEquals("", result);
        }

        @Test
        @DisplayName("readInt returns valid integer in range")
        void readIntSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("5\n", out);

            int result = io.readInt("Enter number", 1, 10);
            assertEquals(5, result);
        }

        @Test
        @DisplayName("readInt retries on non-integer input and out-of-range integer")
        void readIntRetriesOnInvalid() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("abc\n15\n-2\n4\n", out);

            int result = io.readInt("Option", 1, 5);
            assertEquals(4, result);

            String output = out.toString();
            assertTrue(output.contains("Invalid integer format."));
            assertTrue(output.contains("Value must be between 1 and 5."));
        }

        @Test
        @DisplayName("readBigDecimal returns parsed non-negative decimal")
        void readBigDecimalSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("1250000.50\n", out);

            BigDecimal result = io.readBigDecimal("Price");
            assertEquals(new BigDecimal("1250000.50"), result);
        }

        @Test
        @DisplayName("readBigDecimal retries on negative number and non-numeric string")
        void readBigDecimalRetriesOnInvalid() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("not_a_num\n-500\n250.75\n", out);

            BigDecimal result = io.readBigDecimal("Amount");
            assertEquals(new BigDecimal("250.75"), result);

            String output = out.toString();
            assertTrue(output.contains("Invalid decimal format."));
            assertTrue(output.contains("Value cannot be negative."));
        }

        @Test
        @DisplayName("readPassword reads password line from fallback scanner")
        void readPasswordFallback() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("secretPass123\n", out);

            String pass = io.readPassword("Password");
            assertEquals("secretPass123", pass);
        }

        @Test
        @DisplayName("readBoolean accepts 'y', 'yes', 'n', 'no' case-insensitively and retries on others")
        void readBooleanVariants() {
            ByteArrayOutputStream out1 = new ByteArrayOutputStream();
            ConsoleIO io1 = createConsoleIO("Y\n", out1);
            assertTrue(io1.readBoolean("Confirm"));

            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            ConsoleIO io2 = createConsoleIO("yes\n", out2);
            assertTrue(io2.readBoolean("Confirm"));

            ByteArrayOutputStream out3 = new ByteArrayOutputStream();
            ConsoleIO io3 = createConsoleIO("N\n", out3);
            assertFalse(io3.readBoolean("Confirm"));

            ByteArrayOutputStream out4 = new ByteArrayOutputStream();
            ConsoleIO io4 = createConsoleIO("maybe\nno\n", out4);
            assertFalse(io4.readBoolean("Confirm"));
            assertTrue(out4.toString().contains("Please enter 'y' for yes or 'n' for no."));
        }
    }

    @Nested
    @DisplayName("Output Printing Tests")
    class OutputPrintingTests {

        @Test
        @DisplayName("printHeader displays centered title and borders")
        void printHeaderFormat() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);

            io.printHeader("Main Dashboard");
            String output = out.toString();
            assertTrue(output.contains("Main Dashboard"));
            assertTrue(output.contains("==="));
        }

        @Test
        @DisplayName("Status banners format with proper tag prefix")
        void statusBanners() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);

            io.printSuccess("Operation successful");
            io.printError("Something broke");
            io.printWarning("Low disk space");
            io.printInfo("System update ready");
            io.printDivider();

            String output = out.toString();
            assertTrue(output.contains("[SUCCESS] Operation successful"));
            assertTrue(output.contains("[ERROR] Something broke"));
            assertTrue(output.contains("[WARNING] Low disk space"));
            assertTrue(output.contains("[INFO] System update ready"));
            assertTrue(output.contains("----------------------------------------"));
        }

        @Test
        @DisplayName("printTable formats rectangular ASCII table with headers and data")
        void printTableFormatting() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);

            List<String> headers = List.of("ID", "Name", "Price");
            List<List<String>> rows = List.of(
                    List.of("prod-1", "Laptop", "1000000 IRR"),
                    List.of("prod-2", "Mouse", "50000 IRR")
            );

            io.printTable(headers, rows);
            String output = out.toString();

            assertTrue(output.contains("+"));
            assertTrue(output.contains("| ID"));
            assertTrue(output.contains("Laptop"));
            assertTrue(output.contains("50000 IRR"));
        }

        @Test
        @DisplayName("printTable with empty rows displays empty table placeholder")
        void printTableEmptyRows() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);

            List<String> headers = List.of("Col1", "Col2");
            io.printTable(headers, List.of());
            String output = out.toString();

            assertTrue(output.contains("No records to display"));
        }
    }
}
