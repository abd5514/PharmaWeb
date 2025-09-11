package org.tab.data;

import org.tab.utils.PropReader;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Retries downloads listed in src/test/resources/images/failed_downloads.csv.
 * CSV columns (no header): "timestamp","imageUrl","filePath","error"
 */
public class RetryFailedDownloads {

    private static final String FAILED_CSV = PropReader.get("csvFilePath", "src/test/resources/failed_downloads.csv");
    private static final String BACKUP_CSV = "src/test/resources/failed_downloads.csv.bak";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void main(String[] args) {
        // Run from CLI or call retryAll() from your tests
        retryAllFast();
    }

    public static void retryAll() {
        File csv = new File(FAILED_CSV);
        if (!csv.exists()) {
            System.out.println("✅ No failed downloads file found. Nothing to retry.");
            return;
        }

        List<String[]> rows = readCsv(csv);
        if (rows.isEmpty()) {
            System.out.println("✅ failed_downloads.csv is empty. Nothing to retry.");
            return;
        }

        int total = rows.size();
        int success = 0;
        List<String[]> stillFailed = new ArrayList<>();

        for (String[] cols : rows) {
            // Expect 4 columns: id, timestamp, url, path, error
            if (cols.length < 3) continue;
            String imageUrl = safeGet(cols, 2);
            String filePath = safeGet(cols, 3);

            try {
                ensureParentDir(filePath);
                downloadImage(imageUrl, filePath);
                success++;
                System.out.println("⬇️  Retried OK: " + imageUrl + " -> " + filePath);
            } catch (Exception e) {
                String msg = (e.getMessage() == null) ? "" : e.getMessage().replaceAll("[\\r\\n]", " ");
                stillFailed.add(new String[] {
                        LocalDateTime.now().toString(),
                        imageUrl,
                        filePath,
                        msg
                });
                System.out.println("❌ Retry failed: " + imageUrl + " -> " + filePath + " | " + msg);
            }
        }

        // Backup original then rewrite with remaining failures
        backupFile(csv, new File(BACKUP_CSV));
        writeCsv(csv, stillFailed);

        System.out.printf("Finished. Retried: %d, Success: %d, Still failed: %d%n",
                total, success, stillFailed.size());
        if (stillFailed.isEmpty()) {
            System.out.println("🎉 All failed downloads were recovered!");
        } else {
            System.out.println("⚠️ Some entries still failed. See: " + FAILED_CSV);
        }
    }

