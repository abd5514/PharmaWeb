# Selenium Framework (Modernized for Selenium 4.35)

This is a fresh project that **preserves your package structure** (`org.tab.core.*`, `org.tab.utils.*`, `org.tab.web_pages.*`) but
**removes deprecated APIs** (e.g., `DesiredCapabilities`) and switches to Selenium 4 **Options** + **Selenium Manager**.

## Highlights
- Java 17, Selenium **4.35.0**, TestNG **7.11.0**
- Thread‑safe `DriverManager` for parallel readiness
- Local & Remote drivers with `Chrome/Firefox/Edge` + `*Remote` providers
- `DesiredListener.handleDesiredCaps(MutableCapabilities)` retained (modernized) to keep structure
- Config via `src/main/resources/config.properties` or `-D` system properties
- Simple Page Object + sample TestNG test

## Quick Start
```bash
mvn -q -DskipTests test
# or pick browser/headless/remote at runtime
mvn -q -Dbrowser=chrome -Dheadless=true -DbaseUrl=https://example.org test
```

## Key Changes from your legacy framework
- **Replaced `DesiredCapabilities` with `ChromeOptions/FirefoxOptions/EdgeOptions`** (Selenium 4 requirement).
- **RemoteWebDriver** now receives an `Options` object directly.
- **No WebDriverManager dependency** — Selenium Manager handles driver provisioning.
- Preserved class/package names where possible so your tests migrate with minimal edits.
