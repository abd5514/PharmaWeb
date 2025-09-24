/*
package org.tab.utils;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.json.JSONObject; // from org.json (or use your preferred JSON lib)
import org.openqa.selenium.*;

public class CaptchaSolver {

    private static final String API_KEY = "YOUR_2CAPTCHA_API_KEY";
    private static final HttpClient http = HttpClient.newHttpClient();

    // 1) submit captcha solving request
    public static String submitRecaptcha(String siteKey, String pageUrl) throws Exception {
        String url = String.format(
                "http://2captcha.com/in.php?key=%s&method=userrecaptcha&googlekey=%s&pageurl=%s&json=1",
                API_KEY, siteKey, java.net.URLEncoder.encode(pageUrl, "UTF-8")
        );
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(resp.body());
        if (json.getInt("status") == 1) {
            return json.getString("request"); // captcha id
        } else {
            throw new RuntimeException("2captcha submit error: " + json.toString());
        }
    }

    // 2) poll for result
    public static String pollForResult(String captchaId, int maxSeconds) throws Exception {
        String resUrlTemplate = "http://2captcha.com/res.php?key=%s&action=get&id=%s&json=1";
        int waited = 0;
        while (waited < maxSeconds) {
            Thread.sleep(5000); // poll every 5 sec
            waited += 5;
            String url = String.format(resUrlTemplate, API_KEY, captchaId);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(resp.body());
            int status = json.getInt("status");
            if (status == 1) {
                return json.getString("request"); // the solution token
            } else {
                String reqStr = json.optString("request");
                // when not ready, 2captcha returns {"status":0,"request":"CAPCHA_NOT_READY"}
                // continue polling
            }
        }
        throw new RuntimeException("Timeout waiting for captcha solve");
    }

    // 3) inject token into page and submit
    public static void injectTokenAndSubmit(WebDriver driver, String token) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // some pages have a textarea with id 'g-recaptcha-response' hidden. Create it if absent:
        String jsCreate =
                "var el = document.getElementById('g-recaptcha-response');" +
                        "if (!el) { el = document.createElement('textarea'); el.id = 'g-recaptcha-response'; el.name = 'g-recaptcha-response'; el.style.display=''; document.body.appendChild(el);} " +
                        "el.value = arguments[0];" +
                        // dispatch events in case page listens for change
                        "el.dispatchEvent(new Event('change'));" +
                        "el.dispatchEvent(new Event('input'));";
        js.executeScript(jsCreate, token);

        // Now either submit the form or call the callback — simplest: submit the form that contains g-recaptcha-response
        // Adjust selector to your form
        try {
            js.executeScript("document.querySelector('form').submit();");
        } catch (Exception e) {
            // fallback: trigger click on submit button
            try {
                js.executeScript("document.querySelector(\"button[type='submit']\").click();");
            } catch (Exception ignored) {}
        }
    }

    // Example usage inside a test
    public static void solveRecaptchaFlow(WebDriver driver) throws Exception {
        String pageUrl = driver.getCurrentUrl();

        // Obtain site key (example: from element with data-sitekey)
        WebElement recaptchaElem = driver.findElement(By.cssSelector("[data-sitekey]"));
        String siteKey = recaptchaElem.getAttribute("data-sitekey");

        String captchaId = submitRecaptcha(siteKey, pageUrl);
        String token = pollForResult(captchaId, 180); // wait up to 3 minutes
        injectTokenAndSubmit(driver, token);

        // now continue with assertions
    }
}*/
