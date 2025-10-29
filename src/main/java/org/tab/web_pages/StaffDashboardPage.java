package org.tab.web_pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;
import org.tab.data.NewImageUploader;
import org.tab.utils.CSVLogger;
import org.tab.utils.PDFConverter;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.tab.data.TestDataReader.getXMLData;
import static org.tab.utils.CSVLogger.logSkipped;
import static org.tab.utils.CSVLogger.logUrls;
import static org.tab.utils.common.SharedMethods.*;

public class StaffDashboardPage {

    @FindBy (id="data.email")
    public WebElement userNameInput;
    @FindBy (id="data.password")
    public WebElement passwordInput;
    @FindBy (xpath="//button[@type='submit']")
    public WebElement loginBtn;
    @FindBy (xpath="//span[normalize-space()='Stores']")
    public WebElement sideMenuStores;
    @FindBy (id="input-1")
    public WebElement searchTenantInput;
    @FindBy (xpath="//button[contains(@class,'fi-icon-btn') and contains(@class,'fi-ac-icon-btn-group')]")
    public WebElement moreBtn;
    @FindBy (xpath="//a[contains(@type,'button')]")
    public WebElement editBtn;
    @FindBy (css="input.filepond--browser[type='file']")
    public WebElement uploadInput;
    @FindBy (xpath="//button[@id='key-bindings-1']")
    public WebElement uploadBtn;
    @FindBy (xpath="//button[@title='Filter']")
    public WebElement filterBtn;
    @FindBy (xpath="//select[@id='tableFilters.city.value']")
    public WebElement cityDropdown;
    @FindBy (xpath="//div[@class='filepond--image-preview-wrapper']")
    public WebElement imageContainer;
    @FindBy(xpath = "//div[contains(@class,'flex w-full gap-3 p-4')]")
    public WebElement savePopup;
    @FindBy(xpath = "//button[@aria-label='Next']")
    public WebElement nextBtn;
    @FindBy(xpath = "//table")
    public WebElement tableElement;

    public StaffDashboardPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void assertDisplayedAndLog(WebElement el, String city, String store, int images) {
        if (!el.isDisplayed()) {
            logSkipped(city, store, null, images);
            Assert.fail("❌ Image container NOT displayed for store=" + store);
        }
    }

