package org.tab.tests;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.tab.base.Base;
import org.tab.core.instance.DriverFactory;
import org.tab.data.ImageUploader;
import org.tab.data.NewImageUploader;
import org.tab.utils.CSVLogger;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.utils.NewImageMenuFilterParallel;
import org.tab.web_pages.StaffDashboardPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
// --- add these imports at the top of StaffDashboardPageTest.java ---
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


import static org.tab.data.TestDataReader.getXMLData;
import static org.tab.utils.CSVLogger.logSkipped;
import static org.tab.utils.ImageMenuFilterParallel.processRootOnce;
import static org.tab.utils.common.SharedMethods.*;

@Listeners(ExtentTestListener.class)
public class StaffDashboardPageTest extends Base {

    @Test(description = "menu image uploader new")
    public void newMenuUploader() {
        int skipCount = 0;
        int uploadCount = 0;
        ImageUploader imageUploader = new ImageUploader();
        List<String> storeFolders = imageUploader.getImageFolderNames();
        StaffDashboardPage staffDashboardPage = new StaffDashboardPage(driver);
        staffDashboardPage.userNameInput.sendKeys(getXMLData("staffusername"));
        staffDashboardPage.passwordInput.sendKeys(getXMLData("staffpassword"));
        staffDashboardPage.loginBtn.click();
        waitUntilElementClickable(staffDashboardPage.sideMenuStores);
        for(int i=0;i<=storeFolders.size()-1;i++){
            String storeXpath = "//span[normalize-space()='"+storeFolders.get(i)+"']";
            String storeSearch= storeFolders.get(i).replace(" ","+");
            //dmmam city
            String filterUrl= getXMLData("baseuploaderUrl") +"?tableFilters[city][value]="+getXMLData("currentcity")+"&tableSearch="+storeSearch;
            try {
                driver.get(filterUrl);
                try {
                    driver.findElement(By.xpath(storeXpath)).click();
                } catch (org.openqa.selenium.NoSuchElementException ex) {
                    String fallbackXpath = "(//span[contains(normalize-space(),'" + storeFolders.get(i) + "')])[4]";
                    System.out.println("⚠️ Fallback to: " + fallbackXpath);
                    driver.findElement(By.xpath(fallbackXpath)).click();
                }
                pageBottom();
                List<String> images = imageUploader.getImagePathsInFolder(storeFolders.get(i));
                staticWait(500);
                try {
                    /*if(images.size()>9){
                        images = images.subList(0, 9);// limit to first 9 images
                        imageUploader.uploadAllAtOnce(driver, staffDashboardPage.uploadInput, images);
                        logSkipped(storeFolders.get(i),null, imagesCount);
                    }
                    else*/
                    imageUploader.uploadAllAtOnce(driver, staffDashboardPage.uploadInput, images);
                    staticWait(200);
                    waitUntilTextChanged(staffDashboardPage.uploadBtn, "Save changes");
                } catch (Exception e) {
                    System.out.println("fg3 3a image   " + i);
                    continue;
                }
                staffDashboardPage.uploadBtn.click();
                System.out.println("current loop  " + i + " store   " + storeFolders.get(i) + " uploaded");
                uploadCount++;
                int param=(i+1)%400;
                if(param==0){
                    staticWait(100000);
                }else {
                    staticWait(60000);}
            } catch (Exception e) {
//                logSkipped(city, store, e, 0);
                skipCount++;
            }
        }
        System.out.println("Total stores uploaded  " + uploadCount + "  skipped  " + skipCount);
    }

