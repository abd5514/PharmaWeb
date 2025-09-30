package org.utils;

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

    public static void main(String path) {
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
}