    public int[] processCity(WebDriver driver,String city, NewImageUploader imageUploader, StaffDashboardPage staffDashboardPage) throws IOException {
        int uploadCount = 0;
        int skipCount = 0;
        int j=0;
        List<String> storeFolders = imageUploader.getStoreFolderNames(city);
        for (int i = j; i <storeFolders.size(); i++) {
            String store = storeFolders.get(i);
            /*// ✅ Remove trailing timestamp if present
            store = store.replaceAll("_[0-9]{8}_[0-9]{6}$", "");*/
            // ✅ Read original name from original_name.txt if exists
            String originalName = readOriginalStoreName(city, store);
            String storeXpath = /*"//span[normalize-space()='" + store + "']";*/"//div[@data-store='id_"+originalName+"']//span";
//            String storeXpath = /*"//span[normalize-space()='" + store + "']";*/"//div[@data-store='id_"+store+"']//span";
            String storeSearch = store.replace(" ", "+");
            List<String> images = null;
            try {
                images = imageUploader.getImagePathsInFolder(city, store);
            } catch (IOException e) {
                logSkipped(city, store, e, 0);
                continue;
            }
            if (images.isEmpty()) {
                logSkipped(city, store, null, 0);
                continue;
            }
            else if(images.size()>60){
                CSVLogger.logSkipped(city, store, null, images.size());
                skipCount++;
                continue;
            }
            try {
                String filterUrl = getXMLData("baseuploaderUrl")
                        + "?tableFilters[city][value]=" + city
                        + "&tableSearch=" + storeSearch;
                driver.get(filterUrl);
                staticWait(100);
                /*int products = 0;
                try {
                    products = Integer.parseInt(
                            driver.findElement(By.xpath("//div[@data-store='product_"+store+"']//span")).getText()
                    );
                }catch (Exception e){
                    String fallbackXpath = "(//span[contains(normalize-space(),'" + store + "')])[4]";
                    products = Integer.parseInt(
                            driver.findElement(By.xpath(fallbackXpath + "/ancestor::tr//td[13]//span")).getText()
                    );
                }*/
                try {
                    driver.findElement(By.xpath(storeXpath)).click();
                } catch (NoSuchElementException ex) {
                    storeXpath = "//div[contains(@data-store,'id_" + store + "')]//span";
                    System.out.println("⚠️ Fallback to: " + storeXpath);
                    driver.findElement(By.xpath(storeXpath)).click();
                }

                pageBottom();
                int waitTime;
                if(images.size()<3){ waitTime=images.size()*1000;}
                else {waitTime =images.size()*400;}
                staticWait(800);
//                System.out.println("📦 Products BEFORE upload for store [" + store + "]: " + products);
                staffDashboardPage.uploadInput.clear();
                staticWait(800);
                imageUploader.uploadAllAtOnce(driver, staffDashboardPage.uploadInput, images);
                staticWait(500);
                pageBottom();
                pageBottom();
                waitUntilTextChanged(staffDashboardPage.uploadBtn, "Save changes");
                pageBottom();
                pageBottom();
                staticWait(1200);
                staffDashboardPage.uploadBtn.click();
                clickUntilElementFound(driver,staffDashboardPage.uploadBtn,staffDashboardPage.savePopup,20);
//                try{staffDashboardPage.uploadBtn.click();}catch (Exception ignored){}
                System.out.printf("✅ [%s] Store %s uploaded (%d images)%n", city, store, images.size());
                uploadCount++;
                staticWait(waitTime);
                /*// 🔎 Use dynamic wait instead of fixed sleep
                driver.get(filterUrl);
                staticWait(200);
                products = waitForProductsAboveZero(driver, store,images.size(),filterUrl);
                System.out.println("📦 Products AFTER upload for store [" + store + "]: " + products);

                if (products <= 0) {
                    CSVLogger.logSkipped(city, store, null, images.size());
                    skipCount++;
                }*/
                System.out.println("current loop  " + i + " store  " + store + " uploaded");
            } catch (Exception e) {
                logSkipped(city, store, e, 0);
                System.out.println("❌ current loop  " + i + " store  " + store + " skipped");
                skipCount++;
            }
        }
        return new int[]{uploadCount, skipCount};
    }

