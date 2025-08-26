package org.tab.web_pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;
import java.util.NoSuchElementException;

import static org.tab.utils.common.SharedMethods.generateRandomNumber;

public class ProductsPage {

    @FindBy (xpath="//button[@id='new_model']")
    public WebElement addNewProductBtn;
    @FindBy (xpath="(//input[@id='name'])[2]")
    public WebElement productNameInput;
    @FindBy (id="local_name")
    public WebElement productLocalNameInput;
    @FindBy (xpath="//button[normalize-space()='Update']")
    public WebElement updateBtn;
    @FindBy (xpath="//form[contains(@action, \"/dashboard/products\")]")
    public WebElement form;
    @FindBy (xpath="//div[@id='newModal']")
    public WebElement formMainDiv;
    @FindBy (xpath="(//table[@id='products-table']//a[@title='Edit'])")
    public List<WebElement> editBtns;
    @FindBy (xpath="(//button[@title='Delete'])")
    public List<WebElement> deleteBtns;
    @FindBy (xpath="//button[normalize-space()='Delete' and @type='submit']")
    public WebElement deleteConfirmBtn;



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

    public void editOrDeleteBtnsClick(List<WebElement> locator){
        if (locator.isEmpty()) {
            throw new NoSuchElementException("❌ No Edit links found in products table.");
        }
        int id= generateRandomNumber(locator.size());
        locator.get(0).click();
    }
}
