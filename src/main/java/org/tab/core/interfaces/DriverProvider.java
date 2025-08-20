package org.tab.core.interfaces;

import org.openqa.selenium.WebDriver;

public interface DriverProvider {
    WebDriver getBrowser(boolean headless);
}