    /**
     * Dynamic wait until product count > 0 for the given store row.
     */
    public int waitForProductsAboveZero(WebDriver driver, String storeXpath,int imagesCount,String url) {
        WebElement span = driver.findElement(By.xpath("//span[normalize-space()='" + storeXpath + "']/ancestor::tr//td[13]//span"));
        System.out.println("Found span text = " + span.getText());
        int timeoutSeconds;
        if(imagesCount<2){timeoutSeconds=imagesCount*120;}
        else{timeoutSeconds = imagesCount*80;}
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))  // <-- Add timeout
                .pollingEvery(Duration.ofMillis(10000))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        /*return wait.until(d -> {
            WebElement span1 = d.findElement(By.xpath(storeXpath + "/ancestor::tr//td[12]//span"));
            String text = span1.getText().trim();
            if (!text.isEmpty() && text.matches("\\d+")) {
                int value = Integer.parseInt(text);
                if (value > 0) {
                    return value;
                }
            }
            return null; // keep polling
        });*/
        /*return wait.until(d -> {
            try {
                try {
                    driver.get(url);
                    String text = span1.getText().trim();
                    System.out.println("DEBUG -> Found text: '" + text + "'");
                    if (!text.isEmpty() && text.matches("\\d+")) {
                        int value = Integer.parseInt(text);
                        if (value > 0) {
                            System.out.println("DEBUG -> Returning value: " + value);
                            return value;
                        }
                    }
                } catch (Exception e) {
                    driver.get(url);
                    String text = span.getText().trim();
                    System.out.println("DEBUG -> Found text: '" + text + "'");
                    if (!text.isEmpty() && text.matches("\\d+")) {
                        int value = Integer.parseInt(text);
                        if (value > 0) {
                            System.out.println("DEBUG -> Returning value: " + value);
                            return value;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("DEBUG -> NumberFormatException: " + e.getMessage());
            }
            return null; // keep polling
        });*/
        return wait.until(d -> {
            try {
                d.get(url); // careful: this reloads page every poll
                WebElement spanCandidate;
                try {
                    spanCandidate = d.findElement(By.xpath("(//span[contains(normalize-space(),'" + storeXpath + "')])[4]/ancestor::tr//td[13]//span"));
                } catch (NoSuchElementException e1) {
                    spanCandidate = d.findElement(By.xpath("//span[normalize-space()='" + storeXpath + "']/ancestor::tr//td[13]//span"));
                }

                String text = spanCandidate.getText().trim();
                System.out.println("DEBUG -> Found text: '" + text + "'"); // ✅ will print every poll

                if (!text.isEmpty() && text.matches("\\d+")) {
                    int value = Integer.parseInt(text);
                    if (value > 0) {
                        System.out.println("DEBUG -> Returning value: " + value);
                        return value;
                    }
                }
            } catch (Exception e) {
                System.out.println("DEBUG -> Exception in poll: " + e.getMessage());
            }
            return null; // keep polling
        });
    }

    /**
     * Reads the original store name (before sanitization) from the folder’s original_name.txt.
     * @param city City name used in folder path
     * @param sanitizedFolderName The sanitized store folder name
     * @return Original store name if found, otherwise null
     */
    private String readOriginalStoreName(String city, String sanitizedFolderName) {
        File txt = new File("src/test/resources/images/" + city + "/" + sanitizedFolderName + "/original_name.txt");
        if (txt.exists() && txt.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(txt))) {
                String line = br.readLine();
                if (line != null) {
                    return line.trim();
                }
            } catch (IOException ignored) {}
        }
        return sanitizedFolderName;
    }

    /**
     * check zero stores*/
    /*public void zeroStoresCheck(WebDriver driver,String city, NewImageUploader imageUploader, StaffDashboardPage staffDashboardPage) throws IOException {
        String filterUrl = getXMLData("baseuploaderUrl");
        driver.get(filterUrl);
        while (nextBtn.isDisplayed()) {
            List<WebElement> rows = driver.findElements(By.cssSelector("table.fi-ta-table tbody tr"));
            for (WebElement row : rows) {
                // Store name (Name column)
                String storeName = row.findElement(By.cssSelector("td.fi-table-cell-name span.fi-ta-text-item-label")).getText().trim();
                // City (City column)
                String cityn = row.findElement(By.cssSelector("td.fi-table-cell-city span.fi-ta-text-item-label")).getText().trim();
                // Google link (Google Link column)
                WebElement googleLinkElement = row.findElement(By.cssSelector("td.fi-table-cell-url a"));
                String googleLink = googleLinkElement.getAttribute("href");
                // Menu count (Menu column)
                String menuCount = row.findElement(By.cssSelector("td.fi-table-cell-products-count span.fi-ta-text-item-label")).getText().trim();
                if (Integer.parseInt(menuCount) <= 0) {
                    logUrls(city, storeName, googleLink);
                }
            }
            nextBtn.click();
        }


    }*/

    public void zeroStoresCheck(WebDriver driver) throws IOException {
        String filterUrl = getXMLData("baseuploaderUrl");
        driver.get(filterUrl);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        boolean hasNext = true;
        while (hasNext) {
            // Wait for table to be visible (after each reload)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.fi-ta-table tbody")));

            List<WebElement> rows = driver.findElements(By.cssSelector("table.fi-ta-table tbody tr"));
            System.out.println("Found rows: " + rows.size());

            for (int i = 0; i < rows.size(); i++) {
                try {
                    WebElement row = rows.get(i);

                    String storeName = row.findElement(By.cssSelector("td.fi-table-cell-name span.fi-ta-text-item-label")).getText().trim();
                    String neighborhood = row.findElement(By.cssSelector("td.fi-table-cell-neighborhood span.fi-ta-text-item-label")).getText().trim();
                    String cityName = row.findElement(By.cssSelector("td.fi-table-cell-city span.fi-ta-text-item-label")).getText().trim();
                    String googleLink = row.findElement(By.cssSelector("td.fi-table-cell-url a")).getAttribute("href");
                    String menuCount = row.findElement(By.cssSelector("td.fi-table-cell-products-count span.fi-ta-text-item-label")).getText().trim();

                    if (Integer.parseInt(menuCount) <= 0) {
                        logUrls(cityName,neighborhood, storeName, googleLink);
                    }

                } catch (StaleElementReferenceException e) {
                    // If page refreshed mid-loop, skip this row and continue safely
                    System.out.println("⚠️ Skipping stale row, will continue...");
                    continue;
                }
            }

            try {
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[aria-label='Next']")));
                if (nextBtn.isDisplayed() && nextBtn.isEnabled()) {
                    nextBtn.click();
                    // Wait for the old table to refresh before next loop
                    wait.until(ExpectedConditions.stalenessOf(rows.get(0)));
                } else {
                    hasNext = false;
                }
            } catch (TimeoutException | NoSuchElementException e) {
                hasNext = false;
            }
        }
    }

    public void zeroStoresCheck(WebDriver driver, String pageNum) throws IOException {
        String filterUrl = getXMLData("baseuploaderUrl") + "?page=" + pageNum;
        driver.get(filterUrl);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        boolean hasNext = true;
        while (hasNext) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.fi-ta-table tbody")));

            List<WebElement> rows = driver.findElements(By.cssSelector("table.fi-ta-table tbody tr"));
            System.out.println("Found rows: " + rows.size());

            for (int i = 0; i < rows.size(); i++) {
                try {
                    WebElement row = rows.get(i);

                    String storeName = row.findElement(By.cssSelector("td.fi-table-cell-name span.fi-ta-text-item-label")).getText().trim();
                    String cityName = safeGetText(row, "td.fi-table-cell-city span.fi-ta-text-item-label");
                    String neighborhood = safeGetText(row, "td.fi-table-cell-neighborhood span.fi-ta-text-item-label");
                    String googleLink = row.findElement(By.cssSelector("td.fi-table-cell-url a")).getAttribute("href");
                    String menuCount = safeGetText(row, "td.fi-table-cell-products-count span.fi-ta-text-item-label");

                    if (menuCount.isEmpty()) menuCount = "0";

                    if (Integer.parseInt(menuCount) <= 0) {
                        logUrls(cityName,neighborhood, storeName, googleLink);
                    }

                } catch (StaleElementReferenceException e) {
                    System.out.println("⚠️ Stale row, skipping...");
                } catch (NoSuchElementException e) {
                    System.out.println("⚠️ Missing data in row, skipping...");
                }
            }

            try {
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[aria-label='Next']")));
                if (nextBtn.isDisplayed() && nextBtn.isEnabled()) {
                    nextBtn.click();
                    wait.until(ExpectedConditions.stalenessOf(rows.get(0)));
                } else {
                    hasNext = false;
                }
            } catch (TimeoutException | NoSuchElementException e) {
                hasNext = false;
            }
        }
    }

    /**
     * Helper to safely get inner text, return "N/A" if element missing or empty.
     */
    private String safeGetText(WebElement parent, String cssSelector) {
        try {
            WebElement el = parent.findElement(By.cssSelector(cssSelector));
            String text = el.getText().trim();
            return text.isEmpty() ? "N/A" : text;
        } catch (NoSuchElementException e) {
            return "N/A";
        }
    }

}
