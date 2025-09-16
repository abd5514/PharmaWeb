package org.tab.tests;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.tab.data.FastJSONReader;
import org.tab.data.FastJSONReader.Item;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.FastGoogleMapPage;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Listeners(ExtentTestListener.class)
public class FastGoogleMapPageTest {

    @Test(description = "FAST parallel JSON reader + image downloader")
    public void jsonReader_parallel() throws IOException, InterruptedException {
        // Load items (googleMapsUri + displayName.text). File path comes from -DJSONFilePath or PropReader.
        FastJSONReader r = new FastJSONReader();
        List<Item> items = r.toItems();

        // Sanity check
        Assert.assertTrue(items != null && !items.isEmpty(), "No places found in JSON.");
        /*
        * failed worker
        *
        * */
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

    @Test
    public void jsonReader_parallelWithSpacificIndexs() throws IOException, InterruptedException {
        // Load items (googleMapsUri + displayName.text). File path comes from -DJSONFilePath or PropReader.
        FastJSONReader r = new FastJSONReader();
        List<Item> items = r.toItems();

        // Sanity check
        Assert.assertTrue(items != null && !items.isEmpty(), "No places found in JSON.");

        // Tunables
        int workers = Integer.parseInt(System.getProperty("workers", "6"));               // parallel WebDrivers
        int perPlaceDownloads = Integer.parseInt(System.getProperty("perPlaceDownloads", "4")); // per-place parallel downloads

        System.out.printf("Items=%d, workers=%d, perPlaceDownloads=%d%n", items.size(), workers, perPlaceDownloads);

        // ✅ Define your rows array
        int[] rows = {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                14, 15, 144, 146, 175, 183, 186, 187, 204, 257, 258, 259, 264, 265, 272, 281,
                1035, 1036, 1037, 1039, 1040, 1041, 1042, 1043, 1044, 1068, 1074, 1527,
                2049, 2051, 2056, 2057, 2058, 2059, 2060, 2061, 2062, 2063, 2064, 2065,
                2066, 2067, 2068, 2069, 2070, 2071, 2072, 2073, 2074, 2075, 2076, 2077,
                2078, 2079, 2080, 2081, 2082, 2083, 2084, 2085, 2086, 2087, 2088, 2089,
                2090, 2091, 2092, 2093, 2094, 2095, 2096, 2097, 2098, 2099, 2100, 2101,
                2102, 2103, 2104, 2105, 2106, 2107, 2108, 2109, 2110, 2111, 2112, 2113,
                2114, 2115, 2116, 2117, 2118, 2119, 2120, 2121, 2122, 2123, 2124, 2125,
                2126, 2127, 2128, 2129, 2130, 2131, 2132, 2133, 2134, 2135, 2136, 2137,
                2138, 2139, 2140, 2141, 2142, 2143, 2144, 2145, 2146, 2147, 2148, 2149,
                2150, 2151, 2152, 2153, 2154, 2155, 2156, 2157, 2158, 2159, 2160, 2161,
                2162, 2163, 2164, 2165, 2166, 2167, 2168, 2169, 2170, 2171, 2172, 2173,
                2174, 2175, 2176, 2177, 2178, 2179, 2180, 2181, 2182, 2183, 2184, 2185,
                2186, 2187, 2188, 2189, 2190, 2191, 2192, 2193, 2194, 2195, 2196, 2197,
                2198, 2199, 2200, 2201, 2202, 2203, 2204, 2205, 2206, 2207, 2208, 2209,
                2210, 2211, 2212, 2213, 2214, 2215, 2216, 2217, 2218, 2219, 2220, 2221,
                2222, 2223, 2224, 2225, 2226, 2227, 2228, 2229, 2230, 2231, 2232, 2233,
                2234, 2235, 2236, 2237, 2238, 2239, 2240, 2241, 2242, 2243, 2244, 2245,
                2246, 2247, 2248, 2249, 2250, 2251, 2252, 2253, 2254, 2255, 2256, 2257,
                2258, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266, 2267, 2268, 2269,
                2270, 2271, 2272, 2273, 2274, 2275, 2276, 2277, 2278, 2279, 2280, 2281,
                2282, 2283, 2284, 2285, 2287, 2286, 2288, 2289, 2290, 2291, 2292, 2293,
                2294, 2295, 2296, 2297, 2298, 2299, 2301, 2300, 2302, 2303, 2304, 2305,
                2306, 2307, 2308, 2309, 2310, 2311, 2312, 2313, 2314, 2315, 2316, 2317,
                2318, 2320, 2319, 2321, 2322, 2323, 2324, 2325, 2326, 2327, 2328, 2329,
                2330, 2332, 2331, 2333, 2334, 2335, 2336, 2337, 2338, 2339, 2340, 2341,
                2343, 2342, 2344, 2345, 2346, 2348, 2349, 2350, 2351, 2347, 2352, 2353,
                2354, 2355, 2356, 2357, 2358, 2359, 2360, 2361, 2362, 2363, 2364, 2365,
                2366, 2367, 2368, 2369, 2370, 2371, 2372, 2373, 2374, 2375, 2376, 2377,
                2378, 2379, 2380, 2381, 2382, 2383, 2384, 2385, 2386, 2388, 2387, 2389,
                2390, 2391, 2392, 2393, 2394, 2395, 2397, 2398, 2396, 2399,
                2054, 2055
        };

        Set<Integer> allowedIndexes = Arrays.stream(rows).boxed().collect(Collectors.toSet());

        // ✅ Filter only items whose index is in rows
        BlockingQueue<Item> queue = new LinkedBlockingQueue<>();
        for (Item it : items) {
            if (allowedIndexes.contains(it.index)) {
                queue.add(it);
            }
        }

        System.out.printf("Filtered items=%d (from %d)%n", queue.size(), items.size());

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

}