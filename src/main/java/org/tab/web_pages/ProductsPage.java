package org.tab.web_pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

public class ProductsPage {

    @FindBy (xpath="//button[@id='new_model']")
    public WebElement addNewProductBtn;
    /*@FindBy (id="name")
    public WebElement productNameInput;
    @FindBy (id="category_id")
    public WebElement categoryDDL;
    @FindBy (id="selling_unit_id")
    public WebElement sellingUnitDDL;
    @FindBy (id="tax_id")
    public WebElement taxDDL;
    @FindBy (id="cost")
    public WebElement costInput;
    @FindBy (id="gross_price")
    public WebElement grossPriceInput;
    @FindBy (xpath="//button[@name='continue']")
    public WebElement saveAndContinueBtn;*/
    @FindBy (xpath="//form[contains(@action, \"/dashboard/products\")]")
    public WebElement form;
    @FindBy (xpath="//div[@id='newModal']")
    public WebElement formMainDiv;



    public ProductsPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void selectDDL(WebElement locator, String value){
        locator.click();
        locator.sendKeys(value, Keys.ENTER);
    }

    public void switchFrame(WebElement locator, WebDriver driver, String value){
        driver.switchTo().frame(form);
        formMainDiv.click();
        locator.click();
        locator.sendKeys(value);
        driver.switchTo().defaultContent();
    }

    public void fillProductForm(
            WebDriver driver,
            String productName,
            String cost,
            String grossPrice
    ) {
        WebElement productNameInput   = driver.findElement(By.xpath("(//input[@id='name'])[2]"));
        WebElement costInput          = driver.findElement(By.id("cost"));
        WebElement grossPriceInput    = driver.findElement(By.id("gross_price"));
        WebElement saveAndContinueBtn = driver.findElement(By.xpath("//button[@name='continue']"));

        try {
            productNameInput.clear();
            productNameInput.sendKeys(productName);
            costInput.clear();
            costInput.sendKeys(cost);
            grossPriceInput.clear();
            grossPriceInput.sendKeys(grossPrice);
            saveAndContinueBtn.click();
        } catch (Exception e) {
            System.out.println("❌ Failed to fill form: " + e.getMessage());
            throw e;
        }
    }
}
