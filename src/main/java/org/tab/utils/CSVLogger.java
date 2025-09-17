package org.tab.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.tab.data.TestDataReader.getXMLData;

public class CSVLogger {

    private static final String CSV_FILE = "src/test/resources/logs/skipped_stores_"+getXMLData("currentcity")+".csv";

    /**
     * Append skipped store info to CSV
     * @param storeName store folder skipped
     * @param exception exception message
     */
    public static synchronized void logSkipped(String storeName, Exception exception, int imageCount) {
        File file = new File(CSV_FILE);

        // Ensure parent directory exists
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean newFile = !file.exists();

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file, true),
                StandardCharsets.UTF_8)) {

            // Write BOM only if file is new
            if (newFile) {
                writer.write('\ufeff'); // UTF-8 BOM
                writer.write("Store,ErrorMessage" + System.lineSeparator());
            }

            String safeMessage = exception != null
                    ? exception.getMessage()
                    .replace(",", ";")
                    .replaceAll("[\\r\\n]+", " ")
                    : "have more than 60 MB of images, count is ( " + imageCount + " )";
            writer.write(storeName + "," + safeMessage + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("⚠️ Failed to log skipped store: " + storeName);
            e.printStackTrace();
        }
    }
}
