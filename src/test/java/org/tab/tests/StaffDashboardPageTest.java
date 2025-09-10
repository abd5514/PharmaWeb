package org.tab.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.tab.base.Base;
import org.tab.data.ImageUploader;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.StaffDashboardPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

import static org.tab.data.TestDataReader.getXMLData;
import static org.tab.utils.common.SharedMethods.*;

@Listeners(ExtentTestListener.class)
public class StaffDashboardPageTest extends Base {

    @Test(description = "menu image uploader")
    public void menuUploader() {
        ImageUploader imageUploader = new ImageUploader();
        List<String> storeFolders = imageUploader.getImageFolderNames();
        driver.get(getXMLData("staffurl"));
        StaffDashboardPage staffDashboardPage = new StaffDashboardPage(driver);
        staffDashboardPage.userNameInput.sendKeys(getXMLData("staffusername"));
        staffDashboardPage.passwordInput.sendKeys(getXMLData("staffpassword"));
        staffDashboardPage.loginBtn.click();
        for(int i=80;i<=storeFolders.size();i++){
            String storeXpath = "//span[normalize-space()='"+storeFolders.get(i)+"']";
            try {
                waitUntilElementClickable(staffDashboardPage.sideMenuStores);
                staffDashboardPage.sideMenuStores.click();
                waitUntilElementVisible(staffDashboardPage.searchTenantInput);
                staffDashboardPage.searchTenantInput.sendKeys(storeFolders.get(i), Keys.ENTER);
                waitUntilElementClickable(staffDashboardPage.moreBtn);
                staticWait(1800);
                driver.findElement(By.xpath(storeXpath)).click();
                pageBottom();
                List<String> images = imageUploader.getImagePathsInFolder(storeFolders.get(i));
                staticWait(1000);
                try {
                    for (String imgPath : images) {
                        imageUploader.uploadImage(driver, staffDashboardPage.uploadInput, imgPath);
                        staticWait(300);
                        waitUntilTextChanged(staffDashboardPage.uploadBtn, "Save changes");
                    }
                } catch (Exception e) {
                    System.out.println("fg3 3a image   " );
                    continue;
                }
                staffDashboardPage.uploadBtn.click();
                staticWait(2000);
                System.out.println("current loop  " + i + " store   " + storeFolders.get(i) + " uploaded");
//                driver.manage().deleteAllCookies();
                staticWait(700);
            } catch (Exception e) {
                System.out.println("current loop  " + i + " store   " + storeFolders.get(i) + " skipped    " + e.getMessage());
            }
//            driver.navigate().refresh();
        }
    }
}
