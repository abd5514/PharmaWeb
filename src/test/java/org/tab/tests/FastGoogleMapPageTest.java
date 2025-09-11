package org.tab.tests;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.tab.data.FastJSONReader;
import org.tab.data.JSONReader;
import org.tab.data.FastJSONReader.Item;
import org.tab.web_pages.FastGoogleMapPage;
import org.tab.web_pages.GoogleMapPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FastGoogleMapPageTest {

    @Test(description = "FAST parallel JSON reader + image downloader")
    public void jsonReader_parallel() throws IOException, InterruptedException {
        // Load items (googleMapsUri + displayName.text). File path comes from -DJSONFilePath or PropReader.
        FastJSONReader r = new FastJSONReader();
        List<Item> items = r.toItems();

        // Sanity check
        Assert.assertTrue(items != null && !items.isEmpty(), "No places found in JSON.");

        // Tunables
        int workers = Integer.parseInt(System.getProperty("workers", "6"));               // parallel WebDrivers
        int perPlaceDownloads = Integer.parseInt(System.getProperty("perPlaceDownloads", "4")); // per-place parallel downloads

        System.out.printf("Items=%d, workers=%d, perPlaceDownloads=%d%n", items.size(), workers, perPlaceDownloads);

        // Use an unbounded queue to avoid IllegalStateException: Queue full
        BlockingQueue<Item> queue = new LinkedBlockingQueue<>();
        queue.addAll(items);

        CountDownLatch done = new CountDownLatch(workers);
        List<Thread> threads = new ArrayList<>(workers);

        for (int w = 0; w < workers; w++) {
            Thread t = new Thread(() -> {
                WebDriver driver = null;
                try {
                    driver = newChrome();

                    // Try to switch to English once (if the link exists)
                    try {
                        driver.get("https://www.google.com");
                        FastGoogleMapPage pageTmp = new FastGoogleMapPage(driver);
                        try { pageTmp.enBtn.click(); } catch (Exception ignored) {}
                    } catch (Exception ignored) {}

                    FastGoogleMapPage page = new FastGoogleMapPage(driver);

                    while (true) {
                        Item it = queue.poll(200, TimeUnit.MILLISECONDS);
                        if (it == null) break; // no more work
                        try {
                            page.processPlace(driver, it.url, it.name, it.index, perPlaceDownloads);
                        } catch (Throwable ex) {
                            FastGoogleMapPage.saveFailedDownload("worker-failed", it.name, new RuntimeException(ex), it.index);
                        }
                    }
                } catch (Throwable boot) {
                    System.out.println("Worker failed to start: " + boot.getMessage());
                } finally {
                    if (driver != null) {
                        try { driver.quit(); } catch (Exception ignored) {}
                    }
                    done.countDown();
                }
            }, "worker-" + (w + 1));
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }

        // Wait for all workers to finish
        done.await();
    }

    /* ------------ Local, fast, headless Chrome (independent per worker) ------------ */

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
}