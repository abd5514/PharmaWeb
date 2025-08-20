package org.tab.core.instance;

import org.openqa.selenium.WebDriver;

public class DriverManager {
    private static final ThreadLocal<WebDriver> TL = new ThreadLocal<>();

    public static WebDriver getDriver() {
        return TL.get();
    }

    public static void setDriver(WebDriver driver) {
        TL.set(driver);
    }

    public static void unload() {
        WebDriver d = TL.get();
        if (d != null) {
            try { d.quit(); } catch (Exception ignored) {}
            TL.remove();
        }
    }
}
