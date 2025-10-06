package org.tab.web_pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.tab.utils.PropReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class FastGoogleMapPage {

    @FindBy(xpath = "//div[normalize-space()='Menu'][1]")
    public WebElement menuBtn;
    @FindBy(xpath = "//div[@class='cRLbXd']//div[contains(@class,'dryRY')]")
    public WebElement imageContainer;
    @FindBy(xpath = "//a[normalize-space()='English']")
    public WebElement enBtn;
    @FindBy(xpath = "//button[contains(@class,'Tc0rEd XMkGfe cPtXLb')]")
    public WebElement nextBtn;

    public FastGoogleMapPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    /* ========================= Public API ========================= */

    /**
     * Open a place URL, expand its gallery, collect image URLs, and download them concurrently.
     *
     * @param driver   thread-local WebDriver
     * @param placeUrl googleMapsUri
     * @param storeName displayName.text (folder-safe name will be derived)
     * @param loopId   index in the JSON array (for logging)
     * @param perPlaceConcurrency number of parallel downloads per place (e.g., 6)
     */
    public void processPlace(WebDriver driver,
                             String placeUrl,
                             String storeName,
                             int loopId,
                             int perPlaceConcurrency) {

        driver.get(placeUrl);

        // Try to open the "Menu" tab if present (your current behavior). :contentReference[oaicite:5]{index=5}
        try {
            safeClick(menuBtn, 2);
        } catch (Exception e) {
            saveFailedDownload("no menu tab found", storeName, e, loopId);
            return;
        }

        // Scroll/advance the gallery several times to force-load thumbnails (your locator set). :contentReference[oaicite:6]{index=6}
        for (int i = 0; i < 7; i++) {
            try {
                if (isDisplayed(nextBtn)) {
                    for (int j = 0; j < 40; j++) {
                        try { nextBtn.click(); Thread.sleep(5); } catch (Exception ignore) {}
                    }
                } else break;
            } catch (Exception ignore) {}
        }

        // Collect thumbnail img nodes
        List<WebElement> thumbs;
        try {
            thumbs = driver.findElements(By.xpath("//div[@class='cRLbXd']//div[contains(@class,'dryRY')]//img[@class='DaSXdd']"));
        } catch (Exception e) {
            saveFailedDownload("no images container found", storeName, e, loopId);
            return;
        }

        if (thumbs == null || thumbs.isEmpty()) {
            saveFailedDownload("no images found under menu tab", storeName, null, loopId);
            return;
        }

        // Extract hi-res URLs first (avoid serial IO while we still have the page open)
        List<String> urls = new ArrayList<>(thumbs.size());
        for (WebElement img : thumbs) {
            try {
                String src = img.getAttribute("src");
                if (src != null && src.contains("lh3.googleusercontent.com")) {
                    // Prefer higher resolution
                    String high = W_H_PATTERN.matcher(src).replaceAll("w6000");
                    urls.add(high);
                }
            } catch (Exception ignore) {}
        }

        if (urls.isEmpty()) {
            saveFailedDownload("no usable image URLs", storeName, null, loopId);
            return;
        }

        // Prepare target dir per store (clean once per run)
        File dir = new File("src/test/resources/images/" +getCityName()+ "/" + sanitizeForWindows(storeName));
        if (!dir.exists() && !dir.mkdirs()) {
            saveFailedDownload("cannot create dir", dir.getAbsolutePath(), null, loopId);
            return;
        }
        /*
        String basePath = "src/test/resources/images/" + sanitizeForWindows(storeName);
        File dir = new File(basePath);
        // 🔄 If already exists, append timestamp
        if (dir.exists()) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            dir = new File(basePath + "_" + timestamp);
        }
        // ✅ Create the directory
        if (!dir.mkdirs()) {
            saveFailedDownload("cannot create dir", dir.getAbsolutePath(), null, loopId);
            return;
        }*/

        // Download concurrently (bounded)
        int parallel = Math.max(1, perPlaceConcurrency);
        ExecutorService pool = Executors.newFixedThreadPool(parallel, r -> {
            Thread t = new Thread(r, "img-dl-" + storeName);
            t.setDaemon(true);
            return t;
        });

        List<Future<?>> futures = new ArrayList<>(urls.size());
        final int[] idx = {1};
        for (String u : urls) {
            final int myIdx = idx[0]++;
            futures.add(pool.submit(() -> {
                String file = new File(dir, "image_" + sanitizeForWindows(storeName) + "_" + myIdx + ".png").getPath();
                try {
                    downloadImage(u, file);
                } catch (Exception e) {
                    saveFailedDownload(u, file, e, loopId);
                }
            }));
        }

        // Wait for completion
        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException ignored) {}
    }

    /* ========================= Helpers ========================= */

    private boolean isDisplayed(WebElement el) {
        try { return el != null && el.isDisplayed(); } catch (Exception e) { return false; }
    }

    private void safeClick(WebElement el, int retries) throws Exception {
        if (el == null) throw new Exception("element is null");
        for (int i = 0; i <= retries; i++) {
            try { el.click(); return; } catch (Exception e) {
                if (i == retries) throw e;
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static final Pattern W_H_PATTERN = Pattern.compile("w\\d+-h\\d+-p");

    private static String sanitizeForWindows(String name) {
        // Keep same spirit as your current code (safe file/folder names). :contentReference[oaicite:7]{index=7}
        return name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    /* ---------- Robust download with redirect/timeout handling ---------- */

    private void downloadImage(String imageUrl, String filePath) throws IOException {
        File target = new File(filePath);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(imageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = conn.getHeaderField("Location");
                if (loc != null && !loc.isEmpty()) {
                    conn.disconnect();
                    URL redir = new URL(url, loc);
                    conn = (HttpURLConnection) redir.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    code = conn.getResponseCode();
                }
            }

            if (code < 200 || code >= 300)
                throw new IOException("HTTP " + code + " for " + imageUrl);

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int r; long total = 0;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                    total += r;
                }
                if (total == 0) throw new IOException("Zero bytes downloaded.");
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    /* ---------- Thread-safe CSV logger (appends) ---------- */
    private static final String run_Id = String.valueOf(System.currentTimeMillis());
    private static final File log_File = new File("src/test/resources/failed_downloads_" + run_Id + ".csv");
    public static synchronized void saveFailedDownload(String imageUrl, String filePath, Exception e, int loopId) {

        try {
            File parent = log_File.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            boolean newFile = !log_File.exists() || log_File.length() == 0;

            try (FileOutputStream fos = new FileOutputStream(log_File, true)) {
                if (newFile) fos.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF}); // BOM for Excel
                try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     PrintWriter pw = new PrintWriter(osw)) {
                    String timestamp = LocalDateTime.now().toString();
                    String errorMsg = (e == null || e.getMessage() == null) ? "" : e.getMessage().replaceAll("[\\r\\n]", " ");
                    // loopId, timestamp, image/urlOrReason, filePathOrStore, errorMsg
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            loopId, timestamp, safeCsv(imageUrl), safeCsv(filePath), safeCsv(errorMsg));
                }
            }
        } catch (IOException io) {
            System.out.println("⚠️ Failed to log download error: " + io.getMessage());
        }
    }

    private static String safeCsv(String s) { return s == null ? "" : s.replace("\"","'"); }

    private static String getCityName() {
        String path = PropReader.get("JSONFilePath","src/test/resources/Riyadh_details.json"); // fallback
        String filename = path.contains("/") ?
                path.substring(path.lastIndexOf("/") + 1) :
                path.contains("\\") ?
                        path.substring(path.lastIndexOf("\\") + 1) :
                        path; // fallback if just filename
        return filename.split("_")[0];
    }

    /*private static String getCityName() {
        String path = System.getProperty("JSONFilePath"); // must be set by you at runtime

        // cross-platform: get filename from path
        String filename = new File(path).getName(); // e.g., "Jeddah_details.json"

        // remove extension
        int dot = filename.lastIndexOf('.');
        if (dot > 0) filename = filename.substring(0, dot); // "Jeddah_details"

        // split on underscore, dash, or whitespace and take first token
        String[] tokens = filename.split("[_\\-\\s]+");
        return tokens.length > 0 ? tokens[0] : filename;
    }*/

}
