package org.tab.utils.ExtentReport;

import org.openqa.selenium.MutableCapabilities;

/**
 * Placeholder to preserve structure. In Selenium 4+, shift from DesiredCapabilities to Options/Capabilities.
 * This method lets you attach metadata (e.g., test name/build) to capabilities if you need it.
 */
public class DesiredListener {

    private static final ThreadLocal<String> desiredName = new ThreadLocal<>();

    public static void setDesiredName(String name) {
        desiredName.set(name);
    }

    public static void clear() {
        desiredName.remove();
    }

    public static void handleDesiredCaps(MutableCapabilities caps) {
        String name = desiredName.get();
        if (name != null) {
            // Common vendor‑agnostic metadata slots
            caps.setCapability("name", name);
            caps.setCapability("build", System.getProperty("build", "local"));
        }
    }
}
