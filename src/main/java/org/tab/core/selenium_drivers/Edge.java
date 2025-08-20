package org.tab.core.selenium_drivers;

import org.tab.core.interfaces.DriverProvider;
import org.tab.core.interfaces.SelDriverProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

public class Edge implements DriverProvider, SelDriverProvider {

    @Override
    public WebDriver getBrowser(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) options.addArguments("--headless=new");
        return new EdgeDriver(options);
    }
}
