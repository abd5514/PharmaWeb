/*
package org.tab.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.tab.data.TestDataReader.getXMLData;

public class CSVLogger {

    private static final String CSV_FILE = "src/test/resources/logs/skipped_stores_"+getXMLData("currentcity")+".csv";

    */
/**
     * Append skipped store info to CSV
     * @param storeName store folder skipped
     * @param exception exception message
     *//*

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
*/
package org.tab.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CSVLogger {

    // Base folder for logs
    private static final String LOG_DIR = "src/test/resources/logs/";

    /**
     * Append skipped store info to a city-specific CSV
     * @param cityName city folder name
     * @param storeName store folder skipped
     * @param exception exception message
     * @param imageCount number of images if skipped due to size
     */
    public static synchronized void logSkipped(String cityName, String storeName, Exception exception, int imageCount) {
        // ❌ Filter out known non-critical skip case
        if (exception != null && exception.getMessage() != null &&
                exception.getMessage().startsWith("Expected condition failed: waiting for text ('Save changes') to be present")) {
            System.out.printf("⚠️ Not logging store %s in %s due to benign 'Save changes' wait issue%n", storeName, cityName);
            return; // don’t log this case
        }
        // Each city gets its own skipped_stores_<city>.csv
        File file = new File(LOG_DIR + "skipped_stores_" + cityName + ".csv");

        // Ensure parent directory exists
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean newFile = !file.exists();

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file, true),
                StandardCharsets.UTF_8)) {

            // Write BOM + header if file is new
            if (newFile) {
                writer.write('\ufeff'); // UTF-8 BOM
                writer.write("City,Store,ErrorMessage" + System.lineSeparator());
            }

            String safeMessage = (exception != null)
                    ? exception.getMessage()
                    .replace(",", ";")
                    .replaceAll("[\\r\\n]+", " ")
                    : /*"have more than 60 MB of images, count is ( " + imageCount + " )"*/"images not uploaded correctly or folder is empty, image count is  " + imageCount;

            writer.write(cityName + "," + storeName + "," + safeMessage + System.lineSeparator());

        } catch (IOException e) {
            System.err.println("⚠️ Failed to log skipped store: " + storeName + " in city: " + cityName);
            e.printStackTrace();
        }
    }
}
