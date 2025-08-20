package org.tab.core.instance;

import org.tab.core.enums.Drivers;
import org.tab.core.interfaces.DriverProvider;
import org.tab.core.selenium_drivers.*;
import org.tab.utils.PropReader;
import org.openqa.selenium.WebDriver;

public class DriverFactory {

    public static WebDriver create() {
        String browser = PropReader.get("browser", "chrome").toUpperCase();
        boolean headless = Boolean.parseBoolean(PropReader.get("headless", "false"));
        boolean remote = Boolean.parseBoolean(PropReader.get("remote", "false"));
        String remoteUrl = PropReader.get("remoteUrl", "http://localhost:4444/wd/hub");

        Drivers drv = Drivers.valueOf(browser);
        DriverProvider provider;

        if (remote) {
            provider = switch (drv) {
                case CHROME -> new ChromeRemote(remoteUrl);
                case FIREFOX -> new FirefoxRemote(remoteUrl);
                case EDGE -> new EdgeRemote(remoteUrl);
                case SAFARI -> throw new UnsupportedOperationException("Safari remote not configured");
            };
        } else {
            provider = switch (drv) {
                case CHROME -> new Chrome();
                case FIREFOX -> new Firefox();
                case EDGE -> new Edge();
                case SAFARI -> throw new UnsupportedOperationException("Safari local not configured");
            };
        }
        return provider.getBrowser(headless);
    }
}
