package org.tab.utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.testng.annotations.Listeners;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

@Listeners(ExtentTestListener.class)
public class NewImageMenuFilterParallel {

    // ===== Config (override via -Dkey=value) =====
    private static final String TESSDATA_PATH = System.getProperty("tessdata.path", "C:/Program Files/Tesseract-OCR/tessdata");
    private static final String OCR_LANG     = System.getProperty("ocr.lang", "eng+ara");
    private static final String IMAGES_ROOT  = System.getProperty("images.root", "src/test/resources/images");
    private static final int    THREADS      = Integer.getInteger("ocr.threads", 5);

    // --- LOGGING CONFIG ---
    private static final String DELETE_LOG         = System.getProperty("delete.log", "target/deleted_images.log");
    private static final boolean LOG_APPEND        = Boolean.parseBoolean(System.getProperty("delete.logAppend", "true"));
    public static final boolean LOG_FAILED_DELETE = Boolean.parseBoolean(System.getProperty("delete.logFailed", "false"));

    // ===== Internal =====
    private static final Set<String> IMAGE_EXTS = Set.of("png","jpg","jpeg","webp","bmp","tif","tiff","gif");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern ARABIC = Pattern.compile("\\p{InArabic}");
    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");

    private static final ThreadLocal<ITesseract> OCR = ThreadLocal.withInitial(() -> {
        Tesseract t = new Tesseract();
        t.setDatapath(TESSDATA_PATH);
        t.setLanguage(OCR_LANG);
        return t;
    });

    private static final Object LOG_LOCK = new Object();
    private static Path LOG_PATH;
    private static Path CURRENT_ROOT;

    // ====== KEYWORDS + thresholds (same as before; trim to your needs) ======
    private static final String[] KEYWORDS = new String[]{
            "latte","espresso","americano","mocha","tea","drip","cold brew","menu","price","cal","sr","sar",
            "قهوة","المشروبات","الحلويات","المخبوزات","قائمة","الاصناف","السعر","السعرات","حرارية","ريال","دينار"
    };
    private static final int MIN_LINES = Integer.getInteger("menu.minLines", 6);
    private static final int MIN_WORDS = Integer.getInteger("menu.minWords", 20);
    private static final int MIN_DIGITS = Integer.getInteger("menu.minDigits", 3);
    private static final int MIN_KEYWORD_HITS = Integer.getInteger("menu.minKeywordHits", 1);

    public static void main(String[] args) {
        processRootOnce();
    }

    public static void processRootOnce() {
        Path root = Paths.get(IMAGES_ROOT).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.out.println("❌ Images root not found or not a directory: " + root);
            return;
        }
        CURRENT_ROOT = root;

        initDeleteLog(); // <<< prepare the log file

        // collect images (root + first-level subfolders; keep as-is or switch to recursive if you like)
        List<Path> allImages = new ArrayList<>();
        addImagesFromFolder(root, allImages);
        try (Stream<Path> children = Files.list(root)) {
            children.filter(Files::isDirectory).forEach(sub -> addImagesFromFolder(sub, allImages));
        } catch (Exception e) {
            System.out.println("⚠️ Failed listing subfolders under: " + root + " | " + e.getMessage());
        }

        if (allImages.isEmpty()) {
            System.out.println("ℹ️ No images found to process in " + root);
            return;
        }

        System.out.printf("🔎 Processing %d images with %d threads…%n", allImages.size(), THREADS);

        AtomicInteger kept = new AtomicInteger();
        AtomicInteger deleted = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(THREADS * 4);
        ThreadPoolExecutor exec = new ThreadPoolExecutor(
                THREADS, THREADS, 30, TimeUnit.SECONDS, queue,
                r -> { Thread t = new Thread(r, "ocr-menu-worker"); t.setDaemon(true); return t; },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        List<Future<?>> futures = new ArrayList<>(allImages.size());
        for (Path img : allImages) {
            futures.add(exec.submit(() -> processOne(img, kept, deleted, failed)));
        }

        exec.shutdown();
        try {
            for (Future<?> f : futures) f.get();
            exec.awaitTermination(3, TimeUnit.MINUTES);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            System.out.println("⚠️ Uncaught task error: " + ee.getCause());
        }

        System.out.printf("✅ Done. Kept: %d | 🗑 Deleted: %d | ⚠️ OCR Failed: %d | Total: %d%n",
                kept.get(), deleted.get(), failed.get(), allImages.size());
    }

