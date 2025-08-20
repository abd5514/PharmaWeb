package org.tab.tests;

import org.tab.base.Base;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.LoginPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.tab.data.TestDataReader.getXMLData;

@Listeners(ExtentTestListener.class)
public class LoginPageTest extends Base {

    @Test (description = "user able to login with valid credentials")
    public void Login() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.emailInput.sendKeys(getXMLData("username"));
        loginPage.passwordInput.sendKeys(getXMLData("password"));
        loginPage.loginBtn.click();
        loginPage.keepLoginCheckbox.click();
    }
}
