package org.tab.utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.testng.annotations.Listeners;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Word;
import java.awt.Rectangle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

@Listeners(ExtentTestListener .class)
public class ImageMenuFilterParallel {

    // -----------------------
    // Config (override via -Dkey=value)
    // -----------------------
    private static final String TESSDATA_PATH = System.getProperty("tessdata.path", "C:/Program Files/Tesseract-OCR/tessdata");
    private static final String OCR_LANG = System.getProperty("ocr.lang", "eng+ara"); // both English + Arabic
    private static final String IMAGES_ROOT = System.getProperty("images.root", "src/test/resources/images");
    private static final int THREADS = Integer.getInteger("ocr.threads", 7);

    private static final boolean LOG_FAILED_DELETE = Boolean.parseBoolean(System.getProperty("delete.logFailed", "false"));
    private static final Set<String> IMAGE_EXTS = Set.of("png", "jpg", "jpeg", "webp", "bmp", "tif", "tiff");

    /*// Menu detection heuristics (tweak to taste)
    private static final int MIN_LINES = Integer.getInteger("menu.minLines", 6);
    private static final int MIN_WORDS = Integer.getInteger("menu.minWords", 20);
    private static final int MIN_DIGITS = Integer.getInteger("menu.minDigits", 2);
    private static final int MIN_KEYWORD_HITS = Integer.getInteger("menu.minKeywordHits", 1);*/

    // stricter menu signals
    private static final int MIN_PRICE_LINES     = Integer.getInteger("menu.minPriceLines", 2);
    private static final int MIN_DIGIT_GROUPS    = Integer.getInteger("menu.minDigitGroups", 5);
    private static final int MIN_WORDS_DENSITY   = Integer.getInteger("menu.minWords", 24);
    private static final int MIN_LINES_DENSITY   = Integer.getInteger("menu.minLines", 8);
    private static final double MIN_TEXTAREA_RATIO = Double.parseDouble(System.getProperty("menu.minTextArea", "0.015")); // 1.5%
    private static final boolean DEBUG_MENU      = Boolean.parseBoolean(System.getProperty("menu.debug", "false"));

