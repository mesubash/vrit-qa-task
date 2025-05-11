package com.vrit.qa;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountSetupTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private String tempEmail;
    private String emailToken;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(40)); // Increased for slow loading
        driver.manage().window().maximize();
        driver.get("https://authorized-partner.netlify.app/register");
    }

    @Test
    public void testStep1AccountSetup() {
        // Handle Terms of Service consent
        try {
            // Locate checkbox
            WebElement termsCheckbox = wait.until(ExpectedConditions.elementToBeClickable(By.id("remember")));
            System.out.println("Checkbox found: id='remember'");

            // Click checkbox with JavaScript to ensure state change
            try {
                if (!termsCheckbox.isSelected()) {
                    termsCheckbox.click();
                    System.out.println("Checkbox checked via JavaScript");
                }
            } catch (Exception e) {
                System.out.println("Checkbox interaction failed: " + e.getMessage());
                throw e;
            }`
            System.out.println("Checkbox checked state: " + termsCheckbox.isSelected());

            // Locate and click Continue button
            WebElement continueButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector(".primary-btn")));
            System.out.println("Continue button located using CSS selector");
            continueButton.click();
            System.out.println("Continue button clicked");

            // Wait for Personal Details section
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("form")));
            System.out.println("Personal Details section loaded");
        } catch (Exception e) {
            System.out.println("Failed to interact with Terms checkbox or Continue button. Page source: " +
                    driver.getPageSource().substring(0, 500));
            throw new RuntimeException("Unable to proceed past Terms of Service: " + e.getMessage());
        }

        // Create a temporary email
        JSONObject emailAccount = MailinatorUtil.createTemporaryEmail();
        tempEmail = emailAccount.getString("address");
        emailToken = emailAccount.getString("token");
        System.out.println("Using email: " + tempEmail);

        // Generate random data
        String firstName = RandomStringUtils.randomAlphabetic(5, 10);
        String lastName = RandomStringUtils.randomAlphabetic(5, 10);
        String password = "Strong@" + RandomStringUtils.randomAlphanumeric(8);
        // Generate Nepali phone number (format: 9812345678 or 98-12345678)
        long phoneNumber = 9700000000L + (long) (Math.random() * (9899999999L - 9700000000L + 1));
        String phone = String.valueOf(phoneNumber); // e.g., "9812345678"

        // Locate form fields
        try {
            // Debug: Log field counts
            System.out.println("First name field count: " + driver.findElements(By.name("firstName")).size());
            System.out.println("Last name field count: " + driver.findElements(By.name("lastName")).size());
            System.out.println("Email field count: " + driver.findElements(By.name("email")).size());
            System.out.println("Phone field count: " + driver.findElements(By.name("phoneNumber")).size());
            System.out.println("Password field count: " + driver.findElements(By.name("password")).size());
            System.out
                    .println("Confirm password field count: " + driver.findElements(By.name("confirmPassword")).size());
            System.out
                    .println("Next button count: " + driver.findElements(By.xpath("//button[@type='submit']")).size());

            WebElement firstNameField = wait.until(ExpectedConditions.elementToBeClickable(By.name("firstName")));
            WebElement lastNameField = wait.until(ExpectedConditions.elementToBeClickable(By.name("lastName")));
            WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
            WebElement phoneField = wait.until(ExpectedConditions.elementToBeClickable(By.name("phoneNumber")));
            WebElement passwordField = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));
            WebElement confirmPasswordField = wait
                    .until(ExpectedConditions.elementToBeClickable(By.name("confirmPassword")));
            WebElement nextButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")));

            // Fill form
            fillField(firstNameField, firstName, "First name");
            fillField(lastNameField, lastName, "Last name");
            fillField(emailField, tempEmail, "Email");
            try {
                fillField(phoneField, phone, "Phone");
            } catch (Exception e) {
                System.out.println("Phone format failed, trying alternative format");
                String altPhone = phoneNumber / 100000000 % 100 + "-" + phoneNumber % 100000000; // e.g., "98-12345678"
                fillField(phoneField, altPhone, "Phone (alternative format)");
            }
            fillField(passwordField, password, "Password");
            fillField(confirmPasswordField, password, "Confirm password");

            // Verify button is enabled
            wait.until(ExpectedConditions.elementToBeClickable(nextButton));
            assertTrue(nextButton.isEnabled(), "Next button should be enabled after valid input");
            System.out.println("Next button enabled: " + nextButton.isEnabled());

            // Click Next button
            nextButton.click();
            System.out.println("Next button clicked");
        } catch (Exception e) {
            System.out.println("Failed to fill form fields. Page source: " + driver.getPageSource().substring(0, 500));
            throw new RuntimeException("Form filling failed: " + e.getMessage());
        }

        // Verify navigation to Step 2
        try {
            WebElement step2Title = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//h2[contains(text(),'Agency Details')]")));
            assertTrue(step2Title.isDisplayed(), "Failed to navigate to Step 2");
            System.out.println("Navigated to Step 2: Agency Details");
        } catch (Exception e) {
            System.out.println("Navigation to Step 2 failed. Page source: " + driver.getPageSource().substring(0, 500));
            throw new RuntimeException("Navigation failed: " + e.getMessage());
        }

        // Fetch and input verification code
        try {
            String verificationCode = MailinatorUtil.fetchVerificationCode(emailToken);
            WebElement verificationField = driver.findElement(By.id("verificationCode"));
            fillField(verificationField, verificationCode, "Verification code");
        } catch (NoSuchElementException e) {
            System.out.println(
                    "Verification code field not found in Step 1. OTP retrieved for use in Step 4: " + tempEmail);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch verification code: " + e.getMessage());
        }
    }

    private void fillField(WebElement field, String value, String fieldName) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", field);
            field.clear();
            field.sendKeys(value);
            // Verify input
            String actualValue = field.getAttribute("value");
            if (!value.equals(actualValue)) {
                System.out.println("Warning: " + fieldName + " value not set correctly. Expected: " + value
                        + ", Actual: " + actualValue);
                // Fallback: Use JavaScript to set value
                ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", field, value);
                actualValue = field.getAttribute("value");
            }
            System.out.println(fieldName + " field filled: " + actualValue);
        } catch (Exception e) {
            System.out.println("Failed to fill " + fieldName + ": " + e.getMessage());
            throw e;
        }
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

class MailinatorUtil {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static JSONObject createTemporaryEmail() {
        String random = RandomStringUtils.randomAlphanumeric(8);
        JSONObject result = new JSONObject();
        result.put("address", "test" + random + "@mailinator.com");
        result.put("token", random);
        return result;
    }

    public static String fetchVerificationCode(String token) throws Exception {
        // Poll for messages (up to 90 seconds)
        for (int i = 0; i < 18; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://mailinator.com/api/v2/domains/public/mailinator.com/inboxes/test" + token))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseJson = new JSONObject(response.body());
            JSONArray messages = responseJson.getJSONArray("messages");

            if (messages.length() > 0) {
                String messageId = messages.getJSONObject(0).getString("id");
                request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://mailinator.com/api/v2/domains/public/mailinator.com/messages/" + messageId))
                        .GET()
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject message = new JSONObject(response.body());
                String emailBody = message.getString("text");
                return extractVerificationCode(emailBody);
            }
            Thread.sleep(5000); // Wait 5 seconds before retrying
        }
        throw new RuntimeException("No verification email received after 90 seconds.");
    }

    public static String extractVerificationCode(String emailBody) {
        // Extract 6-digit code
        String code = emailBody.replaceAll("[^0-9]", "");
        return code.length() >= 6 ? code.substring(0, 6) : code;
    }
}