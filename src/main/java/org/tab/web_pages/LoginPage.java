package org.tab.web_pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import static org.tab.data.TestDataReader.getXMLData;

public class LoginPage {


    @FindBy (id="emailInput")
    public WebElement emailInput;
    @FindBy (id="keepLogin")
    public WebElement keepLoginCheckbox;
    @FindBy (xpath="//input[@placeholder='Password']")
    public WebElement passwordInput;
    @FindBy (xpath="//button[@type='submit']")
    public WebElement loginBtn;


    public LoginPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void doLogin(){
        emailInput.sendKeys(getXMLData("username"));
        passwordInput.sendKeys(getXMLData("password"));
        loginBtn.click();
    }
}
