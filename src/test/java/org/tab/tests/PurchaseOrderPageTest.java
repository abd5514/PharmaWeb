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

    @Test(description = "json reader case")
    public void jsonReader() throws IOException {
        JSONReader reader = new JSONReader();

        String username = reader.getString("username");             // "abdallah"
        int timeout = reader.getInt("timeout");                     // 30
        boolean admin = reader.getBoolean("isAdmin");               // true
        String nestedName = reader.getNested("user.name");

        System.out.println(username + " | " + timeout + " | " + admin + " | " + nestedName);
    }

    @Test(description = "image upload test")
    public void imageUploadTest() {
        driver.get(PropReader.get("imagetesturl", "https://example.org"));
        // 1) Create once (e.g., in @BeforeClass)
// Put your images under: src/test/resources/images
        staticWait(20000);
        ImageUploader uploader = new ImageUploader();

// 2) Use on your page where the file input exists
        By fileInput = By.cssSelector("input[type='file'][name='productImage']"); // adjust locator
        uploader.uploadImage(driver, fileInput, "logo.png");   // with extension
        uploader.uploadImage(driver, fileInput, "logo");       // without extension (tries .png/.jpg/..)

    }
}
