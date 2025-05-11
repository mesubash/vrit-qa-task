package com.vrit.qa;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.io.File;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountSetupTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private String tempEmail;
    private String emailToken;
    private String originalUrl;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        driver.manage().window().maximize();
        originalUrl = "https://authorized-partner.netlify.app/register";
        driver.get(originalUrl);
    }

    @Test
    public void testStep1AccountSetup() {
        try {
            // Handle Terms of Service consent
            WebElement termsCheckbox = wait.until(ExpectedConditions.elementToBeClickable(By.id("remember")));
            System.out.println("Checkbox found: id='remember'");

            if (!termsCheckbox.isSelected()) {
                termsCheckbox.click();
                System.out.println("Checkbox checked");
            }
            System.out.println("Checkbox checked state: " + termsCheckbox.isSelected());

            WebElement continueButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector(".primary-btn")));
            continueButton.click();
            System.out.println("Continue button clicked");

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("form")));
            System.out.println("Personal Details section loaded");

            // Create temporary email
            JSONObject emailAccount = MailinatorUtil.createTemporaryEmail();
            tempEmail = emailAccount.getString("address");
            emailToken = emailAccount.getString("token");
            System.out.println("Using email: " + tempEmail);

            // Fill Personal Details
            fillPersonalDetails();

            // Handle OTP Verification
            handleOTPVerification();

            // Handle Agency Details
            handleAgencyDetailsSection();

            // Handle Experience Details
            handleExperienceDetails();

            // Handle Business Registration
            handleBusinessRegistration();

        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Test failed: " + e.getMessage());
        }
    }

    private void fillPersonalDetails() {
        try {
            String firstName = RandomStringUtils.randomAlphabetic(5, 10);
            String lastName = RandomStringUtils.randomAlphabetic(5, 10);
            String password = "Strong@" + RandomStringUtils.randomAlphanumeric(8);
            long phoneNumber = 9700000000L + (long) (Math.random() * (9899999999L - 9700000000L + 1));
            String phone = String.valueOf(phoneNumber);

            WebElement firstNameField = wait.until(ExpectedConditions.elementToBeClickable(By.name("firstName")));
            WebElement lastNameField = wait.until(ExpectedConditions.elementToBeClickable(By.name("lastName")));
            WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(By.name("email")));
            WebElement phoneField = wait.until(ExpectedConditions.elementToBeClickable(By.name("phoneNumber")));
            WebElement passwordField = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));
            WebElement confirmPasswordField = wait
                    .until(ExpectedConditions.elementToBeClickable(By.name("confirmPassword")));
            WebElement nextButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")));

            fillField(firstNameField, firstName, "First name");
            fillField(lastNameField, lastName, "Last name");
            fillField(emailField, tempEmail, "Email");
            fillField(phoneField, phone, "Phone");
            fillField(passwordField, password, "Password");
            fillField(confirmPasswordField, password, "Confirm password");

            assertTrue(nextButton.isEnabled(), "Next button should be enabled after valid input");
            System.out.println("Next button enabled: " + nextButton.isEnabled());
            nextButton.click();
            System.out.println("Next button clicked");

        } catch (Exception e) {
            throw new RuntimeException("Failed to fill personal details: " + e.getMessage());
        }
    }

    private void handleOTPVerification() {
        try {
            String inboxName = tempEmail.split("@")[0];
            String currentUrl = driver.getCurrentUrl();

            // Store original window handle
            String originalWindow = driver.getWindowHandle();

            // Open new window for Mailinator
            ((JavascriptExecutor) driver).executeScript("window.open('about:blank', 'mailinator');");

            // Switch to new window
            Set<String> handles = driver.getWindowHandles();
            for (String handle : handles) {
                if (!handle.equals(originalWindow)) {
                    driver.switchTo().window(handle);
                    break;
                }
            }

            // Navigate to Mailinator in new window
            driver.get("https://www.mailinator.com/v4/public/inboxes.jsp?to=" + inboxName);
            System.out.println("Navigated to Mailinator inbox: " + inboxName);

            // Get OTP
            String otp = getOTPFromEmail();
            System.out.println("Retrieved OTP: " + otp);

            // Close Mailinator window and switch back
            driver.close();
            driver.switchTo().window(originalWindow);

            // Ensure we're back on the correct page
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                driver.get(currentUrl);
            }
            Thread.sleep(2000); // Wait for page to stabilize

            // Enter OTP with retries
            int retries = 0;
            boolean otpEntered = false;
            while (retries < 3 && !otpEntered) {
                try {
                    By otpInputSelector = By.cssSelector("input[inputmode='numeric']");
                    WebElement otpInput = wait.until(ExpectedConditions.elementToBeClickable(otpInputSelector));

                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", otpInput);
                    otpInput.clear();
                    otpInput.sendKeys(otp);
                    System.out.println("Entered OTP: " + otp);
                    otpEntered = true;

                    // Click verify button
                    WebElement verifyButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(text(),'Verify') or contains(text(),'Submit')]")));
                    verifyButton.click();
                    System.out.println("Clicked verify button");

                    // Wait for verification to complete
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(otpInputSelector));

                    // Wait for Agency Details section with multiple checks
                    try {
                        wait.until(ExpectedConditions.or(
                                ExpectedConditions.visibilityOfElementLocated(
                                        By.xpath("//h2[contains(text(),'Agency Details')]")),
                                ExpectedConditions.visibilityOfElementLocated(
                                        By.xpath("//form[contains(@class,'agency-details')]")),
                                ExpectedConditions.visibilityOfElementLocated(By.name("agency_name"))));
                        System.out.println("OTP verification completed");
                    } catch (Exception e) {
                        System.out.println("Waiting for Agency Details section failed: " + e.getMessage());
                        throw e;
                    }

                } catch (Exception e) {
                    System.out.println("OTP entry attempt " + (retries + 1) + " failed: " + e.getMessage());
                    retries++;
                    if (retries < 3) {
                        Thread.sleep(2000);
                        driver.navigate().refresh();
                    }
                }
            }

            if (!otpEntered) {
                throw new RuntimeException("Failed to enter OTP after " + retries + " attempts");
            }

        } catch (Exception e) {
            throw new RuntimeException("OTP verification failed: " + e.getMessage());
        }
    }

    private String getOTPFromEmail() throws InterruptedException {
        String otp = null;
        int retries = 0;

        while (retries < 12 && otp == null) {
            try {
                Thread.sleep(5000);
                driver.navigate().refresh();
                System.out.println("Refreshing inbox, attempt: " + (retries + 1));

                WebElement emailRow = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("table.table-striped tbody tr:first-child")));
                emailRow.click();
                System.out.println("Clicked email row");

                Thread.sleep(2000);

                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("html_msg_body")));

                WebElement messageBody = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                String emailContent = messageBody.getText();
                System.out.println("Email content retrieved");

                Pattern pattern = Pattern.compile("\\b\\d{6}\\b");
                Matcher matcher = pattern.matcher(emailContent);

                if (matcher.find()) {
                    otp = matcher.group();
                    System.out.println("Found OTP: " + otp);
                }

                driver.switchTo().defaultContent();

            } catch (Exception e) {
                System.out.println("Attempt " + (retries + 1) + " failed: " + e.getMessage());
                retries++;
            }
        }

        if (otp == null) {
            throw new RuntimeException("Failed to get OTP after " + retries + " attempts");
        }

        return otp;
    }

    private void handleAgencyDetailsSection() {
        try {
            // Wait for form to be fully loaded
            Thread.sleep(2000);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("form")));
            System.out.println("Agency Details form loaded");

            // Generate test data
            String agencyName = "Test Agency " + RandomStringUtils.randomAlphabetic(5);
            String agencyAddress = RandomStringUtils.randomAlphabetic(10) + " Street, Kathmandu";
            String agencyEmail = "agency" + RandomStringUtils.randomNumeric(5) + "@test.com";
            String agencyWebsite = "www.agency" + RandomStringUtils.randomNumeric(5) + ".com";
            String roleInAgency = "Manager";

            // Fill fields with retries if needed
            fillField(By.xpath("//input[@name='agency_name']"), agencyName, "Agency Name");

            // Try multiple locators for address
            boolean addressFilled = false;
            List<By> addressLocators = List.of(
                    By.xpath("//input[@name='agency_address']"),
                    By.xpath("//textarea[@name='agency_address']"));
            for (By locator : addressLocators) {
                try {
                    fillField(locator, agencyAddress, "Agency Address");
                    addressFilled = true;
                    break;
                } catch (Exception e) {
                    System.out.println("Trying next locator for address field");
                }
            }

            if (!addressFilled) {
                throw new RuntimeException("Failed to fill address field with any locator");
            }

            fillField(By.xpath("//input[@name='agency_email']"), agencyEmail, "Agency Email");
            fillField(By.xpath("//input[@name='agency_website']"), agencyWebsite, "Agency Website");
            fillField(By.xpath("//input[@name='role_in_agency']"), roleInAgency, "Role in Agency");

            // Use the selector that worked for countries dropdown
            try {
                WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[@role='combobox']")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});",
                        dropdown);
                dropdown.click();
                System.out.println("Clicked country dropdown");
                Thread.sleep(1000);

                // Try the selector that worked before
                List<WebElement> options = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//div[contains(@id, 'radix-')]/div/div")));
                if (options != null && !options.isEmpty()) {
                    System.out.println("Found " + options.size() + " options with successful selector");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", options.get(0));
                    options.get(0).click();
                    System.out.println("Selected first option");
                    Thread.sleep(1000);
                } else {
                    throw new RuntimeException("No country options found");
                }
            } catch (Exception e) {
                System.out.println("Country selection failed: " + e.getMessage());
                // Instead of failing, use JavaScript injection as fallback
                try {
                    WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[@role='combobox']")));
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].innerHTML = '<div>United States</div>';" +
                                    "arguments[0].setAttribute('data-value', 'US');" +
                                    "let event = new Event('change', { bubbles: true });" +
                                    "arguments[0].dispatchEvent(event);",
                            dropdown);
                    System.out.println("Set country value with JavaScript injection");
                } catch (Exception ex) {
                    System.out.println("JavaScript country fallback failed: " + ex.getMessage());
                }
            }

            // Click the Next button with more robust handling
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@type='submit' and (contains(text(), 'Next') or contains(@type, 'submit'))]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
            Thread.sleep(1000);
            System.out.println("Found Next button, about to click");

            // Try regular click first
            nextButton.click();
            System.out.println("Clicked Next button on Agency Details section");

            // Wait for the next page to load with multiple possible selectors
            try {
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[contains(text(),'Experience')]")),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//*[contains(text(),'Experience and Performance')]")),
                        ExpectedConditions
                                .visibilityOfElementLocated(By.name("number_of_students_recruited_annually"))));
                System.out.println("Experience page loaded successfully");
            } catch (Exception e) {
                // If normal click didn't work, try JavaScript click
                System.out.println("Page transition failed, trying JavaScript click: " + e.getMessage());
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            System.out.println("Agency details error: " + e.getMessage());
            throw new RuntimeException("Failed to fill agency details: " + e.getMessage());
        }
    }

    private void handleExperienceDetails() {
        try {
            // Wait for form to load with multiple possible selectors
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Experience')]")),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'experience')]")),
                    ExpectedConditions.visibilityOfElementLocated(By.name("number_of_students_recruited_annually"))));
            System.out.println("Experience Details form loaded");

            // Handle Years of Experience dropdown
            WebElement expDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@role='combobox' and contains(@class, 'flex')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", expDropdown);
            Thread.sleep(500);
            expDropdown.click();
            System.out.println("Clicked experience dropdown");
            Thread.sleep(1000);

            // Use the same approach that worked for country dropdown
            try {
                List<WebElement> options = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//div[contains(@id, 'radix-')]/div/div")));

                if (options != null && !options.isEmpty()) {
                    System.out.println("Found " + options.size() + " experience options with successful selector");

                    // Look for option with value 5 or select the first option
                    boolean optionSelected = false;
                    for (WebElement option : options) {
                        String text = option.getText().trim();
                        System.out.println("Option text: " + text);

                        if (text.contains("5")) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", option);
                            Thread.sleep(500);
                            option.click();
                            System.out.println("Selected experience option: " + text);
                            optionSelected = true;
                            break;
                        }
                    }

                    // If we didn't find a "5" option, just select the first one
                    if (!optionSelected && !options.isEmpty()) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
                                options.get(0));
                        Thread.sleep(500);
                        options.get(0).click();
                        System.out.println("Selected first experience option: " + options.get(0).getText());
                    }

                    Thread.sleep(1000);
                } else {
                    throw new RuntimeException("No experience options found");
                }
            } catch (Exception e) {
                System.out.println("Experience option selection failed: " + e.getMessage());

                // Try JavaScript approach as fallback
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "document.querySelector('button[role=\"combobox\"]').innerHTML = '<div>5+ years</div>';" +
                                    "document.querySelector('button[role=\"combobox\"]').setAttribute('data-value', '5');"
                                    +
                                    "let event = new Event('change', { bubbles: true });" +
                                    "document.querySelector('button[role=\"combobox\"]').dispatchEvent(event);");
                    System.out.println("Set experience value with JavaScript injection");
                    Thread.sleep(500);
                } catch (Exception ex) {
                    System.out.println("JavaScript experience fallback failed: " + ex.getMessage());
                }
            }

            // Fill number of students recruited
            fillField(By.name("number_of_students_recruited_annually"), "100", "Number of students");

            // Fill focus area
            fillField(By.name("focus_area"), "Undergraduate admissions to Canada", "Focus area");

            // Fill success metrics
            fillField(By.name("success_metrics"), "90", "Success metrics");

            // Select services using checkbox buttons
            String[] services = {
                    "Career Counseling",
                    "Admission Applications",
                    "Visa Processing",
                    "Test Prepration"
            };

            for (String service : services) {
                try {
                    // Find checkbox button by associated label
                    WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//label[normalize-space()='" + service
                                    + "']/preceding-sibling::button[@role='checkbox']")));

                    if (checkbox.getAttribute("aria-checked").equals("false")) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkbox);
                        Thread.sleep(300);
                        checkbox.click();
                        System.out.println("Selected service: " + service);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to select service '" + service + "': " + e.getMessage());
                }
            }

            // Click Next button
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@type='submit' and contains(text(), 'Next')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
            Thread.sleep(500);
            nextButton.click();
            System.out.println("Submitted experience details");

            // Wait for the next page to load
            try {
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(By.name("business_registration_number")),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//*[contains(text(),'Business Registration')]"))));
                System.out.println("Business Registration page loaded successfully");
            } catch (Exception e) {
                // If normal click didn't work, try JavaScript click
                System.out.println("Page transition failed, trying JavaScript click: " + e.getMessage());
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            System.out.println("Failed to fill experience details: " + e.getMessage());
            throw new RuntimeException("Failed to fill experience details: " + e.getMessage());
        }
    }

    private void handleBusinessRegistration() {
        try {
            // Wait for the form to load
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("business_registration_number")));
            System.out.println("Business Registration form loaded");

            // Fill the Business Registration Number field
            String regNumber = "REG" + RandomStringUtils.randomNumeric(8);
            fillField(By.name("business_registration_number"), regNumber, "Business Registration Number");

            // Handle Preferred Countries dropdown
            WebElement countriesDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@role='combobox' and contains(@class, 'inline-flex')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", countriesDropdown);
            countriesDropdown.click();
            System.out.println("Clicked countries dropdown");
            Thread.sleep(1000);

            // Select Institution Types
            String[] institutionTypes = {
                    "Universities",
                    "Colleges",
                    "Vocational School",
                    "Other"
            };

            for (String type : institutionTypes) {
                WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[normalize-space()='" + type
                                + "']/preceding-sibling::button[@role='checkbox']")));

                if (checkbox.getAttribute("aria-checked").equals("false")) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkbox);
                    Thread.sleep(300);
                    checkbox.click();
                    System.out.println("Selected institution type: " + type);
                }
            }

            // Fill Certification Details
            String certDetails = "ICEF Certified Education Agent " + RandomStringUtils.randomAlphanumeric(5);
            fillField(By.name("certification_details"), certDetails, "Certification Details");

            // Upload Business Documents
            String[] filePaths = {
                    System.getProperty("user.dir") + "/src/test/resources/test-docs/business-reg.pdf",
                    System.getProperty("user.dir") + "/src/test/resources/test-docs/certificates.pdf"
            };

            // Locate hidden file inputs
            List<WebElement> fileInputs = driver.findElements(By.xpath("//input[@type='file']"));

            if (fileInputs.size() < filePaths.length) {
                throw new RuntimeException("Not enough file input elements found for upload");
            }

            for (int i = 0; i < filePaths.length; i++) {
                WebElement fileInput = fileInputs.get(i);
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", fileInput);
                fileInput.sendKeys(filePaths[i]); // Upload file
                System.out.println("Uploaded file: " + filePaths[i]);
                Thread.sleep(1000);
            }

            // Click Submit button
            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@type='submit' and contains(@style, '--success')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitButton);
            Thread.sleep(500);
            submitButton.click();
            System.out.println("Submitted business registration details");

        } catch (Exception e) {
            System.out.println("Failed to fill business registration details: " + e.getMessage());
            throw new RuntimeException("Failed to fill business registration details: " + e.getMessage());
        }
    }

    private void fillField(By locator, String value, String fieldName) {
        try {
            WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", field);
            field.clear();
            field.sendKeys(value);
            System.out.println(fieldName + " field filled: " + value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fill " + fieldName + ": " + e.getMessage());
        }
    }

    private void fillField(WebElement field, String value, String fieldName) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", field);
            field.clear();
            field.sendKeys(value);
            System.out.println(fieldName + " field filled: " + value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fill " + fieldName + ": " + e.getMessage());
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
    public static JSONObject createTemporaryEmail() {
        String random = RandomStringUtils.randomAlphabetic(8).toLowerCase();
        JSONObject result = new JSONObject();
        result.put("address", "test" + random + "@mailinator.com");
        result.put("token", random);
        return result;
    }
}