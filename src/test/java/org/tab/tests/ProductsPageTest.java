package org.tab.tests;

import org.tab.base.Base;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.HomePage;
import org.tab.web_pages.ProductsPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.tab.utils.common.SharedMethods.*;

@Listeners(ExtentTestListener.class)
public class ProductsPageTest extends Base {


    @Test (description = "user able to add new product")
    public void addNewProduct() {
        login();
        HomePage homePage = new HomePage(driver);
        ProductsPage productsPage = new ProductsPage(driver);
        waitUntilElementClickable(homePage.inventoryMenu);
        homePage.inventoryMenu.click();
        waitUntilElementClickable(homePage.productsMenu);
        homePage.productsMenu.click();
        waitUntilElementClickable(productsPage.addNewProductBtn);
        productsPage.addNewProductBtn.click();
        productsPage.fillProductForm(driver, generateRandomString(7), "1.3", "1.7");
    }

    @Test (description = "user able to edit product")
    public void editProduct() {
        login();
        HomePage homePage = new HomePage(driver);
        ProductsPage productsPage = new ProductsPage(driver);
        waitUntilElementClickable(homePage.inventoryMenu);
        homePage.inventoryMenu.click();
        waitUntilElementClickable(homePage.productsMenu);
        homePage.productsMenu.click();
        staticWait(600);
        productsPage.editOrDeleteBtnsClick(productsPage.editBtns);
        waitUntilElementVisible(productsPage.productNameInput);
        productsPage.productLocalNameInput.clear();
        productsPage.productLocalNameInput.sendKeys(" update "+generateRandomString(5));
        productsPage.updateBtn.click();
    }

    @Test (description = "user able to delete product")
    public void deleteProduct() {
        login();
        HomePage homePage = new HomePage(driver);
        ProductsPage productsPage = new ProductsPage(driver);
        waitUntilElementClickable(homePage.inventoryMenu);
        homePage.inventoryMenu.click();
        waitUntilElementClickable(homePage.productsMenu);
        homePage.productsMenu.click();
        staticWait(600);
        productsPage.editOrDeleteBtnsClick(productsPage.deleteBtns);
        waitUntilElementClickable(productsPage.deleteConfirmBtn);
        productsPage.deleteConfirmBtn.click();

    }
}
