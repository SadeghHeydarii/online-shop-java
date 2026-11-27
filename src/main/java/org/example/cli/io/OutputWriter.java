package org.example.cli.io;

import java.util.List;

/**
 * Narrow output port for rendering formatted messages, headers, and tables (ISP).
 */
public interface OutputWriter {

    void println(String message);

    void println();

    void print(String message);

    void printHeader(String title);

    void printDivider();

    void printSuccess(String message);

    void printError(String message);

    void printWarning(String message);

    void printInfo(String message);

    void printTable(List<String> headers, List<List<String>> rows);
}
