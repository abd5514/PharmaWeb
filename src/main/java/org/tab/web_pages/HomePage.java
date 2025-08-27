package org.tab.web_pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class HomePage {

    @FindBy (xpath="//p[normalize-space()='Inventory']")
    public WebElement inventoryMenu;
    @FindBy (xpath="//p[normalize-space()='Reports']")
    public WebElement reportsMenu;
    @FindBy (xpath="//p[normalize-space()='Sales']")
    public WebElement salesMenu;
    @FindBy (xpath="//p[normalize-space()='Transactions']")
    public WebElement transactionsMenu;
    @FindBy (xpath="//p[normalize-space()='Purchase']")
    public WebElement purchaseMenu;
    @FindBy (xpath="//p[normalize-space()='Insurance']")
    public WebElement insuranceMenu;
    @FindBy (xpath="//p[normalize-space()='Customers']")
    public WebElement customersMenu;
    @FindBy (xpath="//p[normalize-space()='Marketing']")
    public WebElement marketingMenu;
    @FindBy (xpath="//p[normalize-space()='Accounting']")
    public WebElement accountingMenu;
    @FindBy (xpath="//p[normalize-space()='Apps']")
    public WebElement appsMenu;
    @FindBy (xpath="//p[normalize-space()='Users']")
    public WebElement usersMenu;
    @FindBy (xpath="//p[normalize-space()='Manage']")
    public WebElement manageMenu;
    @FindBy (xpath="//p[normalize-space()='Options']")
    public WebElement optionsMenu;
    @FindBy (xpath="//p[normalize-space()='Reasons']")
    public WebElement reasonsMenu;
    @FindBy (xpath="//p[normalize-space()='Settings']")
    public WebElement settingsMenu;
    @FindBy (xpath="//a[contains(@href, \"/dashboard/products\")]")
    public WebElement productsMenu;
    @FindBy (xpath="//a[contains(@href, \"/dashboard/purchase-receives\")]")
    public WebElement purchaseOrderMenu;


    public HomePage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }


}
