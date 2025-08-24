package org.tab.tests;

import org.tab.base.Base;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.HomePage;
import org.tab.web_pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.tab.core.instance.DriverManager.getDriver;
import static org.tab.data.TestDataReader.getXMLData;
import static org.tab.utils.common.SharedMethods.login;
import static org.tab.utils.common.SharedMethods.waitUntilElementClickable;

@Listeners(ExtentTestListener.class)
public class HomePageTest extends Base {

    @Test (description = "user able to click side menu")
    public void clickSideMenu() {
        login();
        HomePage homePage = new HomePage(driver);
        waitUntilElementClickable(homePage.inventoryMenu);
        homePage.inventoryMenu.click();
        waitUntilElementClickable(homePage.productsMenu);
        homePage.productsMenu.click();
    }
}
