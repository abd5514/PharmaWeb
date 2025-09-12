package org.tab.data;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tab.utils.PropReader;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public class ImageUploader {

    // Predefined base directory for test images (change as needed)
    private final Path baseDir;

    public ImageUploader() {
        this.baseDir = Paths.get(PropReader.get("ImageFilesPath", "src/test/resources/images/")).toAbsolutePath().normalize();
        if (!Files.isDirectory(this.baseDir)) {
            throw new IllegalArgumentException("Base image directory does not exist: " + this.baseDir);
        }
    }

    /**
     * Find an image file by name (case-insensitive, recursive).
     * If 'imageName' has no extension, common image extensions are tried.
     * Examples:
     *  - "logo.png"
     *  - "logo"  (will try .png .jpg .jpeg .webp .gif)
     */
    /*public Path findImage(String imageName) {
        Objects.requireNonNull(imageName, "imageName is null");
        String lower = imageName.toLowerCase(Locale.ROOT);

        String[] exts = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp"};
        boolean hasExt = lower.contains(".");

        try (Stream<Path> stream = Files.walk(baseDir)) {
            Stream<Path> candidates = stream.filter(Files::isRegularFile);

            if (hasExt) {
                // Exact filename match (case-insensitive)
                return candidates
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase(imageName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Image not found: " + imageName + " under " + baseDir));
            } else {
                // Try with any of the known extensions (prefer most recently modified if multiple)
                return candidates
                        .filter(p -> {
                            String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            for (String e : exts) {
                                if (fn.equals(lower + e)) return true;
                            }
                            return false;
                        })
                        .max(Comparator.comparingLong(ImageUploader::lastModifiedMillis))
                        .orElseThrow(() -> new RuntimeException("Image not found (any extension): " + imageName + " under " + baseDir));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search for image '" + imageName + "' under " + baseDir + ": " + e.getMessage(), e);
        }
    }*/
    public Path findImage(String imageName) {
        Objects.requireNonNull(imageName, "imageName is null");

        Path imagePath = Path.of(imageName);

        // 🔑 Fix: if it's already an absolute path, just return it directly
        if (imagePath.isAbsolute()) {
            if (Files.exists(imagePath)) {
                return imagePath;
            } else {
                throw new RuntimeException("Image not found: " + imagePath.toAbsolutePath());
            }
        }

        String lower = imageName.toLowerCase(Locale.ROOT);
        String[] exts = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".pdf"};
        boolean hasExt = lower.contains(".");

        try (Stream<Path> stream = Files.walk(baseDir)) {
            Stream<Path> candidates = stream.filter(Files::isRegularFile);

            if (hasExt) {
                // Exact filename match (case-insensitive)
                return candidates
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase(imageName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Image not found: " + imageName + " under " + baseDir));
            } else {
                // Try with any of the known extensions (prefer most recently modified if multiple)
                return candidates
                        .filter(p -> {
                            String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            for (String e : exts) {
                                if (fn.equals(lower + e)) return true;
                            }
                            return false;
                        })
                        .max(Comparator.comparingLong(ImageUploader::lastModifiedMillis))
                        .orElseThrow(() -> new RuntimeException("Image not found (any extension): " + imageName + " under " + baseDir));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search for image '" + imageName + "' under " + baseDir + ": " + e.getMessage(), e);
        }
    }

    /**
     * Upload the located image into an <input type="file">.
     * Works even if the input is hidden (we temporarily unhide via JS).
     */
    public void uploadImage(WebDriver driver, WebElement fileInputLocator, String imageName) {
        Path img = findImage(imageName);
        String absolutePath = img.toFile().getAbsolutePath();

        // If input is not displayed or not interactable, unhide/enable it via JS
        try {
            if (!fileInputLocator.isDisplayed() || !fileInputLocator.isEnabled()) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript(
                        "arguments[0].style.display='block'; arguments[0].style.visibility='visible'; arguments[0].removeAttribute('disabled');",
                        fileInputLocator
                );
            }
        } catch (Exception ignored) {}

        // Some UIs put the input off-screen. Ensure it's in view.
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", fileInputLocator);
        } catch (Exception ignored) {}

        // Selenium can only interact with native file pickers via sendKeys
        fileInputLocator.sendKeys(absolutePath);
    }

    /**
     * Variant that waits for the input to show a selected filename attribute (if app reflects it).
     * Useful when you need a post-condition.
     */
    public void uploadImageAndVerify(WebDriver driver, WebElement fileInputLocator, String imageName) {
        uploadImage(driver, fileInputLocator, imageName);
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> {
            String val = fileInputLocator.getAttribute("value"); // often contains the filename
            return val != null && !val.isBlank();
        });
    }

    private static long lastModifiedMillis(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<String> getImageFolderNames() {
        List<String> folderNames = new ArrayList<>();
        File baseDir = new File(PropReader.get("ImageFilesPath", "src/test/resources/images/"));

        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] folders = baseDir.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    folderNames.add(folder.getName());
                }
            }
        } else {
            System.out.println("❌ Directory not found: " + baseDir.getPath());
        }
        return folderNames;
    }

    public List<String> getImagePathsInFolder(String storeName) {
        List<String> imagePaths = new ArrayList<>();
        File storeDir = new File("src/test/resources/images/" + storeName);

        if (storeDir.exists() && storeDir.isDirectory()) {
            File[] files = storeDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".png") ||
                            name.toLowerCase().endsWith(".jpg") ||
                            name.toLowerCase().endsWith(".jpeg"));
            if (files != null) {
                for (File f : files) {
                    imagePaths.add(f.getAbsolutePath());
                }
            }
        } else {
            System.out.println("❌ Folder not found: " + storeDir.getPath());
        }
        return imagePaths;
    }

    // ImageUploader.java
    public void uploadAllAtOnce(WebDriver driver, WebElement input, List<String> absPaths) {
        if (absPaths == null || absPaths.isEmpty()) return;

        // Ensure input is visible & enabled; then send newline-separated absolute paths
        String joined = String.join("\n", absPaths);

        // If your page uses a custom button, make sure to click it to reveal the real <input type="file">.
        // Then call sendKeys on the *real input element*:
        input.sendKeys(joined);

        // OPTIONAL: wait until your UI shows "upload complete" (replace with your actual locator/condition)
        // new WebDriverWait(driver, Duration.ofSeconds(120))
        //      .until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".uploader-progress")));
    }

}