    public static void retryAllFast() {
        File csv = new File(FAILED_CSV);
        if (!csv.exists()) {
            System.out.println("✅ No failed downloads file found. Nothing to retry.");
            return;
        }

        List<String[]> rows = readCsv(csv);
        if (rows.isEmpty()) {
            System.out.println("✅ failed_downloads.csv is empty. Nothing to retry.");
            return;
        }

        // Optional: skip header if present (expects “timestamp,imageUrl,filePath,error” wording)
        if (looksLikeHeader(rows.get(0))) {
            System.out.println("ℹ️ Detected header row, skipping it.");
            rows.remove(0);
        }

        final int COL_URL  = 2; // your current CSV layout
        final int COL_PATH = 3;

        // Thread pool: CPU*4 capped at 32 (tweak if needed)
        final int threads = Math.min(32, Math.max(4, Runtime.getRuntime().availableProcessors() * 4));
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<String[]> stillFailed = new CopyOnWriteArrayList<>();
        AtomicInteger success = new AtomicInteger(0);
        long t0 = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>(rows.size());
        for (String[] cols : rows) {
            futures.add(pool.submit(() -> {
                try {
                    if (cols == null || cols.length <= Math.max(COL_URL, COL_PATH)) {
                        stillFailed.add(new String[] { LocalDateTime.now().toString(), "", "", "Row too short" });
                        return;
                    }

                    String imageUrl = safeGet(cols, COL_URL).trim();
                    String filePath = safeGet(cols, COL_PATH).trim();

                    if (imageUrl.isEmpty() || (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://"))) {
                        stillFailed.add(new String[] { LocalDateTime.now().toString(), imageUrl, filePath, "Invalid URL" });
                        return;
                    }
                    if (filePath.isEmpty()) {
                        stillFailed.add(new String[] { LocalDateTime.now().toString(), imageUrl, filePath, "Empty path" });
                        return;
                    }

                    // Skip if already downloaded with content
                    File target = new File(filePath);
                    if (target.exists() && target.length() > 0) {
                        success.incrementAndGet();
                        return;
                    }

                    ensureParentDir(filePath);
                    downloadImageFast(imageUrl, Path.of(filePath)); // uses your robust 2-strategy method
                    success.incrementAndGet();
                    System.out.println("⬇️  Retried OK: " + imageUrl + " -> " + filePath);
                } catch (Exception e) {
                    String msg = (e.getMessage() == null) ? "" : e.getMessage().replaceAll("[\\r\\n]", " ");
                    stillFailed.add(new String[] { LocalDateTime.now().toString(), safeGet(cols, COL_URL), safeGet(cols, COL_PATH), msg });
                    System.out.println("❌ Retry failed: " + safeGet(cols, COL_URL) + " -> " + safeGet(cols, COL_PATH) + " | " + msg);
                }
            }));
        }

        pool.shutdown();
        try { pool.awaitTermination(30, TimeUnit.MINUTES); } catch (InterruptedException ignored) {}

        // Backup original then rewrite with remaining failures
        backupFile(csv, new File(BACKUP_CSV));
        writeCsv(csv, stillFailed);

        long t1 = System.currentTimeMillis();
        int total = rows.size();
        System.out.printf("Finished. Retried: %d, Success: %d, Still failed: %d, Threads: %d, Time: %.2fs%n",
                total, success.get(), stillFailed.size(), threads, (t1 - t0) / 1000.0);

        if (stillFailed.isEmpty()) {
            System.out.println("🎉 All failed downloads were recovered!");
        } else {
            System.out.println("⚠️ Some entries still failed. See: " + FAILED_CSV);
        }
    }


    // --------- Helpers ---------

    private static boolean looksLikeHeader(String[] cols) {
        if (cols == null || cols.length < 4) return false;
        String a = cols[0].toLowerCase();
        String b = cols[1].toLowerCase();
        String c = cols[2].toLowerCase();
        String d = cols[3].toLowerCase();
        return (a.contains("time") || a.contains("timestamp")) &&
                (b.contains("url")) &&
                (c.contains("path") || c.contains("file")) &&
                (d.contains("err"));
    }


    /*private static void downloadImage(String imageUrl, String filePath) throws IOException {
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(new File(filePath))) {

            byte[] buffer = new byte[8192];
            int r;
            while ((r = in.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
        }
    }*/

    // Add at top if missing:
// import java.net.HttpURLConnection;

    private static void downloadImage(String imageUrl, String filePath) throws IOException {
        File target = new File(filePath);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        IOException firstFailure = null;

        // ── Strategy 1: Legacy approach ─────────────────────────
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(target, false)) {
            byte[] buf = new byte[8192];
            int r; long total = 0;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                total += r;
            }
            if (total == 0) throw new IOException("Zero bytes downloaded (legacy).");
            return; // success → stops here, no duplicate
        } catch (IOException ex) {
            firstFailure = ex;
            // Cleanup ANY partial (zero or non-zero) so Strategy 2 starts clean
            try { if (target.exists()) target.delete(); } catch (Exception ignore) {}
        }

