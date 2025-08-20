package org.tab.utils.ExtentReport;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ExtentManager {
    private static ExtentReports extent;
    private static Path reportsDir;   // <project>/Reports
    private static Path reportPath;   // <project>/Reports/ExtentReport_<ts>.html

    private ExtentManager() {}

    public static synchronized ExtentReports getExtent() {
        if (extent == null) {
            try {
                String projectRoot = System.getProperty("user.dir");
                String reportsBase = System.getProperty("reports.dir", "Reports");

                reportsDir = Path.of(projectRoot, reportsBase);
                Files.createDirectories(reportsDir); // ensure Reports/ exists

                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                // If you prefer a fixed name, replace with: reportsDir.resolve("ExtentReport.html")
                reportPath = reportsDir.resolve("ExtentReport_" + ts + ".html");

                ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toString());
                spark.config().setTheme(Theme.STANDARD);
                spark.config().setDocumentTitle("Automation Report");
                spark.config().setReportName("Execution Results");

                extent = new ExtentReports();
                extent.attachReporter(spark);
                extent.setSystemInfo("OS", System.getProperty("os.name"));
                extent.setSystemInfo("Env", System.getProperty("env", "local"));
                extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize ExtentReports", e);
            }
        }
        return extent;
    }

    public static Path getReportsDir() {
        if (reportsDir == null) getExtent();
        return reportsDir;
    }

    public static Path getReportPath() {
        if (reportPath == null) getExtent();
        return reportPath;
    }
}