    @Test(description = "menu image uploader new")
    public void newMenuUploader(int i) {
        int skipCount = 0;
        int uploadCount = 0;
        ImageUploader imageUploader = new ImageUploader();
        List<String> storeFolders = imageUploader.getImageFolderNames();
        if (i < 0 || i >= storeFolders.size()) {
            System.out.println("Index out of bounds: " + i);
            return;
        }
        StaffDashboardPage staffDashboardPage = new StaffDashboardPage(driver);
        staffDashboardPage.userNameInput.sendKeys(getXMLData("staffusername"));
        staffDashboardPage.passwordInput.sendKeys(getXMLData("staffpassword"));
        staffDashboardPage.loginBtn.click();
        waitUntilElementClickable(staffDashboardPage.sideMenuStores);

        String storeFolder = storeFolders.get(i);
        String storeXpath = "//span[normalize-space()='" + storeFolder + "']";
        String storeSearch = storeFolder.replace(" ", "+");
        String filterUrl = getXMLData("baseuploaderUrl") + "?tableFilters[city][value]=" + getXMLData("currentcity") + "&tableSearch=" + storeSearch;
        try {
            driver.get(filterUrl);
            driver.findElement(By.xpath(storeXpath)).click();
            pageBottom();
            List<String> images = imageUploader.getImagePathsInFolder(storeFolder);
            staticWait(500);
            try {
                imageUploader.uploadAllAtOnce(driver, staffDashboardPage.uploadInput, images);
                staticWait(200);
                waitUntilTextChanged(staffDashboardPage.uploadBtn, "Save changes");
            } catch (Exception e) {
                System.out.println("fg3 3a image   " + i);
                return;
            }
            staffDashboardPage.uploadBtn.click();
            System.out.println("current loop  " + i + " store   " + storeFolder + " uploaded");
            uploadCount++;
            staticWait(100);
        } catch (Exception e) {
            System.out.println("current loop  " + i + " store   " + storeFolder + " skipped    ");
            skipCount++;
        }
        System.out.println("Total stores uploaded  " + uploadCount + "  skipped  " + skipCount);
    }

    /*@Test(description = "Run newMenuUploader in parallel (not recommended, for demo only)")
    public void runNewMenuUploaderInParallel() throws InterruptedException {
        int parallelRuns = 4; // Number of concurrent executions
        ExecutorService pool = Executors.newFixedThreadPool(parallelRuns);
        CountDownLatch latch = new CountDownLatch(parallelRuns);

        for (int i = 0; i < parallelRuns; i++) {
            pool.submit(() -> {
                try {
                    StaffDashboardPageTest testInstance = new StaffDashboardPageTest();
                    testInstance.driver = newChrome(); // Ensure each instance has its own driver
                    testInstance.newMenuUploader();
                } finally {
                    latch.countDown();
                }
            });
        }

        pool.shutdown();
        latch.await();
        System.out.println("All parallel newMenuUploader runs finished.");
    }*/

