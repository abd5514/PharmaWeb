package org.tab.tests;

import org.openqa.selenium.By;
import org.tab.base.Base;
import org.tab.data.JSONReader;
import org.tab.data.RetryFailedDownloads;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.GoogleMapPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.tab.utils.common.SharedMethods.staticWait;

@Listeners(ExtentTestListener.class)
public class GoogleMapPageTest extends Base {

    @Test(description = "json reader case")
    public void jsonReader() throws IOException {
        GoogleMapPage googleMapPage = new GoogleMapPage(driver);
        JSONReader r = new JSONReader(); // points to array JSON
        String uri = "https://www.google.com";
        int count = r.size(); // number of items
        driver.get(uri);
        googleMapPage.enBtn.click();
        staticWait(1000);
        for (int i = 0; i < count; i++) {
            String url = r.getString(i, "googleMapsUri");
            String storeName = r.getString(i, "displayName.text");
            driver.get(url);
            staticWait(400);
            try {
                googleMapPage.menuBtn.click();
                staticWait(200);

                // If a new tab is opened, close it
                if (driver.getWindowHandles().size() > 1) {
                    for (String handle : driver.getWindowHandles()) {
                        driver.switchTo().window(handle);
                    }
                    driver.close(); // closes the new tab (last opened)
                    // Switch back to first tab
                    driver.switchTo().window(driver.getWindowHandles().iterator().next());
                }
            } catch (Exception e) {
                continue;
            }
            googleMapPage.getAllImages(driver,storeName,i);
            System.out.println("current loop  "+i);
        }
    }

    @Test(description = "redownload failed images")
    public void reDownload() throws IOException {
        RetryFailedDownloads.retryAll();
    }
}
