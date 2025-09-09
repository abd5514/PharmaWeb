package org.tab.web_pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

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
    @FindBy (xpath="//button[@id='key-bindings-2']")
    public WebElement uploadBtn;
    @FindBy (xpath="//h3[normalize-space()='Saved']")
    public WebElement savedText;


    public StaffDashboardPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }



}