    @Test (description = "Run newMenuUploader in parallel (per index)")
    public void runNewMenuUploaderInParallelEnhanched() throws InterruptedException {
        ImageUploader imageUploader = new ImageUploader();
        List<String> storeFolders = imageUploader.getImageFolderNames();
        int parallelRuns = storeFolders.size(); // or any number <= storeFolders.size()
        ExecutorService pool = Executors.newFixedThreadPool(parallelRuns);
        CountDownLatch latch = new CountDownLatch(parallelRuns);

        for (int i = 0; i < parallelRuns; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    System.out.println("Thread " + Thread.currentThread().getId() + " starting run for index: " + idx);
                    StaffDashboardPageTest testInstance = new StaffDashboardPageTest();
                    testInstance.driver = newChrome();
                    testInstance.newMenuUploader(idx);
                    System.out.println("Thread " + Thread.currentThread().getId() + " finished run for index: " + idx);
                } finally {
                    latch.countDown();
                }
            });
        }

        pool.shutdown();

        latch.await();
        System.out.println("All parallel newMenuUploader runs finished.");
    }



    // --- THIS is the new parallel test method ---
    @Test(description = "Parallel menu image uploader (per store folder)")
    public void newMenuUploader_parallel() throws InterruptedException {
        ImageUploader imageUploader = new ImageUploader();
        java.util.List<String> storeFolders = imageUploader.getImageFolderNames();
        if (storeFolders == null || storeFolders.isEmpty()) {
            System.out.println("No store folders found. Nothing to upload.");
            return;
        }

        final int workers = Integer.parseInt(System.getProperty("workers", "4"));
        final int perStoreLimit = Integer.parseInt(System.getProperty("perStoreUploads", "10")); // 0 = all images
        System.out.printf("Stores=%d, workers=%d, perStoreUploads=%d%n",
                storeFolders.size(), workers, perStoreLimit);

        // shared work queue (unbounded like your Fast test)
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.addAll(storeFolders);

        CountDownLatch done = new CountDownLatch(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        for (int w = 0; w < workers; w++) {
            pool.submit(() -> {
                WebDriver d = null;
                try {
                    d = newChrome();

                    // --- per-thread login (isolated sessions) ---
                    d.get(getXMLData("staffurl"));
                    StaffDashboardPage page = new StaffDashboardPage(d);
                    page.userNameInput.sendKeys(getXMLData("staffusername"));
                    page.passwordInput.sendKeys(getXMLData("staffpassword"));
                    page.loginBtn.click();

                    WebDriverWait wait = new WebDriverWait(d, Duration.ofSeconds(15));
                    wait.until(ExpectedConditions.visibilityOf(page.storesH1)); // "Stores" header as login OK signal

                    String storeName;
                    while ((storeName = queue.poll()) != null) {
                        String storeXpath = "//span[normalize-space()='" + storeName + "']";
                        String storeSearch = storeName.replace(" ", "+");
                        String filterUrl = getXMLData("baseuploaderUrl")
                                + "?tableFilters[city][value]=" + getXMLData("currentcity")
                                + "&tableSearch=" + storeSearch;

                        try {
                            System.out.printf("[T-%s] >>> %s%n", Thread.currentThread().getId(), storeName);
                            d.get(filterUrl);

                            // sometimes the list needs a short wait
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                            d.findElement(By.xpath(storeXpath)).click();

                            // go to bottom (your existing util if available; otherwise scroll)
                            try { pageBottom(); } catch (Throwable t) { ((org.openqa.selenium.JavascriptExecutor)d).executeScript("window.scrollTo(0, document.body.scrollHeight)"); }

                            java.util.List<String> images = imageUploader.getImagePathsInFolder(storeName);
                            if (images == null || images.isEmpty()) {
                                System.out.printf("[T-%s] %s -> no images, skipping%n", Thread.currentThread().getId(), storeName);
                                continue;
                            }

                            int uploaded = 0;
                            imageUploader.uploadAllAtOnce(driver, page.uploadInput, images);
                            staticWait(300);
                            waitUntilTextChanged(page.uploadBtn, "Save changes");
                            page.uploadBtn.click();
                            System.out.printf("[T-%s] <<< %s uploaded=%d%n", Thread.currentThread().getId(), storeName, uploaded);
                        } catch (Exception e) {
                            System.out.printf("[T-%s] !!! %s skipped: %s%n", Thread.currentThread().getId(), storeName, e.getMessage());
                        }
                    }
                } catch (Exception threadInit) {
                    System.out.println("Thread init/login failed: " + threadInit.getMessage());
                } finally {
                    try { if (d != null) d.quit(); } catch (Exception ignored) {}
                    done.countDown();
                }
            });
        }

        pool.shutdown();
        done.await(); // wait all threads
        System.out.println("All workers finished.");
    }


    // --- paste this helper inside the class (near the bottom is fine) ---
    private WebDriver newChrome() {
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--headless=new");
        opt.addArguments("--disable-gpu");
        opt.addArguments("--no-sandbox");
        opt.addArguments("--disable-dev-shm-usage");
        opt.addArguments("--window-size=1920,1080");
        opt.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        ChromeDriver d = new ChromeDriver(opt);
        d.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
        d.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        d.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        return d;
    }

    @Test
    public void pruneNonMenus() {
        NewImageMenuFilterParallel.processRootOnce();
    }

    @Test(description = "menu image uploader new (per city)")
    public void newMenuUploaderWithCity() {
        int skipCount = 0;
        int uploadCount = 0;

        NewImageUploader imageUploader = new NewImageUploader();
        StaffDashboardPage staffDashboardPage = new StaffDashboardPage(driver);

        // Login
        staffDashboardPage.userNameInput.sendKeys(getXMLData("staffusername"));
        staffDashboardPage.passwordInput.sendKeys(getXMLData("staffpassword"));
        staffDashboardPage.loginBtn.click();
        waitUntilElementClickable(staffDashboardPage.sideMenuStores);

        // Loop over each city
        for (String city : imageUploader.getCityFolderNames()) {
            System.out.println("🏙 Processing city: " + city);

            List<String> storeFolders = imageUploader.getStoreFolderNames(city);
            for (int i = 0; i < storeFolders.size(); i++) {
                    String store = storeFolders.get(i);
                    String storeXpath = "//span[normalize-space()='" + store + "']";
                    String storeSearch = store.replace(" ", "+");

                String filterUrl = getXMLData("baseuploaderUrl")
                        + "?tableFilters[city][value]=" + city
                        + "&tableSearch=" + storeSearch;

                    try {
                        driver.get(filterUrl);

                        try {
                            driver.findElement(By.xpath(storeXpath)).click();
                        } catch (org.openqa.selenium.NoSuchElementException ex) {
                            String fallbackXpath = "(//span[contains(normalize-space(),'" + store + "')])[4]";
                            System.out.println("⚠️ Fallback to: " + fallbackXpath);
                            driver.findElement(By.xpath(fallbackXpath)).click();
                        }

                        pageBottom();
                        List<String> images = imageUploader.getImagePathsInFolder(city, store);
                        staticWait(500);

                        imageUploader.uploadAllAtOnce(driver, staffDashboardPage.uploadInput, images);
                        staticWait(200);
                        waitUntilTextChanged(staffDashboardPage.uploadBtn, "Save changes");

                        staffDashboardPage.uploadBtn.click();
                        System.out.printf("✅ [%s] Store %s uploaded (%d images)%n", city, store, images.size());
                        uploadCount++;

                        int param = (i + 1) % 400;
                        if (param == 0) staticWait(600000);
                        else staticWait(60000);
                    } catch (Exception e) {
                        logSkipped(city, store, e, 0);
                        skipCount++;
                    }
                }
            staticWait(120000);
        }
        System.out.println("🎯 Total stores uploaded: " + uploadCount + " | skipped: " + skipCount);
    }

    /*
    * new uploader with city and run riyadh first
    * start
    * */
    @Test(description = "menu image uploader new (per city)")
    public void newMenuUploaderWithRiyadhFirst() {
        int skipCount = 0;
        int uploadCount = 0;

        NewImageUploader imageUploader = new NewImageUploader();
        StaffDashboardPage staffDashboardPage = new StaffDashboardPage(driver);

        // ✅ Login
        staffDashboardPage.userNameInput.sendKeys(getXMLData("staffusername"));
        staffDashboardPage.passwordInput.sendKeys(getXMLData("staffpassword"));
        staffDashboardPage.loginBtn.click();
        waitUntilElementClickable(staffDashboardPage.sideMenuStores);

        // ✅ Get all cities
        List<String> cities = imageUploader.getCityFolderNames();

        // 🔑 Run Riyadh first if present
        if (cities.contains("Riyadh")) {
            System.out.println("🏙 Processing city (priority): Riyadh");
            int[] counts = processCity("Riyadh", imageUploader, staffDashboardPage);
            uploadCount += counts[0];
            skipCount += counts[1];
        }

        // 🔑 Run all other cities
        for (String city : cities) {
            if ("Riyadh".equalsIgnoreCase(city)) continue; // skip Riyadh (already done)
            System.out.println("🏙 Processing city: " + city);
            int[] counts = processCity(city, imageUploader, staffDashboardPage);
            uploadCount += counts[0];
            skipCount += counts[1];
        }

        // ✅ Final summary
        System.out.println("🎯 Total stores uploaded: " + uploadCount + " | skipped: " + skipCount);
    }

    private int[] processCity(String city, NewImageUploader imageUploader, StaffDashboardPage staffDashboardPage) {
        int uploadCount = 0;
        int skipCount = 0;

        List<String> storeFolders = imageUploader.getStoreFolderNames(city);

        for (int i = 0; i < storeFolders.size(); i++) {
            String store = storeFolders.get(i);
            String storeXpath = "//span[normalize-space()='" + store + "']";
            String storeSearch = store.replace(" ", "+");

            try {
                String filterUrl = getXMLData("baseuploaderUrl")
                        + "?tableFilters[city][value]=" + city
                        + "&tableSearch=" + storeSearch;
                driver.get(filterUrl);

                try {
                    driver.findElement(By.xpath(storeXpath)).click();
                } catch (org.openqa.selenium.NoSuchElementException ex) {
                    String fallbackXpath = "(//span[contains(normalize-space(),'" + store + "')])[4]";
                    System.out.println("⚠️ Fallback to: " + fallbackXpath);
                    driver.findElement(By.xpath(fallbackXpath)).click();
                }

                pageBottom();
                List<String> images = imageUploader.getImagePathsInFolder(city, store);
                staticWait(500);

                imageUploader.uploadAllAtOnce(driver, staffDashboardPage.uploadInput, images);
                staticWait(200);
                waitUntilTextChanged(staffDashboardPage.uploadBtn, "Save changes");

                staffDashboardPage.uploadBtn.click();
                System.out.printf("✅ [%s] Store %s uploaded (%d images)%n", city, store, images.size());
                uploadCount++;

                // pacing
                int param = (i + 1) % 400;
                if (param == 0) staticWait(100000);
                else staticWait(60000);

            } catch (Exception e) {
//                logSkipped(city, store, e, 0);
                CSVLogger.logSkipped(city, store, e, 0);
                skipCount++;
            }
        }

        return new int[]{uploadCount, skipCount};
    }

    /*
     * new uploader with city and run riyadh first
     * end
     * */
}
