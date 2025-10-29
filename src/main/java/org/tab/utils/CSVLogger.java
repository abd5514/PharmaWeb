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
            String msg;
            if(imageCount==0){
                msg="folder have no image";
            }else if(imageCount>60){msg="have more than 60 images, count is ( " + imageCount + " )";}
            else{
                msg="images not uploaded correctly";
            }
            String safeMessage;
            safeMessage = (exception != null)
                        ? exception.getMessage()
                        .replace(",", ";")
                        .replaceAll("[\\r\\n]+", " ")
                        : msg;
            writer.write(cityName + "," + storeName + "," + safeMessage + System.lineSeparator());

        } catch (IOException e) {
            System.err.println("⚠️ Failed to log skipped store: " + storeName + " in city: " + cityName);
            e.printStackTrace();
        }
    }

    public static synchronized void logSkipped(String cityName, String storeName, Exception exception, int imageCount, String extraInfo) {

        // Each city gets its own skipped_stores_<city>.csv
        File file = new File(LOG_DIR + "skipped_stores_" + cityName + "_"+extraInfo+ ".csv");

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
            String msg;
            if(imageCount==0){
                msg="folder have no image";
            }else if(imageCount>30){msg="have more than 30 images, count is ( " + imageCount + " )";}
            else{
                msg="images not uploaded correctly";
            }
            String safeMessage;
            if(exception.getMessage().startsWith(
                    "no such element: Unable to locate element: {\"method\":\"xpath\",\"selector\":\"//div[@class='filepond--image-preview-wrapper']\"}")){
                safeMessage="images not uploaded on server correctly";
            }else {
                safeMessage = (exception != null)
                        ? exception.getMessage()
                        .replace(",", ";")
                        .replaceAll("[\\r\\n]+", " ")
                        : msg;
            }
            writer.write(cityName + "," + storeName + "," + safeMessage + System.lineSeparator());

        } catch (IOException e) {
            System.err.println("⚠️ Failed to log skipped store: " + storeName + " in city: " + cityName);
            e.printStackTrace();
        }
    }

    public static synchronized void logUrls(String cityName,String Nighborhood, String storeName, String Url) {

        // Each city gets its own skipped_stores_<city>.csv
        File file = new File(LOG_DIR + "Zero_Stores.csv");

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
                writer.write("City,Neighborhood,Store,URL" + System.lineSeparator());
            }
            writer.write(cityName + ","+ Nighborhood + "," + storeName + "," + Url + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("⚠️ Failed to log skipped store: " + storeName + " in city: " + cityName);
            e.printStackTrace();
        }
    }
}
