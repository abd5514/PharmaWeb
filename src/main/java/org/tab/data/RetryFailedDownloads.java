package org.tab.data;

import org.tab.utils.PropReader;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Retries downloads listed in src/test/resources/images/failed_downloads.csv.
 * CSV columns (no header): "timestamp","imageUrl","filePath","error"
 */
public class RetryFailedDownloads {

    private static final String FAILED_CSV = PropReader.get("csvFilePath", "src/test/resources/failed_downloads.csv");
    private static final String BACKUP_CSV = "src/test/resources/failed_downloads.csv.bak";

    public static void main(String[] args) {
        // Run from CLI or call retryAll() from your tests
        retryAll();
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
            // Expect 4 columns: timestamp, url, path, error
            if (cols.length < 3) continue;
            String imageUrl = safeGet(cols, 1);
            String filePath = safeGet(cols, 2);

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

    // --------- Helpers ---------

    private static void downloadImage(String imageUrl, String filePath) throws IOException {
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(new File(filePath))) {

            byte[] buffer = new byte[8192];
            int r;
            while ((r = in.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
        }
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
