package org.tab.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;

public class FolderMover {

    public static void main(String[] args) throws IOException {
        // === Configuration ===
        Path doneUploadRoot = Paths.get("src/test/resources/done_upload");
        Path skippedRoot = Paths.get("src/test/resources/skipped");
        Path imagesRoot = Paths.get("src/test/resources/images");

        // Move everything modified after this date
        LocalDate cutoffDate = LocalDate.of(2025, 9, 28);
        Instant cutoffInstant = cutoffDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        moveRecentFolders(doneUploadRoot, imagesRoot, cutoffInstant);
        moveRecentFolders(skippedRoot, imagesRoot, cutoffInstant);

        System.out.println("✅ Folder move complete. Older folders left untouched.");
    }

    private static void moveRecentFolders(Path sourceRoot, Path targetRoot, Instant cutoffInstant) throws IOException {
        if (!Files.exists(sourceRoot)) {
            System.out.println("⚠ Source folder not found: " + sourceRoot);
            return;
        }

        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(sourceRoot)) return FileVisitResult.CONTINUE;

                Instant lastModified = attrs.lastModifiedTime().toInstant();

                // Only move folders newer than cutoff date
                if (lastModified.isAfter(cutoffInstant)) {
                    Path relativePath = sourceRoot.relativize(dir);
                    Path targetDir = targetRoot.resolve(relativePath);

                    Files.createDirectories(targetDir.getParent());

                    try {
                        System.out.println("➡ Moving: " + dir + " → " + targetDir);
                        Files.move(dir, targetDir, StandardCopyOption.REPLACE_EXISTING);
                    } catch (DirectoryNotEmptyException e) {
                        // Merge files if target already exists
                        Files.walk(dir)
                                .filter(p -> !Files.isDirectory(p))
                                .forEach(p -> {
                                    try {
                                        Path rel = dir.relativize(p);
                                        Path destFile = targetDir.resolve(rel);
                                        Files.createDirectories(destFile.getParent());
                                        Files.copy(p, destFile, StandardCopyOption.REPLACE_EXISTING);
                                    } catch (IOException io) {
                                        System.err.println("❌ Failed to copy: " + p + " → " + io.getMessage());
                                    }
                                });
                        System.out.println("ℹ️ Merged contents from " + dir + " into existing " + targetDir);
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // Skip older folders entirely
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }
}
