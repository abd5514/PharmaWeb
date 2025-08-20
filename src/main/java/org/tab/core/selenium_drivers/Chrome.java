package org.tab.core.selenium_drivers;

import org.tab.core.interfaces.DriverProvider;
import org.tab.core.interfaces.SelDriverProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Chrome implements DriverProvider, SelDriverProvider {

    @Override
    public WebDriver getBrowser(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--start-maximized");
        return new ChromeDriver(options); // Selenium Manager will provision the driver
    }
}
