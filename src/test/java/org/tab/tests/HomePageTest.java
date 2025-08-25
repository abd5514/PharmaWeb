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
import static org.tab.utils.common.SharedMethods.*;

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
        staticWait(300);
        homePage.salesMenu.click();
        staticWait(300);
        homePage.reportsMenu.click();
        staticWait(300);
        homePage.transactionsMenu.click();
        staticWait(300);
        homePage.purchaseMenu.click();
        staticWait(300);
        homePage.insuranceMenu.click();
        staticWait(300);
        homePage.customersMenu.click();
        staticWait(300);
        homePage.marketingMenu.click();
        staticWait(300);
        homePage.accountingMenu.click();
        staticWait(300);
        homePage.usersMenu.click();
        staticWait(300);
        homePage.manageMenu.click();
        staticWait(300);
        homePage.optionsMenu.click();
        staticWait(300);
        homePage.reasonsMenu.click();
        staticWait(300);
        homePage.settingsMenu.click();
        staticWait(300);
    }
}
