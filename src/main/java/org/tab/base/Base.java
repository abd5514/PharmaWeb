package org.tab.base;

import org.tab.core.instance.DriverFactory;
import org.tab.core.instance.DriverManager;
import org.tab.data.TestDataReader;
import org.tab.utils.PropReader;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class Base {

    protected WebDriver driver;
    protected String baseUrl;


    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.create();
        DriverManager.setDriver(driver);
        baseUrl = PropReader.get("baseUrl", "https://example.org");

        driver.get(baseUrl);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.unload();
    }
}
