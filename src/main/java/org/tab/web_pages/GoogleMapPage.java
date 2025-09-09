package org.tab.web_pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.tab.utils.common.SharedMethods.*;

public class GoogleMapPage {

    @FindBy (xpath="//div[normalize-space()='Menu'][1]")
    public WebElement menuBtn;
    @FindBy (xpath="//div[@class='cRLbXd']//div[contains(@class,'dryRY')]")
    public WebElement imageContainer;
    @FindBy (xpath="//a[normalize-space()='English']")
    public WebElement enBtn;
    @FindBy (xpath="//button[contains(@class,'Tc0rEd XMkGfe cPtXLb')]")
    public WebElement nextBtn;


    public GoogleMapPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void getAllImages(WebDriver driver, String imageName) {
        for (int i = 0; i < 15; i++) {
            try {
                moveMouseToElement(driver, imageContainer);
                if (nextBtn.isDisplayed()) {
                    nextBtn.click();
                    moveMouseToElement(driver, menuBtn);
                } else break;
            } catch (Exception ignored) {}
        }
        try {
            List<WebElement> images = driver.findElements(
                    By.xpath("//div[@class='cRLbXd']//div[contains(@class,'dryRY')]//img[@class='DaSXdd']")
            );
            // Prepare store folder: src/test/resources/images/<storeName>
            // Auto-clean (delete old files) before saving new ones
            String safeName = sanitizeForWindows(imageName);
            File dir = new File("src/test/resources/images/" + safeName);
            if (dir.exists()) {
                try {
                    deleteDirectoryContents(dir);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to clean folder: " + dir.getPath() + " -> " + e.getMessage());
                }
            } else {
                dir.mkdirs();
            }

            int index = 1; // for naming files
            for (WebElement img : images) {
                String src = img.getAttribute("src");

                if (src != null && src.contains("lh3.googleusercontent.com")) {
                    // Replace size params with w4000-h5500
                    String highResSrc = src.replaceAll("w\\d+-h\\d+", "w4000-h5500");
                    // Save image in src/test/resources/images/<storeName>
                    String fileName = sanitizeForWindows("image_" + safeName + "_" + index + ".png");
                    File target = new File(dir, fileName);
                    try {
                        downloadImage(highResSrc, target.getPath());
                    } catch (Exception e) {
                        saveFailedDownload(highResSrc, target.getPath(), e);
                    }
                    index++;
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("No images found in the specified container.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadImage(String imageUrl, String filePath) {
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(new File(filePath))) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to download image: " + imageUrl);
            e.printStackTrace();
        }
    }

    private void deleteDirectoryContents(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDirectoryContents(f);
            }
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    // NEW: helper to persist failures without changing existing flow
    /*private void saveFailedDownload(String imageUrl, String filePath, Exception e) {
        File logFile = new File("src/test/resources/failed_downloads.csv");
        try {
            // Ensure parent directory exists
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // true = append mode (creates file if not exists, appends if it does)
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = LocalDateTime.now().toString();
                String errorMsg = (e == null || e.getMessage() == null) ? "" :
                        e.getMessage().replaceAll("[\\r\\n]", " ");
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n", timestamp, imageUrl, filePath, errorMsg);
            }
        } catch (IOException io) {
            // Swallow so we don’t interrupt main flow
            System.out.println("⚠️ Failed to log download error: " + io.getMessage());
        }
    }*/

    private void saveFailedDownload(String imageUrl, String filePath, Exception e) {
        File logFile = new File("src/test/resources/failed_downloads.csv");
        try {
            // Ensure parent directory exists
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            boolean newFile = !logFile.exists() || logFile.length() == 0;

            // Open underlying stream in append mode
            try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
                // Write BOM only once for a new/empty file (helps Excel read UTF-8)
                if (newFile) {
                    fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                }
                // Write the CSV line using UTF-8
                try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     PrintWriter pw = new PrintWriter(osw)) {

                    String timestamp = LocalDateTime.now().toString();
                    String errorMsg = (e == null || e.getMessage() == null) ? "" :
                            e.getMessage().replaceAll("[\\r\\n]", " ");

                    // Always quote to be safe with commas
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            timestamp, imageUrl, filePath, errorMsg);
                }
            }
        } catch (IOException io) {
            // Don’t interrupt main flow
            System.out.println("⚠️ Failed to log download error: " + io.getMessage());
        }
    }

}