    // Typical menu keywords (English + Arabic). Add/remove freely.
    private static final String[] KEYWORDS = new String[]{
            // English
            "latte","espresso","americano","mocha","tea","green tea","turkish","flat white","drip","cold brew",
            "signature","bakery","cold drinks","hot coffee","cold coffee","v60","ice cream","tiramisu","sharing",
            "menu","price","cal","calories","sr","sar",
            // Arabic
            "قهوة","مشروبات","باردة","ساخنة","مقطرة","لاتيه","اسبريسو","امريكانو","موكا","شاي","قائمة","سعر","ريال","سعر","كالوري","حليب","مخبو","لومي"
    };

    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern ARABIC = Pattern.compile("\\p{InArabic}");
    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");
    private static final Pattern DIGIT_GROUP =
            Pattern.compile("(?<![\\p{L}\\p{N}])\\d{1,3}(?:[.,]\\d{1,2})?(?![\\p{L}\\p{N}])"); // 7, 7.45, 12, 210
    private static final Pattern CURRENCY_WORDS =
            Pattern.compile("\\b(?:sr|sar|jd|jod|aed|qr|egp)\\b|ريال|ر\\.س|دينار|د\\.أ", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CALORIE_WORDS =
            Pattern.compile("\\b(?:cal|kcal|calories)\\b|سعرات|حرارية", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Each thread gets its own Tesseract instance (Tess4J is NOT thread-safe)
    private static final ThreadLocal<ITesseract> OCR = ThreadLocal.withInitial(() -> {
        Tesseract t = new Tesseract();
        t.setDatapath(TESSDATA_PATH);
        t.setLanguage(OCR_LANG); // e.g. "eng+ara"
        return t;
    });
    private static final Object LOG_LOCK = new Object();
    private static Path LOG_PATH;
    private static Path CURRENT_ROOT;

    public static void main(String[] args) {
        processRootOnce();
    }

    /** Scan images root, OCR in parallel, keep only "menu-like" images. */
    public static void processRootOnce() {
        Path root = Paths.get(IMAGES_ROOT).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.out.println("❌ Images root not found or not a directory: " + root);
            return;
        }

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

    /** Single file path entry point (kept for compatibility). */
    public static void processImage(String imagePath) {
        processOne(Paths.get(imagePath), new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
    }

    // ------------ internals ------------

    /*private static void processOne(Path img, AtomicInteger kept, AtomicInteger deleted, AtomicInteger failed) {
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
                } else {
                    kept.incrementAndGet();
                    System.out.println("⚠️ Couldn’t delete, kept: " + img);
                }
            }
        } catch (TesseractException te) {
            failed.incrementAndGet();
            // On OCR error, keep it (safety)
            System.out.println("⚠️ OCR failed for " + img + " -> " + te.getMessage());
        } catch (Exception e) {
            failed.incrementAndGet();
            System.out.println("⚠️ Error processing " + img + " -> " + e.getMessage());
        }
    }*/

    private static void processOne(Path img, AtomicInteger kept, AtomicInteger deleted, AtomicInteger failed) {
        try {
            OcrMetrics m = extractMetrics(img.toFile());
            boolean menu = isMenuLike(m);

            if (DEBUG_MENU) {
                System.out.printf("DBG %s lines=%d words=%d digitGrp=%d priceLines=%d curr=%d kcal=%d kw=%d ar=%b en=%b text%%=%.3f => %s%n",
                        img.getFileName(), m.lineCount, m.wordCount, m.digitGroups, m.priceLines,
                        m.currencyHits, m.calorieHits, m.keywordHits, m.hasArabic, m.hasEnglish, m.textAreaRatio,
                        menu ? "KEEP" : "DELETE");
            }

            if (menu) {
                kept.incrementAndGet();
                System.out.println("✅ MENU detected, kept: " + img);
            } else {
                if (Files.deleteIfExists(img)) {
                    deleted.incrementAndGet();
                    System.out.println("🗑 Not a menu, deleted: " + img);
                    logDeleted(img, "classified=not_menu");
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


    private static String extractText(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) return "";
        try {
            String out = OCR.get().doOCR(img);
            return out == null ? "" : out;
        } finally {
            img.flush();
        }
    }

    /*
    * new version of extract text below
    * */
    private static final class OcrMetrics {
        String text;
        int lineCount;
        int wordCount;
        int digitGroups;
        int currencyHits;
        int calorieHits;
        int keywordHits;
        int priceLines;           // lines that look like prices
        boolean hasArabic;
        boolean hasEnglish;
        double textAreaRatio;     // total OCR text area / image area
    }

    private static OcrMetrics extractMetrics(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        OcrMetrics mtr = new OcrMetrics();
        if (img == null) { mtr.text = ""; return mtr; }

        try {
            String text = OCR.get().doOCR(img);
            mtr.text = (text == null) ? "" : text.replace('\r', '\n');

            // words + area
            List<Word> words = OCR.get().getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            long textArea = 0;
            for (Word w : words) {
                Rectangle r = w.getBoundingBox();
                textArea += (long) r.width * r.height;
            }
            long imgArea = (long) img.getWidth() * img.getHeight();
            mtr.textAreaRatio = imgArea > 0 ? (double) textArea / imgArea : 0.0;
            mtr.wordCount = words.size();

            // lines + price lines
            List<Word> lines = OCR.get().getWords(img, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
            mtr.lineCount = lines.size();
            int priceLines = 0;
            for (Word ln : lines) {
                String lt = ln.getText() == null ? "" : ln.getText();
                if (DIGIT_GROUP.matcher(lt).find() || CURRENCY_WORDS.matcher(lt).find()) {
                    priceLines++;
                }
            }
            mtr.priceLines = priceLines;

            // digits/currency/calories/keywords
            Matcher dg = DIGIT_GROUP.matcher(mtr.text);
            while (dg.find()) mtr.digitGroups++;
            mtr.currencyHits = countMatches(CURRENCY_WORDS, mtr.text);
            mtr.calorieHits  = countMatches(CALORIE_WORDS, mtr.text);

            int hits = 0;
            String lower = mtr.text.toLowerCase(java.util.Locale.ROOT);
            for (String kw : KEYWORDS) if (!kw.isBlank() && lower.contains(kw.toLowerCase())) hits++;
            mtr.keywordHits = hits;

            mtr.hasArabic  = ARABIC.matcher(mtr.text).find();
            mtr.hasEnglish = lower.matches(".*[a-z].*");
            return mtr;
        } finally {
            img.flush();
        }
    }

    private static int countMatches(Pattern p, String s) {
        int n = 0; Matcher mm = p.matcher(s == null ? "" : s); while (mm.find()) n++; return n;
    }

    /**
     * Heuristic for "menu" pages:
     * - Enough lines & words (menus have many short lines)
     * - Several digits (prices/calories)
     * - Hits on menu keywords (EN/AR) OR presence of Arabic + English mix
     */
    /*public static boolean isMenuLike(String raw) {
        if (raw == null) return false;

        String text = raw.replace('\r', '\n').trim();
        if (text.isEmpty()) return false;

        String[] lines = text.split("\n+");
        int lineCount = 0;
        for (String l : lines) {
            // ignore tiny noise lines
            if (l.trim().length() >= 2) lineCount++;
        }

        String[] words = WORD_SPLIT.split(text);
        int wordCount = 0;
        for (String w : words) if (!w.isBlank()) wordCount++;

        int digitCount = 0;
        var m = DIGIT.matcher(text);
        while (m.find()) digitCount++;

        // keyword hits
        int hits = 0;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : KEYWORDS) {
            if (kw.isBlank()) continue;
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) hits++;
        }

        boolean hasArabic = ARABIC.matcher(text).find();
        boolean hasEnglish = lower.matches(".*[a-z].*");

        // Decision: needs structure + either prices/keywords or bilingual cues
        boolean looksDense = lineCount >= MIN_LINES && wordCount >= MIN_WORDS;
        boolean pricey = digitCount >= MIN_DIGITS;
        boolean keywordy = hits >= MIN_KEYWORD_HITS;
        boolean bilingualMenu = looksDense && (hasArabic && hasEnglish);

        return (looksDense && (pricey || keywordy)) || bilingualMenu;
    }*/

    /*public static boolean isMenuLike(String raw) {
        if (raw == null) return false;
        String text = raw.replace('\r','\n').trim();
        if (text.isEmpty()) return false;

        String[] linesAll = text.split("\n+");
        int lineCount = 0;
        for (String l : linesAll) if (l.trim().length() >= 2) lineCount++;

        String[] words = WORD_SPLIT.split(text);
        int wordCount = 0;
        for (String w : words) if (!w.isBlank()) wordCount++;

        int digitGroups = countMatches(DIGIT_GROUP, text);          // prices like 7.45 / 12 / 210
        int currencyHits = countMatches(CURRENCY_WORDS, text);      // SR / SAR / ريال / دينار / ...
        int calorieHits  = countMatches(CALORIE_WORDS, text);       // CAL / سعرات / حرارية
        int keywordHits  = countKeywordHits(text, KEYWORDS);
        boolean hasArabic  = ARABIC.matcher(text).find();
        boolean hasEnglish = text.matches(".*[a-zA-Z].*");

        // density checks
        boolean looksDense = lineCount >= Integer.getInteger("menu.minLines", 8)
                && wordCount >= Integer.getInteger("menu.minWords", 24);

        // pricing/units evidence
        boolean hasPricesOrUnits = digitGroups >= Integer.getInteger("menu.minDigitGroups", 3)
                || currencyHits > 0
                || calorieHits > 0;

        boolean bilingualCue = looksDense && hasArabic && hasEnglish;

        boolean decision = (looksDense && hasPricesOrUnits && (keywordHits >= 1 || bilingualCue))
                || (hasArabic && digitGroups >= 4 && lineCount >= 6); // Arabic fallback

        // DEBUG metrics (enable with -Dmenu.debug=true)
        if (Boolean.getBoolean("menu.debug")) {
            System.out.printf(
                    "DBG menuCheck: lines=%d words=%d digitGroups=%d currency=%d kcal=%d kw=%d ar=%b en=%b => %s%n",
                    lineCount, wordCount, digitGroups, currencyHits, calorieHits, keywordHits, hasArabic, hasEnglish, decision ? "KEEP" : "DELETE"
            );
        }
        return decision;
    }*/

    public static boolean isMenuLike(String ignoredRaw) { // signature kept, but we won't use 'ignoredRaw'
        // This method expects you to have called extractMetrics in the caller.
        throw new UnsupportedOperationException("Use isMenuLike(OcrMetrics) instead");
    }

    private static boolean isMenuLike(OcrMetrics m) {
        // 1) Reject logos/single-word photos: very small total text on the image
        if (m.textAreaRatio < MIN_TEXTAREA_RATIO && m.priceLines < MIN_PRICE_LINES) return false;

        // 2) Strong menu signal: multiple lines that look like prices/currency
        if (m.priceLines >= MIN_PRICE_LINES) return true;

        // 3) Dense text + numeric evidence + keywords or bilingual cue
        boolean looksDense = (m.lineCount >= MIN_LINES_DENSITY) && (m.wordCount >= MIN_WORDS_DENSITY);
        boolean numericEvidence = (m.digitGroups >= MIN_DIGIT_GROUPS) || m.currencyHits > 0 || m.calorieHits > 0;
        boolean bilingualCue = looksDense && (m.hasArabic && m.hasEnglish);

        return (looksDense && numericEvidence && (m.keywordHits >= 1 || bilingualCue))
                || (m.hasArabic && m.digitGroups >= 4 && m.lineCount >= 6); // Arabic fallback
    }

    /*private static int countMatches(Pattern p, String s) {
        int n = 0; var m = p.matcher(s); while (m.find()) n++; return n;
    }*/
    private static int countKeywordHits(String text, String[] kws) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        int hits = 0; for (String k : kws) if (!k.isBlank() && lower.contains(k.toLowerCase())) hits++; return hits;
    }

    private static void addImagesFromFolder(Path folder, List<Path> out) {
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(ImageMenuFilterParallel::isImageFile)
                    .forEach(out::add);
        } catch (Exception e) {
            System.out.println("⚠️ Failed listing files in " + folder + " -> " + e.getMessage());
        }
    }

    private static boolean isImageFile(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return IMAGE_EXTS.contains(ext);
    }

    private static void logDeleted(Path img, String reason) {
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
}