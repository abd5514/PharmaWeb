package org.tab.utils.ExtentReport;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import org.openqa.selenium.*;
import org.tab.core.instance.DriverManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentTestListener implements ITestListener {

    private static final ThreadLocal<ExtentTest> TL = new ThreadLocal<>();
    private ExtentReports extent;

    @Override
    public void onStart(ITestContext context) {
        extent = ExtentManager.getExtent();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        ExtentTest test = extent.createTest(name);
        test.assignCategory(result.getTestClass().getName());
        TL.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = TL.get();
        test.pass("Test passed");
        if (Boolean.parseBoolean(System.getProperty("screenshotOnPass", "false"))) {
            attachScreenshotFile(test, result, "Passed");
        }
        TL.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = TL.get();
        test.fail(result.getThrowable());
        attachScreenshotFile(test, result, "Failure");
        TL.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = TL.get();
        test.skip("Test skipped");
        TL.remove();
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
            System.out.println("[Extent] Report: " + ExtentManager.getReportPath().toAbsolutePath());
        }
    }

    /** Saves PNG under Reports/Screenshots and attaches via a relative path so it renders in the HTML. */
    private void attachScreenshotFile(ExtentTest test, ITestResult result, String label) {
        try {
            WebDriver driver = DriverManager.getDriver();
            if (!(driver instanceof TakesScreenshot)) {
                test.info("No screenshot: driver does not support TakesScreenshot");
                return;
            }

            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            Path reportsDir = ExtentManager.getReportsDir();
            Path screenshotsDir = reportsDir.resolve("Screenshots");
            Files.createDirectories(screenshotsDir); // ensure Reports/Screenshots exists

            String ts = new SimpleDateFormat("HHmmss_SSS").format(new Date());
            String method = result.getMethod().getMethodName();
            String fileName = method + "_" + label + "_" + ts + ".png";
            Path file = screenshotsDir.resolve(fileName);
            Files.write(file, bytes);

            // Make the path relative to the report location (Reports/)
            Path reportDir = ExtentManager.getReportPath().getParent(); // Reports/
            String relative = reportDir.toAbsolutePath().relativize(file.toAbsolutePath())
                    .toString().replace('\\', '/'); // "Screenshots/xxx.png"

            test.info(label + " screenshot",
                    MediaEntityBuilder.createScreenCaptureFromPath(relative, fileName).build());
        } catch (Exception e) {
            TL.get().warning("Failed to capture screenshot: " + e.getMessage());
        }
    }
}