        // ── Strategy 2: Robust HTTP with status & timeouts ──────
        HttpURLConnection conn = null;
        try {
            URL url = new URL(imageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = conn.getResponseCode();

            // Handle 3xx redirect manually once
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

            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + imageUrl);
            }

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(target, false)) {
                byte[] buf = new byte[8192];
                int r; long total = 0;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                    total += r;
                }
                if (total == 0) throw new IOException("Zero bytes downloaded (HTTP).");
            }
            return; // success
        } catch (IOException ex2) {
            // Cleanup any partial from Strategy 2 as well
            try { if (target.exists()) target.delete(); } catch (Exception ignore) {}
            IOException combined = new IOException("Failed to download after legacy+HTTP attempts: " + imageUrl);
            if (firstFailure != null) combined.addSuppressed(firstFailure);
            combined.addSuppressed(ex2);
            throw combined;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void downloadImageFast(String imageUrl, Path targetPath) throws IOException, InterruptedException {
        // Temp file in the same directory for atomic move
        Path dir = targetPath.getParent();
        if (dir != null) Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir != null ? dir : Path.of("."), ".dl_", ".part");

        // Build request (15s read timeout per image)
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        // Stream to temp; no in-memory buffering
        HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
        int code = resp.statusCode();

        // FollowRedirects.NORMAL should handle 3xx, but double-check just in case
        if (code >= 300 && code < 400) {
            String loc = resp.headers().firstValue("location").orElse("");
            if (!loc.isEmpty()) {
                URI redir = URI.create(loc).isAbsolute() ? URI.create(loc) : URI.create(new URL(new URL(imageUrl), loc).toString());
                req = HttpRequest.newBuilder(redir)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();
                resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
                code = resp.statusCode();
            }
        }

        if (code < 200 || code >= 300) {
            // Clean temp and bail
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw new IOException("HTTP " + code + " for " + imageUrl);
        }

        long total = 0;
        try (InputStream in = resp.body();
             OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buf = new byte[1 << 15]; // 32 KB buffer
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                total += r;
            }
        } catch (IOException e) {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw e;
        }

        if (total == 0) {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw new IOException("Zero bytes downloaded");
        }

        // Atomic replace
        Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void ensureParentDir(String filePath) {
        File f = new File(filePath);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                // if another process just created it, ignore
            }
        }
    }

    private static String safeGet(String[] arr, int idx) {
        return (idx >= 0 && idx < arr.length) ? arr[idx] : "";
    }

    private static void backupFile(File src, File dest) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        } catch (IOException e) {
            // Non-fatal: proceed without backup
            System.out.println("⚠️ Failed to create backup: " + dest.getPath() + " | " + e.getMessage());
        }
    }

    private static List<String[]> readCsv(File file) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                rows.add(parseCsvLine(line));
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to read CSV: " + file.getPath() + " | " + e.getMessage());
        }
        return rows;
    }

    private static void writeCsv(File file, List<String[]> rows) {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            // New file => write BOM
            fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 PrintWriter pw = new PrintWriter(osw)) {
                for (String[] cols : rows) {
                    String ts  = quote(csvSafe(cols, 0));
                    String url = quote(csvSafe(cols, 1));
                    String path= quote(csvSafe(cols, 2));
                    String err = quote(csvSafe(cols, 3));
                    pw.println(ts + "," + url + "," + path + "," + err);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to write CSV: " + file.getPath() + " | " + e.getMessage());
        }
    }

    private static String csvSafe(String[] cols, int idx) {
        if (cols == null || idx < 0 || idx >= cols.length) return "";
        String s = cols[idx] == null ? "" : cols[idx];
        return s.replace("\r", " ").replace("\n", " ");
    }

    // Minimal CSV parser that respects quoted fields and commas inside quotes
    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());

        // Unquote fields
        for (int i = 0; i < fields.size(); i++) {
            String f = fields.get(i);
            if (f.length() >= 2 && f.startsWith("\"") && f.endsWith("\"")) {
                f = f.substring(1, f.length() - 1).replace("\"\"", "\"");
            }
            fields.set(i, f);
        }
        return fields.toArray(new String[0]);
    }

    private static String quote(String s) {
        if (s == null) s = "";
        boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String q = s.replace("\"", "\"\"");
        return needQuotes ? "\"" + q + "\"" : "\"" + q + "\""; // we always quote for consistency
    }
}