    /** process one image; log every successful deletion */
    private static void processOne(Path img, AtomicInteger kept, AtomicInteger deleted, AtomicInteger failed) {
        try {
            String text = extractText(img.toFile());
            boolean menu = isMenuLike(text);

            if (menu) {
                kept.incrementAndGet();
                System.out.println("✅ MENU detected, kept: " + img);
            } else {
                if (Files.deleteIfExists(img)) {
                    deleted.incrementAndGet();
                    System.out.println("🗑 Not a menu (or too little text), deleted: " + img);
                    logDeleted(img, "classified=not_menu"); // <<< LOG HERE
                } else {
                    kept.incrementAndGet();
                    System.out.println("⚠️ Couldn’t delete, kept: " + img);
                    if (LOG_FAILED_DELETE) logDeleted(img, "delete_failed");
                }
            }
        } catch (TesseractException te) {
            failed.incrementAndGet();
            System.out.println("⚠️ OCR failed for " + img + " -> " + te.getMessage());
        } catch (Exception e) {
            failed.incrementAndGet();
            System.out.println("⚠️ Error processing " + img + " -> " + e.getMessage());
        }
    }

    // ---------- LOGGING ----------
    private static void initDeleteLog() {
        try {
            LOG_PATH = Paths.get(DELETE_LOG).toAbsolutePath().normalize();
            Path parent = LOG_PATH.getParent();
            if (parent != null) Files.createDirectories(parent);

            if (!LOG_APPEND || !Files.exists(LOG_PATH)) {
                synchronized (LOG_LOCK) {
                    Files.writeString(LOG_PATH,
                            "timestamp,folder,file,relative_path,reason" + System.lineSeparator(),
                            CREATE, TRUNCATE_EXISTING);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to init delete log: " + e.getMessage());
        }
    }

    public static void logDeleted(Path img, String reason) {
        try {
            String rel = CURRENT_ROOT.relativize(img).toString().replace('\\','/');
            String folder = "(root)";
            int lastSlash = rel.lastIndexOf('/');
            if (lastSlash >= 0) folder = rel.substring(0, lastSlash);
            String file = img.getFileName().toString();
            String ts = LocalDateTime.now().toString();

            String line = String.format("%s,%s,%s,%s,%s%n",
                    ts, csv(folder), csv(file), csv(rel), csv(reason));

            synchronized (LOG_LOCK) {
                Files.writeString(LOG_PATH, line, CREATE, APPEND);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to write delete log for " + img + " -> " + e.getMessage());
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        String q = s.replace("\"","\"\"");
        return q.contains(",") ? "\"" + q + "\"" : q;
    }

    // ---------- OCR / heuristics (unchanged except thresholds above) ----------
    private static String extractText(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) return "";
        try {
            String out = OCR.get().doOCR(img);
            return out == null ? "" : out;
        } finally { img.flush(); }
    }

    public static boolean isMenuLike(String raw) {
        if (raw == null) return false;
        String text = raw.replace('\r','\n').trim();
        if (text.isEmpty()) return false;

        String[] lines = text.split("\n+");
        int lineCount = 0; for (String l : lines) if (l.trim().length() >= 2) lineCount++;
        String[] words = WORD_SPLIT.split(text);
        int wordCount = 0; for (String w : words) if (!w.isBlank()) wordCount++;
        int digitCount = 0; var m = DIGIT.matcher(text); while (m.find()) digitCount++;

        int hits = 0; String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : KEYWORDS) if (!kw.isBlank() && lower.contains(kw.toLowerCase(Locale.ROOT))) hits++;

        boolean hasArabic = ARABIC.matcher(text).find();
        boolean hasEnglish = lower.matches(".*[a-z].*");

        boolean looksDense = lineCount >= MIN_LINES && wordCount >= MIN_WORDS;
        boolean pricey     = digitCount >= MIN_DIGITS;
        boolean keywordy   = hits >= MIN_KEYWORD_HITS;
        boolean bilingualMenu = looksDense && (hasArabic && hasEnglish);

        return (looksDense && (pricey || keywordy)) || bilingualMenu;
    }

    private static void addImagesFromFolder(Path folder, List<Path> out) {
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p))
                    .forEach(out::add);
        } catch (Exception e) {
            System.out.println("⚠️ Failed listing files in " + folder + " -> " + e.getMessage());
        }
    }
    private static boolean isImageFile(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.'); if (dot < 0) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return IMAGE_EXTS.contains(ext);
    }
}
