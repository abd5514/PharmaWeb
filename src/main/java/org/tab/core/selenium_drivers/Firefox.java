package org.tab.core.selenium_drivers;

import org.tab.core.interfaces.DriverProvider;
import org.tab.core.interfaces.SelDriverProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class Firefox implements DriverProvider, SelDriverProvider {

    @Override
    public WebDriver getBrowser(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("-headless");
        return new FirefoxDriver(options);
    }
}
