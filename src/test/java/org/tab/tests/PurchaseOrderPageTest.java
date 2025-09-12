package org.tab.tests;

import org.openqa.selenium.By;
import org.tab.base.Base;
import org.tab.data.ImageUploader;
import org.tab.data.JSONReader;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.utils.PropReader;
import org.tab.web_pages.HomePage;
import org.tab.web_pages.ProductsPage;
import org.tab.web_pages.PurchaseOrderPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.tab.utils.common.SharedMethods.*;

@Listeners(ExtentTestListener.class)
public class PurchaseOrderPageTest extends Base {


    @Test (description = "user able to add new Purchase Order")
    public void addNewPurchaseOrder() {
        login();
        HomePage homePage = new HomePage(driver);
        PurchaseOrderPage purchaseOrderPage = new PurchaseOrderPage(driver);
        waitUntilElementClickable(homePage.purchaseMenu);
        homePage.purchaseMenu.click();
        waitUntilElementClickable(homePage.purchaseOrderMenu);
        homePage.purchaseOrderMenu.click();
        purchaseOrderPage.createNewPurchaseOrderBtn.click();
        waitUntilElementClickable(purchaseOrderPage.supplierDDLContainer);
        purchaseOrderPage.supplierDDLContainer.click();
        staticWait(200);
        purchaseOrderPage.ulList(purchaseOrderPage.supplierDDLOptions);
        purchaseOrderPage.discountInput.clear();
        purchaseOrderPage.discountInput.sendKeys("5");
        purchaseOrderPage.fillPurchaseOrderForm("", purchaseOrderPage.productDDL,"new uom", "2",
                "1",
                purchaseOrderPage.discountTypeDDL, "normal",
                "10", purchaseOrderPage.lotDDL, 1);
        staticWait(10000);
    }

}
