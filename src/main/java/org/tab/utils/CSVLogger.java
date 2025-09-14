package org.tab.utils;

import java.io.FileWriter;
import java.io.IOException;

public class CSVLogger {

    private static final String CSV_FILE = "src/test/resources/logs/skipped_stores.csv";

    /**
     * Append skipped store info to CSV
     * @param storeName store folder skipped
     * @param exception exception message
     */
    public static synchronized void logSkipped(String storeName, Exception exception) {
        try (FileWriter writer = new FileWriter(CSV_FILE, true)) {
            String safeMessage = exception != null
                    ? exception.getMessage()
                    .replace(",", ";")              // avoid breaking columns
                    .replaceAll("[\\r\\n]+", " ")   // replace newlines with space
                    : "";
            writer.write(storeName + "," + safeMessage + "\n");
        } catch (IOException e) {
            System.err.println("⚠️ Failed to log skipped store: " + storeName);
            e.printStackTrace();
        }
    }
}
