/*
package org.tab.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class FolderCleaner {

    private static final String CSV_FILE = "src/test/resources/logs/skipped_stores.csv";
    private static final String TARGET_DIR = "src/test/resources/images";

    public static void main(String[] args) {
        cleanFolders();
    }

    public static void cleanFolders() {
        try {
            // Read first column (StoreName) from CSV into a Set
            Set<String> skippedStores = Files.lines(Paths.get(CSV_FILE), StandardCharsets.UTF_8)
                    .map(line -> line.split(",", 2)[0].trim())  // take first column
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toSet());

            File targetDir = new File(TARGET_DIR);
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                System.err.println("❌ Target directory not found: " + TARGET_DIR);
                return;
            }

            File[] folders = targetDir.listFiles(File::isDirectory);
            if (folders == null) {
                System.out.println("⚠️ No folders found inside: " + TARGET_DIR);
                return;
            }

            for (File folder : folders) {
                String folderName = folder.getName();

                if (skippedStores.contains(folderName)) {
                    System.out.println("✅ Skipping: " + folderName + " (found in CSV)");
                } else {
                    System.out.println("🗑️ Deleting: " + folderName + " (not in CSV)");
                    deleteDirectoryRecursively(folder.toPath());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Utility method to delete folder and contents
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.notExists(path)) return;

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        System.err.println("⚠️ Failed to delete " + p + ": " + e.getMessage());
                    }
                });
    }
}
*/

package org.tab.utils;

import org.tab.utils.ExtentReport.ExtentTestListener;
import org.testng.annotations.Listeners;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.tab.data.TestDataReader.getXMLData;

@Listeners(ExtentTestListener.class)
public class FolderCleaner {

    private static final String CSV_FILE = "src/test/resources/logs/skipped_stores_"+getXMLData("currentcity")+".csv";
    private static final String TARGET_DIR = "src/test/resources/images";
    private static final String DONE_DIR = "src/test/resources/skipped";
    private static final String ARCHIVE_DIR = "src/test/resources/done_upload"; // destination

    public static void main(String[] args) {
        cleanFolders();
    }

    public static void cleanFolders() {
        try {
            // Read first column (StoreName) from CSV into a Set
            Set<String> skippedStores = Files.lines(Paths.get(CSV_FILE), StandardCharsets.UTF_8)
                    .map(line -> line.split(",", 2)[0].trim())  // take first column
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toSet());

            File targetDir = new File(TARGET_DIR);
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                System.err.println("❌ Target directory not found: " + TARGET_DIR);
                return;
            }

            File[] folders = targetDir.listFiles(File::isDirectory);
            if (folders == null) {
                System.out.println("⚠️ No folders found inside: " + TARGET_DIR);
                return;
            }

            for (File folder : folders) {
                String folderName = folder.getName();

                if (skippedStores.contains(folderName)) {
                    moveDirectory(folder.toPath(), Paths.get(DONE_DIR, folderName));
                } else {
                    moveDirectory(folder.toPath(), Paths.get(ARCHIVE_DIR, folderName));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Utility method to move folder
    private static void moveDirectory(Path source, Path target) throws IOException {
        if (Files.notExists(source)) return;

        // Ensure destination parent exists
        Files.createDirectories(target.getParent());

        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to move " + source + " -> " + target + ": " + e.getMessage());
        }
    }
}
