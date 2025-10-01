/*
package org.tab.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDFConverter {

    public static void convertPdfToPng(File pdfFile) throws IOException {
        if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
            System.out.println("❌ Not a PDF file: " + pdfFile.getName());
            return;
        }

        String baseName = pdfFile.getName().replaceFirst("[.][^.]+$", "");
        File parentDir = pdfFile.getParentFile();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300); // high quality
                File outputFile = new File(parentDir, baseName + "_page_" + (i + 1) + ".png");
                ImageIO.write(image, "PNG", outputFile);
                System.out.println("✅ Saved: " + outputFile.getAbsolutePath());
            }
        }
    }

    public static void callMain(String path) {
        File dir = new File(path); // <-- change your path

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) {
            System.out.println("❌ No PDF files found in " + dir.getAbsolutePath());
            return;
        }

        for (File pdf : files) {
            try {
                convertPdfToPng(pdf);
            } catch (IOException e) {
                System.err.println("⚠ Error processing: " + pdf.getName());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        callMain("src/test/resources/images/Jeddah/BREW92 - AL MALIK"); // <-- change your path
    }
}
*/


//second try
/*
package org.tab.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDFConverter {

    public static void convertPdfToPng(File pdfFile) throws IOException {
        if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
            System.out.println("❌ Not a PDF file: " + pdfFile.getName());
            return;
        }

        String baseName = pdfFile.getName().replaceFirst("[.][^.]+$", "");
        File parentDir = pdfFile.getParentFile();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            boolean skipFolder = false;

            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300); // high quality

                if (image.getHeight() > 10000) {
                    System.out.printf("⚠ Skipping folder [%s] because page %d height = %d > 10000%n",
                            parentDir.getAbsolutePath(), (i + 1), image.getHeight());
                    skipFolder = true;
                    break;
                }
            }

            if (skipFolder) {
                return; // ❌ skip saving any images for this PDF
            }

            // ✅ If safe, render again and save images
            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                File outputFile = new File(parentDir, baseName + "_page_" + (i + 1) + ".png");
                ImageIO.write(image, "PNG", outputFile);
                System.out.println("✅ Saved: " + outputFile.getAbsolutePath());
            }
        }
    }

    public static void callMain(String path) {
        File dir = new File(path);

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) {
            System.out.println("❌ No PDF files found in " + dir.getAbsolutePath());
            return;
        }

        for (File pdf : files) {
            try {
                convertPdfToPng(pdf);
            } catch (IOException e) {
                System.err.println("⚠ Error processing: " + pdf.getName());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        callMain("src/test/resources/images/Jeddah/Boho Bistro"); // <-- change your path
    }
}
*/

package org.tab.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PDFConverter {

    public static List<String> convertPdfToPng(File pdfFile) throws IOException {

        if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
            System.out.println("❌ Not a PDF file: " + pdfFile.getName());
            return null;
        }
        List<String> paths=new java.util.ArrayList<>();
        String baseName = pdfFile.getName().replaceFirst("[.][^.]+$", "");
        File parentDir = pdfFile.getParentFile();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            // ✅ First validate all pages
            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300); // high quality
                if (image.getHeight() > 8500) {
                    throw new IOException(String.format(
                            "❌ Skipping folder [%s], page %d too large (height=%d)",
                            parentDir.getAbsolutePath(), (i + 1), image.getHeight()
                    ));
                }
            }

            // ✅ If safe, render again and save images
            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                File outputFile = new File(parentDir, baseName + "_page_" + (i + 1) + ".png");
                ImageIO.write(image, "PNG", outputFile);
                System.out.println("✅ Saved: " + outputFile.getAbsolutePath());
                paths.addAll(Collections.singleton(outputFile.getAbsolutePath()));
            }
        }
        return paths;
    }

    public static void callMain(String path) {
        File dir = new File(path);

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) {
            System.out.println("❌ No PDF files found in " + dir.getAbsolutePath());
            return;
        }

        for (File pdf : files) {
            try {
                convertPdfToPng(pdf);
            } catch (IOException e) {
                // ⚠️ Will now show the "height too large" exception here
                System.err.println("⚠ Error processing: " + pdf.getName() + " → " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        callMain("src/test/resources/images/Jeddah/Boho Bistro"); // <-- change your path
    }
}

