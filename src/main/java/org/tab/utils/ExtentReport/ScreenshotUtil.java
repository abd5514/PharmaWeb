package org.tab.utils.ExtentReport;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtil {

    public static String takeBase64(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            return null;
        }
    }

    public static String saveToFile(WebDriver driver, String testName) {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path dir = Path.of("target", "screenshots");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path file = dir.resolve(testName.replaceAll("[^a-zA-Z0-9._-]", "_") + "_" + ts + ".png");
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(file, bytes);
            return file.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
