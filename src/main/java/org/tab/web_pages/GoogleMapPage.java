package org.tab.web_pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;

import static org.tab.utils.common.SharedMethods.*;

public class GoogleMapPage {

    @FindBy (xpath="//div[normalize-space()='Menu'][1]")
    public WebElement menuBtn;
    @FindBy (xpath="//div[@class='cRLbXd']//div[contains(@class,'dryRY')]")
    public WebElement imageContainer;
    @FindBy (xpath="//a[normalize-space()='English']")
    public WebElement enBtn;
    @FindBy (xpath="//button[contains(@class,'Tc0rEd XMkGfe cPtXLb')]")
    public WebElement nextBtn;


    public GoogleMapPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    /*public void getAllImages(WebDriver driver, String imageName) {
        for(int i=0;i<10;i++){
            moveMouseToElement(driver,imageContainer);
            if (nextBtn.isDisplayed()){
                try {
                    nextBtn.click();
                    moveMouseToElement(driver,menuBtn);
                }catch (Exception e){
                    System.out.println("No more images to load.");
                }
            }else
                break;
        }
        try {
            List<WebElement> images = driver.findElements(
                    By.xpath("//div[@class='cRLbXd']//div[contains(@class,'dryRY')]//img[@class='DaSXdd']")
            );
            System.out.println("Total images: " + images.size());

            int index = 1; // for naming files
            for (WebElement img : images) {
                String src = img.getAttribute("src");

                if (src != null && src.contains("lh3.googleusercontent.com")) {
                    // Replace size params with w1000-h1000
                    String highResSrc = src.replaceAll("w\\d+-h\\d+", "w1000-h1000");
                    // Save image in src/test/resources/images
                    downloadImage(highResSrc, "src/test/resources/images/image_" + imageName +"_"+index + ".png");
                    index++;
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("No images found in the specified container.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadImage(String imageUrl, String filePath) {
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(new File(filePath))) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to download image: " + imageUrl);
            e.printStackTrace();
        }
    }*/

    public void getAllImages(WebDriver driver, String imageName) {
        for (int i = 0; i < 15; i++) {
            try {
                moveMouseToElement(driver, imageContainer);
                if (nextBtn.isDisplayed()) {
                    nextBtn.click();
                    moveMouseToElement(driver, menuBtn);
                } else break;
            } catch (Exception ignored) {}
        }
        try {
            List<WebElement> images = driver.findElements(
                    By.xpath("//div[@class='cRLbXd']//div[contains(@class,'dryRY')]//img[@class='DaSXdd']")
            );
            // Prepare store folder: src/test/resources/images/<storeName>
            // Auto-clean (delete old files) before saving new ones
            File dir = new File("src/test/resources/images/" + imageName);
            if (dir.exists()) {
                try {
                    deleteDirectoryContents(dir);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to clean folder: " + dir.getPath() + " -> " + e.getMessage());
                }
            } else {
                dir.mkdirs();
            }

            int index = 1; // for naming files
            for (WebElement img : images) {
                String src = img.getAttribute("src");

                if (src != null && src.contains("lh3.googleusercontent.com")) {
                    // Replace size params with w1000-h1000
                    String highResSrc = src.replaceAll("w\\d+-h\\d+", "w1000-h1000");
                    // Save image in src/test/resources/images/<storeName>
                    downloadImage(highResSrc, dir.getPath() + "/image_" + imageName + "_" + index + ".png");
                    index++;
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("No images found in the specified container.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadImage(String imageUrl, String filePath) {
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(new File(filePath))) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to download image: " + imageUrl);
            e.printStackTrace();
        }
    }

    private void deleteDirectoryContents(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDirectoryContents(f);
            }
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

}
