package org.tab.core.interfaces;

import org.openqa.selenium.WebDriver;

@Deprecated
public interface SelDriverProvider {
    WebDriver getBrowser(boolean headless);
}
