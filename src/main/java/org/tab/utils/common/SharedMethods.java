package org.tab.utils.common;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tab.web_pages.LoginPage;

import java.time.Duration;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import static org.tab.core.instance.DriverManager.getDriver;

public class SharedMethods {

    WebDriver driver;
    public String winHandleBefore ;
    // Windows-safe name sanitization for folders/files
    private static final String WINDOWS_RESERVED = "(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$";

    public static String sanitizeForWindows(String s) {
        if (s == null || s.isBlank()) return "unknown";
        String cleaned = s
                .replaceAll("[\\\\/:*?\"<>|]", "_")   // illegal characters
                .replaceAll("[\\p{Cntrl}]", "_")      // control chars
                .trim();
        // remove trailing dots/spaces (Windows forbids them at end)
        cleaned = cleaned.replaceAll("[\\.\\s]+$", "");
        // avoid reserved device names
        if (cleaned.matches(WINDOWS_RESERVED)) cleaned = "_" + cleaned;
        return cleaned;
    }


    public SharedMethods(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    /*public static String generateRandomString(){
        int length = 10;
        boolean useLetters = true;
        boolean useNumbers = false;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }*/

    public  int getCurrentDay() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        //Get Current Day as a number
        int todayInt = calendar.get(Calendar.DAY_OF_MONTH);
        return todayInt;
    }


    public static void waitUntilElementVisible(WebElement webElement)
    {
        WebDriverWait wait = new WebDriverWait(getDriver (), Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOf(webElement));
    }
    public static void waitUntilElementClickable(WebElement webElement)
    {
        WebDriverWait wait = new WebDriverWait(getDriver (),Duration.ofSeconds(30));
        wait.until(ExpectedConditions.elementToBeClickable(webElement));
    }

    public static void waitUntilTextChanged(WebElement element, String text) {
        WebDriverWait wait = new WebDriverWait(getDriver (),Duration.ofSeconds(10));
        wait.until(ExpectedConditions.textToBePresentInElement(element, text));
    }

    public static void mouseOverAction(WebElement Locator)
    {
        String javaScript = "var evObj = document.createEvent('MouseEvents');" +
                "evObj.initMouseEvent(\"mouseover\",true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);" +
                "arguments[0].dispatchEvent(evObj);";
        ((JavascriptExecutor)getDriver ()).executeScript(javaScript, Locator);
    }

    public static void jsScrollUp(WebElement Locator)
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver ();
        js.executeScript("window.scrollBy(0,-250)", Locator);
    }

    public static void jsScrollDown(WebElement Locator)
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver ();
        js.executeScript("window.scrollBy(0,350)", Locator);
    }

    public static void jsScrollDown(int pixel)
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver ();
        js.executeScript("window.scrollBy(0,"+pixel+")");
    }

    public static void mouseClickAction(WebElement Locator)
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver ();
        js.executeScript("arguments[0].scrollIntoView()", Locator);
        js.executeScript("arguments[0].click()", Locator);
    }


    public void waitElement(WebElement Locator)
    {
        WebDriverWait wait = new WebDriverWait(getDriver (),Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable((Locator)));
    }

    public void changeAttire(WebElement locator){
        JavascriptExecutor js = (JavascriptExecutor) getDriver ();
        js.executeScript("arguments[0].removeAttribute('disabled','disabled')",locator);
    }

    public static void pageBottom(){
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("window.scrollBy(0,document.body.scrollHeight)", "");
    }

    public static void login(){
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.doLogin();
    }

    public static void staticWait(int milliseconds){
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateRandomString(int length) {
        boolean useLetters = true;
        boolean useNumbers = false;
        java.util.Random rand = new java.util.Random();
        int number   = 1000 + rand.nextInt(9000);
        return (org.apache.commons.lang3.RandomStringUtils.random(length, useLetters, useNumbers) + number);
    }

    public static int generateRandomNumber(int length) {
        Random rand = new java.util.Random();
        return rand.nextInt(length);
    }

    public static void moveMouseToElement(WebDriver driver, WebElement element) {
        try {
            Actions actions = new Actions(driver);
            actions.moveToElement(element)
                    .pause(java.time.Duration.ofMillis(300)) // optional hover pause
                    .perform();
        } catch (Exception e) {
            System.out.println("❌ Failed to move mouse to element: " + e.getMessage());
        }
    }
}
