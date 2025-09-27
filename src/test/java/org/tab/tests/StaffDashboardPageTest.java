package org.tab.tests;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.tab.base.Base;
import org.tab.data.ImageUploader;
import org.tab.data.NewImageUploader;
import org.tab.utils.CSVLogger;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.StaffDashboardPage;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// --- add these imports at the top of StaffDashboardPageTest.java ---
import org.openqa.selenium.WebDriver;

import java.util.concurrent.*;


import static org.tab.data.TestDataReader.getXMLData;
import static org.tab.utils.CSVLogger.logSkipped;
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
                } catch (NoSuchElementException ex) {
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
            /*staticWait(300000); // 5 min break between cities*/
        }

        // ✅ Final summary
        System.out.println("🎯 Total stores uploaded: " + uploadCount + " | skipped: " + skipCount);
    }

    private int[] processCity(String city, NewImageUploader imageUploader, StaffDashboardPage staffDashboardPage) {
        int uploadCount = 0;
        int skipCount = 0;
        int j=0;
        List<String> storeFolders = imageUploader.getStoreFolderNames(city);
        for (int i = j; i <storeFolders.size(); i++) {
            String store = storeFolders.get(i);
            String storeXpath = /*"//span[normalize-space()='" + store + "']";*/"//div[@data-store='id_"+store+"']//span";
            //div[@data-store='id_نمق كافيه | Namq Cafe']
            //div[@data-store='product_AteeqTea']//span
            String storeSearch = store.replace(" ", "+");
            List<String> images = imageUploader.getImagePathsInFolder(city, store);
            if (images.isEmpty()) {
                logSkipped(city, store, null, 0);
                skipCount++;
                continue;
            }
            else if(images.size()>30){
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
                else {waitTime =images.size()*700;}
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
                System.out.println("current loop  " + i + " store   " + store + " uploaded");
            } catch (Exception e) {
                logSkipped(city, store, e, 0);
                System.out.println("current loop  " + i + " store   " + store + " skipped");
                skipCount++;
            }
        }
        return new int[]{uploadCount, skipCount};
    }

    /**
     * Dynamic wait until product count > 0 for the given store row.
     */
    private int waitForProductsAboveZero(WebDriver driver, String storeXpath,int imagesCount,String url) {
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

    /*
     * new uploader with city and run riyadh first
     * end
     * */

    /*@Test(description = "menu image uploader new (per city)")
    public void uploadChecker() {
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
            int[] counts = cityChecker("Riyadh", imageUploader, staffDashboardPage);
            uploadCount += counts[0];
            skipCount += counts[1];
        }

        // 🔑 Run all other cities
        for (String city : cities) {
            if ("Riyadh".equalsIgnoreCase(city)) continue; // skip Riyadh (already done)
            System.out.println("🏙 Processing city: " + city);
            int[] counts = cityChecker(city, imageUploader, staffDashboardPage);
            uploadCount += counts[0];
            skipCount += counts[1];
        }

        // ✅ Final summary
        System.out.println("🎯 Total stores uploaded: " + uploadCount + " | skipped: " + skipCount);
    }

    private int[] cityChecker(String city, NewImageUploader imageUploader, StaffDashboardPage staffDashboardPage) {
        int uploadCount = 0;
        int skipCount = 0;
        int j=0;
        List<String> storeFolders = imageUploader.getStoreFolderNames(city);
        for (int i = j; i < storeFolders.size(); i++) {
            String store = storeFolders.get(i);
            String storeXpath = "//span[normalize-space()='" + store + "']";
            String storeSearch = store.replace(" ", "+");

            try {
                String filterUrl = getXMLData("baseuploaderUrl")
                        + "?tableFilters[city][value]=" + city
                        + "&tableSearch=" + storeSearch;
                driver.get(filterUrl);
                staticWait(500);
                int products;
                try {
                    products=Integer.parseInt(
                            driver.findElement(By.xpath(storeXpath + "/ancestor::tr//td[12]//span")).getText());
                } catch (org.openqa.selenium.NoSuchElementException ex) {
                    String fallbackXpath = "(//span[contains(normalize-space(),'" + store + "')])[4]/ancestor::tr//td[12]//span";
                    products=Integer.parseInt(
                            driver.findElement(By.xpath(fallbackXpath)).getText());
                }

                System.out.println("📦 Products checker [" + store + "]: " + products);
                List<String> images = imageUploader.getImagePathsInFolder(city, store);
                staticWait(500);
                if (images.isEmpty()) {
                    CSVLogger.logSkipped(city, store, null, 0);
                    skipCount++;
                    continue;
                }
                if (products <= 0) {
                    CSVLogger.logSkipped(city, store, null, images.size());
                    skipCount++;
                }

            } catch (Exception e) {
                CSVLogger.logSkipped(city, store, e, 0);
                skipCount++;
            }
        }
        return new int[]{uploadCount, skipCount};
    }


    *//*
    * city and stores loop Parallel start
    * */
    @Test(description = "Parallel menu image uploader checker (cities + stores)")
    public void testUploadCheckerParallel() {
        uploadCheckerParallel();
    }
    // ✅ Main method: runs cities in parallel
    public void uploadCheckerParallel() {
        int totalUpload = 0;
        int totalSkip = 0;

        NewImageUploader imageUploader = new NewImageUploader();

        // ✅ Get all cities
        List<String> cities = imageUploader.getCityFolderNames();

        // 🔑 Prioritize Riyadh first

        // ✅ Run remaining cities in parallel
        ExecutorService cityExecutor = Executors.newFixedThreadPool(3); // adjust #threads for cities
        List<Future<int[]>> cityFutures = new CopyOnWriteArrayList<>();

        for (String city : cities) {
            cityFutures.add(cityExecutor.submit(() -> {
                System.out.println("🏙 Processing city: " + city);
                return cityCheckerNew(city, imageUploader); // already parallel per store
            }));
        }

        cityExecutor.shutdown();

        try {
            for (Future<int[]> f : cityFutures) {
                int[] result = f.get();
                totalUpload += result[0];
                totalSkip += result[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ✅ Final summary
        System.out.println("🎯 Total stores uploaded: " + totalUpload + " | skipped: " + totalSkip);
    }
    // ✅ City checker with parallel stores
    private int[] cityCheckerNew(String city, NewImageUploader imageUploader) {
        int uploadCount = 0;
        int skipCount = 0;

        List<String> storeFolders = imageUploader.getStoreFolderNames(city);

        ExecutorService executor = Executors.newFixedThreadPool(4); // adjust threads per city
        List<Future<int[]>> futures = new CopyOnWriteArrayList<>();

        for (String store : storeFolders) {
            futures.add(executor.submit(() -> {
                WebDriver localDriver = createDriver();
                StaffDashboardPage staffDashboardPage = new StaffDashboardPage(localDriver);
                int localUpload = 0;
                int localSkip = 0;

                try {
                    String storeXpath = "//div[@data-store='id_" + store + "']";
                    //div[@data-store='id_AteeqTea']
                    //div[@data-store='product_AteeqTea']//span
                    String storeSearch = store.replace(" ", "+");

                    String filterUrl = getXMLData("baseuploaderUrl")
                            + "?tableFilters[city][value]=" + city
                            + "&tableSearch=" + storeSearch;

                    localDriver.get(filterUrl);
                    staticWait(500);

                    int products = Integer.parseInt(
                                localDriver.findElement(By.xpath("//div[@data-store='product_"+store+"']//span")).getText());
                    System.out.println("📦 Products checker [" + store + "]: " + products);
                    List<String> images = imageUploader.getImagePathsInFolder(city, store);
                    staticWait(500);

                    if (images.isEmpty()) {
                        logSkipped(city, store, null, 0);
                        System.out.println("⚠️ No images found for store: " + store + " in city: " + city);
                        localSkip++;
                    } else if (products <= 0) {
                        logSkipped(city, store, null, images.size());
                        System.out.println("⚠️ Store with 0 products: " + store + " in city: " + city + " (images: " + images.size() + ")");
                        localSkip++;
                    } else {
                        localUpload++;
                    }
                } catch (Exception e) {
                    logSkipped(city, store, e, 0);
                    localSkip++;
                } finally {
                    localDriver.quit();
                }

                return new int[]{localUpload, localSkip};
            }));
        }

        executor.shutdown();
        try {
            for (Future<int[]> f : futures) {
                int[] result = f.get();
                uploadCount += result[0];
                skipCount += result[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new int[]{uploadCount, skipCount};
    }
    private static WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");   // modern headless mode
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        return driver;
    }

    /*
     * city and stores loop Parallel end
     * */



    /*
    * check uploader stores start
    * */
    @Test(description = "check upload status with logging not uploaded correctly")
    public void checkImageUpload() {
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
            int[] counts = processCityUpload("Riyadh", imageUploader, staffDashboardPage);
            uploadCount += counts[0];
            skipCount += counts[1];
        }

        // 🔑 Run all other cities
        for (String city : cities) {
            if ("Riyadh".equalsIgnoreCase(city)) continue; // skip Riyadh (already done)
            System.out.println("🏙 Processing city: " + city);
            int[] counts = processCityUpload(city, imageUploader, staffDashboardPage);
            uploadCount += counts[0];
            skipCount += counts[1];
            /*staticWait(300000); // 5 min break between cities*/
        }

        // ✅ Final summary
        System.out.println("🎯 Total stores uploaded: " + uploadCount + " | skipped: " + skipCount);
    }

    private int[] processCityUpload(String city, NewImageUploader imageUploader, StaffDashboardPage staffDashboardPage) {
        int uploadCount = 0;
        int skipCount = 0;
        int j=0;
        List<String> storeFolders = imageUploader.getStoreFolderNames(city);
        for (int i = j; i < storeFolders.size(); i++) {
            String store = storeFolders.get(i);
            String storeXpath = "//div[@data-store='id_"+store+"']";
            int products;
            String storeSearch = store.replace(" ", "+");
            List<String> images = imageUploader.getImagePathsInFolder(city, store);
            if (images.isEmpty() || images.size()>30) {
                skipCount++;
                continue;
            }
            try {
                String filterUrl = getXMLData("baseuploaderUrl")
                        + "?tableFilters[city][value]=" + city
                        + "&tableSearch=" + storeSearch;
                driver.get(filterUrl);
                staticWait(100);
                try {
                    products=
                            Integer.parseInt(driver.findElement(By.xpath("//div[@data-store='product_"+store+"']//span")).getText());
                } catch (NoSuchElementException ex) {
                    try {
                        products = Integer.parseInt(driver.findElement(By.xpath("//div[contains(@data-store,'product_" + store + "')]//span")).getText());
                    } catch (NoSuchElementException e) {
                        try {
                            products = Integer.parseInt(driver.findElement(By.xpath(storeXpath + "//ancestor::tr//td[13]//span")).getText());
                        }catch (NoSuchElementException es){
                            products = Integer.parseInt(driver.findElement(By.xpath("//div[contains(@data-store,'id_" + store + "')]//ancestor::tr//td[13]//span")).getText());
                        }
                    }
                }
                if(products>0){continue;}
                try {
                    driver.findElement(By.xpath(storeXpath)).click();
                } catch (NoSuchElementException ex) {
                    storeXpath = "//div[contains(@data-store,'id_" + store + "')]";
                    System.out.println("⚠️ Fallback to: " + storeXpath);
                    driver.findElement(By.xpath(storeXpath)).click();
                }
                pageBottom();
                waitUntilElementClickable(staffDashboardPage.sideMenuStores);
                int waitTime;
                if(images.size()<2){
                    waitTime=images.size()*2000;
                }else {waitTime=images.size()*1000;}
                staticWait(waitTime);
                staffDashboardPage.assertDisplayedAndLog(staffDashboardPage.imageContainer, city, store, images.size());
                uploadCount++;
            } catch (Exception e) {
                if(e.getMessage().startsWith(
                        "no such element: Unable to locate element: {\"method\":\"xpath\",\"selector\":\"//div[@class='filepond--image-preview-wrapper']\"}")){
                    logSkipped(city, store, e, 0,"check_upload");
                }
                skipCount++;
            }
        }
        return new int[]{uploadCount, skipCount};
    }

    /*
     * check uploader stores end
     * */
}
