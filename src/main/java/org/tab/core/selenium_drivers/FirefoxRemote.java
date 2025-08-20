package org.tab.core.selenium_drivers;

import org.tab.core.interfaces.DriverProvider;
import org.tab.core.interfaces.SelDriverProvider;
import org.tab.utils.ExtentReport.DesiredListener;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public class FirefoxRemote implements DriverProvider, SelDriverProvider {
    private final String remoteUrl;

    public FirefoxRemote(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @Override
    public WebDriver getBrowser(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("-headless");
        DesiredListener.handleDesiredCaps(options);
        try {
            return new RemoteWebDriver(new URL(remoteUrl), options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Remote URL is invalid: " + remoteUrl, e);
        }
    }
}
