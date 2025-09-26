package org.tab.web_pages;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

import static org.tab.utils.CSVLogger.logSkipped;

public class StaffDashboardPage {

    @FindBy (id="data.email")
    public WebElement userNameInput;
    @FindBy (id="data.password")
    public WebElement passwordInput;
    @FindBy (xpath="//button[@type='submit']")
    public WebElement loginBtn;
    @FindBy (xpath="//span[normalize-space()='Stores']")
    public WebElement sideMenuStores;
    @FindBy (id="input-1")
    public WebElement searchTenantInput;
    @FindBy (xpath="//button[contains(@class,'fi-icon-btn') and contains(@class,'fi-ac-icon-btn-group')]")
    public WebElement moreBtn;
    @FindBy (xpath="//a[contains(@type,'button')]")
    public WebElement editBtn;
    @FindBy (css="input.filepond--browser[type='file']")
    public WebElement uploadInput;
    @FindBy (xpath="//button[@id='key-bindings-1']")
    public WebElement uploadBtn;
    @FindBy (xpath="//button[@title='Filter']")
    public WebElement filterBtn;
    @FindBy (xpath="//select[@id='tableFilters.city.value']")
    public WebElement cityDropdown;
    @FindBy (xpath="//div[@class='filepond--image-preview-wrapper']")
    public WebElement imageContainer;
    @FindBy(xpath = "//div[contains(@class,'flex w-full gap-3 p-4')]")
    public WebElement savePopup;



    public StaffDashboardPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void assertDisplayedAndLog(WebElement el, String city, String store, int images) {
        if (!el.isDisplayed()) {
            logSkipped(city, store, null, images);
            Assert.fail("❌ Image container NOT displayed for store=" + store);
        }
    }
}
