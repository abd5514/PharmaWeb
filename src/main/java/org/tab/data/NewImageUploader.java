package org.tab.data;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tab.utils.PDFConverter;
import org.tab.utils.PropReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public class NewImageUploader {
    private final Path baseDir;

    public NewImageUploader() {
        this.baseDir = Paths.get(
                PropReader.get("ImageFilesPath", "src/test/resources/images/")
        ).toAbsolutePath().normalize();

        if (!Files.isDirectory(this.baseDir)) {
            throw new IllegalArgumentException("Base image directory does not exist: " + this.baseDir);
        }
    }

    // ---------- Folder hierarchy ----------
    public List<String> getCityFolderNames() {
        List<String> cityNames = new ArrayList<>();
        File[] cities = baseDir.toFile().listFiles(File::isDirectory);
        if (cities != null) {
            for (File c : cities) {
                cityNames.add(c.getName());
            }
        }
        return cityNames;
    }

    public List<String> getStoreFolderNames(String cityName) {
        List<String> storeNames = new ArrayList<>();
        File cityDir = baseDir.resolve(cityName).toFile();
        if (cityDir.exists() && cityDir.isDirectory()) {
            File[] stores = cityDir.listFiles(File::isDirectory);
            if (stores != null) {
                for (File s : stores) {
                    storeNames.add(s.getName());
                }
            }
        }
        return storeNames;
    }

    public List<String> getImagePathsInFolder(String city, String store) throws IOException {
        List<String> imagePaths = new ArrayList<>();
        File storeDir = baseDir.resolve(city).resolve(store).toFile();
        List<String> convertedPaths;
        if (storeDir.exists() && storeDir.isDirectory()) {
            File[] files = storeDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".png")
                            || name.toLowerCase().endsWith(".jpg")
                            || name.toLowerCase().endsWith(".jpeg")
                            || name.toLowerCase().endsWith(".pdf"));
            if (files != null) {
                for (File f : files) {
                    if (!f.getName().toLowerCase().endsWith(".pdf")) {
                        imagePaths.add(f.getAbsolutePath());
                    }else {
                        // Convert PDF to PNG and add the PNG paths
                        convertedPaths = PDFConverter.convertPdfToPng(f);
                        imagePaths.addAll(convertedPaths);
                    }
                }
            }
        } else {
            System.out.println("❌ Folder not found: " + storeDir.getPath());
        }
        return imagePaths;
    }

    // ---------- Image locating ----------
    public Path findImage(String imageName) {
        Objects.requireNonNull(imageName, "imageName is null");

        Path imagePath = Path.of(imageName);
        if (imagePath.isAbsolute()) {
            if (Files.exists(imagePath)) return imagePath;
            throw new RuntimeException("Image not found: " + imagePath.toAbsolutePath());
        }

        String lower = imageName.toLowerCase(Locale.ROOT);
        String[] exts = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".pdf"};
        boolean hasExt = lower.contains(".");

        try (Stream<Path> stream = Files.walk(baseDir)) {
            Stream<Path> candidates = stream.filter(Files::isRegularFile);
            if (hasExt) {
                return candidates.filter(p -> p.getFileName().toString().equalsIgnoreCase(imageName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Image not found: " + imageName + " under " + baseDir));
            } else {
                return candidates.filter(p -> {
                            String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            for (String e : exts) if (fn.equals(lower + e)) return true;
                            return false;
                        })
                        .max(Comparator.comparingLong(NewImageUploader::lastModifiedMillis))
                        .orElseThrow(() -> new RuntimeException("Image not found (any extension): " + imageName + " under " + baseDir));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search for image '" + imageName + "' under " + baseDir, e);
        }
    }

    private static long lastModifiedMillis(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    // ---------- Upload helpers ----------
    public void uploadAllAtOnce(WebDriver driver, WebElement input, List<String> absPaths) {
        if (absPaths == null || absPaths.isEmpty()) return;
        String joined = String.join("\n", absPaths);
        input.sendKeys(joined);
    }
}