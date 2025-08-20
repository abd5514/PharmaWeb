package org.tab.core.selenium_drivers;

import org.tab.core.interfaces.DriverProvider;
import org.tab.core.interfaces.SelDriverProvider;
import org.tab.utils.ExtentReport.DesiredListener;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public class EdgeRemote implements DriverProvider, SelDriverProvider {
    private final String remoteUrl;

    public EdgeRemote(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @Override
    public WebDriver getBrowser(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) options.addArguments("--headless=new");
        DesiredListener.handleDesiredCaps(options);
        try {
            return new RemoteWebDriver(new URL(remoteUrl), options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Remote URL is invalid: " + remoteUrl, e);
        }
    }
}
