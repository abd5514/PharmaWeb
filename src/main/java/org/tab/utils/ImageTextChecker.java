package org.tab.utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ImageTextChecker {

    // -----------------------
    // Config (override via -Dkey=value)
    // -----------------------
    private static final String TESSDATA_PATH = System.getProperty("tessdata.path", "C:/Program Files/Tesseract-OCR/tessdata");
    private static final String OCR_LANG = System.getProperty("ocr.lang", "eng"); // e.g. "eng+ara"
    private static final String IMAGES_ROOT = System.getProperty("images.root", "src/test/resources/images");
    private static final int THREADS = Integer.getInteger("ocr.threads", 5);       // set to 5 by default

    private static final Set<String> IMAGE_EXTS = Set.of("png", "jpg", "jpeg", "webp", "bmp", "tif", "tiff", "gif");

    // Each thread gets its own Tesseract instance (Tess4J is NOT thread-safe)
    private static final ThreadLocal<ITesseract> OCR = ThreadLocal.withInitial(() -> {
        Tesseract t = new Tesseract();
        t.setDatapath(TESSDATA_PATH);
        t.setLanguage(OCR_LANG);
        return t;
    });

    public static void main(String[] args) {
        processRootOnce();
    }

    /** Scan images root, process images in root + each immediate subfolder with a thread pool. */
    public static void processRootOnce() {
        Path root = Paths.get(IMAGES_ROOT).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.out.println("❌ Images root not found or not a directory: " + root);
            return;
        }

        System.out.println("🔎 Scanning (parallel " + THREADS + "): " + root);

        AtomicInteger kept = new AtomicInteger();
        AtomicInteger deleted = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        // Collect all image paths (root + one level of subfolders)
        List<Path> allImages = new ArrayList<>();
        // Images directly in root
        addImagesFromFolder(root, allImages);
        // Images in immediate subfolders
        try (Stream<Path> children = Files.list(root)) {
            children.filter(Files::isDirectory).forEach(subdir -> addImagesFromFolder(subdir, allImages));
        } catch (Exception e) {
            System.out.println("⚠️ Failed listing subfolders under: " + root + " | " + e.getMessage());
        }

        if (allImages.isEmpty()) {
            System.out.println("ℹ️ No images found to process.");
            return;
        }

        // Bounded queue to avoid RAM spikes on huge sets
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(THREADS * 4);
        ThreadPoolExecutor exec = new ThreadPoolExecutor(
                THREADS, THREADS, 30, TimeUnit.SECONDS, queue,
                r -> {
                    Thread t = new Thread(r, "ocr-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // backpressure
        );

        List<Future<?>> futures = new ArrayList<>(allImages.size());
        for (Path img : allImages) {
            futures.add(exec.submit(() -> processOne(img, kept, deleted, failed)));
        }

        exec.shutdown();
        try {
            // Wait for all tasks to finish
            for (Future<?> f : futures) f.get();
            exec.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println("⛔ Interrupted while waiting for OCR tasks.");
        } catch (ExecutionException ee) {
            System.out.println("⚠️ Uncaught OCR task error: " + ee.getCause());
        }

        System.out.printf("✅ Done. Kept: %d | 🗑 Deleted: %d | ⚠️ OCR Failed: %d | Total: %d%n",
                kept.get(), deleted.get(), failed.get(), allImages.size());
    }

    /** Submit-friendly single-image processing. */
    private static void processOne(Path img,
                                   AtomicInteger kept,
                                   AtomicInteger deleted,
                                   AtomicInteger failed) {
        try {
            boolean hasText = hasAnyText(img.toFile());
            if (!hasText) {
                if (Files.deleteIfExists(img)) {
                    deleted.incrementAndGet();
                    System.out.println("🗑 Deleted (no text): " + img);
                } else {
                    kept.incrementAndGet();
                    System.out.println("⚠️ Failed to delete: " + img);
                }
            } else {
                kept.incrementAndGet();
                System.out.println("✅ Text found, kept: " + img);
            }
        } catch (TesseractException te) {
            failed.incrementAndGet();
            System.out.println("⚠️ OCR failed for " + img + " -> " + te.getMessage());
        } catch (Exception e) {
            failed.incrementAndGet();
            System.out.println("⚠️ Error processing " + img + " -> " + e.getMessage());
        }
    }

    /** Keep your original single-file API for compatibility. */
    public static void processImage(String imagePath) {
        processOne(Paths.get(imagePath), new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
    }

    // -----------------------
    // Helpers
    // -----------------------

    private static void addImagesFromFolder(Path folder, List<Path> out) {
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(ImageTextChecker::isImageFile)
                    .forEach(out::add);
        } catch (Exception e) {
            System.out.println("⚠️ Failed listing files in " + folder + " -> " + e.getMessage());
        }
    }

    private static boolean isImageFile(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1).toLowerCase();
        return IMAGE_EXTS.contains(ext);
    }

    private static boolean hasAnyText(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            // unreadable/corrupt image → treat as "no text" (skip delete logic in caller will still try to delete)
            return false;
        }
        try {
            String result = OCR.get().doOCR(img);
            return result != null && !result.trim().isEmpty();
        } finally {
            // Help GC on massive batches
            img.flush();
        }
    }
}