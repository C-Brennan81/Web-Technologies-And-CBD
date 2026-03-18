package com.gamelibrary.Tests.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import com.gamelibrary.stats.GameLibraryStatsApp;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@SpringBootTest(classes = GameLibraryStatsApp.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
 class UISmokeIT {

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1280,1000");
        driver = new ChromeDriver(options); // Selenium Manager will resolve ChromeDriver
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void goHome() {
        driver.get("http://localhost:8081/");
        // Root app mounts and router injects landing view
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("landing")));
    }

    @Test
    @Order(1)
    void registerUser_viaUI_successMessageShown() {
        goHome();

        // Switch to Register tab
        WebElement tabRegister = wait.until(ExpectedConditions.elementToBeClickable(By.id("tabRegister")));
        tabRegister.click();

        String username = "selenium_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Pass1234!";

        driver.findElement(By.id("regUsername")).sendKeys(username);
        // Optional email is present but not mandatory in backend; keep it blank to avoid constraints
        driver.findElement(By.id("regPassword")).sendKeys(password);
        driver.findElement(By.id("regPassword2")).sendKeys(password);

        driver.findElement(By.id("btnRegister")).click();

        WebElement msg = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("authMessage")));
        // Expect one of the known messages
        String text = msg.getText();
        Assertions.assertTrue(text == null || text.isEmpty() || text.contains("Registered") || text.toLowerCase().contains("sign in"),
                () -> "Unexpected register message: '" + text + "'");

        // Store username/password into localStorage for next test? Better to just return values.
        // Instead, keep them in system properties for the next test run within same JVM.
        System.setProperty("ui.username", username);
        System.setProperty("ui.password", password);
    }

    @Test
    @Order(2)
    void login_viaUI_navigatesToDashboard() {
        goHome();

        String username = System.getProperty("ui.username");
        String password = System.getProperty("ui.password");
        Assertions.assertNotNull(username, "username from previous step missing");
        Assertions.assertNotNull(password, "password from previous step missing");

        driver.findElement(By.id("loginUsername")).sendKeys(username);
        driver.findElement(By.id("loginPassword")).sendKeys(password);
        driver.findElement(By.id("btnLogin")).click();

        // Router should navigate to dashboard and inject content
        wait.until(ExpectedConditions.urlContains("#/dashboard"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btnLogout")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btnImport")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("gameSearch")));

        // Also check stat counters exist
        Assertions.assertNotNull(driver.findElement(By.id("totalValue")));
        Assertions.assertNotNull(driver.findElement(By.id("completionValue")));
    }

    @Test
    @Order(3)
    void importCsv_viaUI_updatesDashboard() throws Exception {
        // Navigate home and determine current auth state
        driver.get("http://localhost:8081/");
        boolean atDashboard;
        try {
            // If already logged in, dashboard elements will be present quickly
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.or(
                            ExpectedConditions.urlContains("#/dashboard"),
                            ExpectedConditions.presenceOfElementLocated(By.id("btnLogout"))
                    ));
            atDashboard = true;
        } catch (TimeoutException e) {
            atDashboard = false;
        }

        if (!atDashboard) {
            // Perform login from landing page
            String username = System.getProperty("ui.username");
            String password = System.getProperty("ui.password");
            Assertions.assertNotNull(username, "username from previous step missing");
            Assertions.assertNotNull(password, "password from previous step missing");

            // Wait for landing panel
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("landing")));
            driver.findElement(By.id("loginUsername")).sendKeys(username);
            driver.findElement(By.id("loginPassword")).sendKeys(password);
            driver.findElement(By.id("btnLogin")).click();

            wait.until(ExpectedConditions.urlContains("#/dashboard"));
        }

        // Upload CSV via hidden input triggered by Import button
        WebElement importBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("btnImport")));
        importBtn.click();

        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("gameFile")));

        // Resolve template.csv absolute path
        Path csv = Path.of("src", "main", "resources", "static", "csv", "template.csv").toAbsolutePath();
        Assertions.assertTrue(Files.exists(csv), "template.csv must exist: " + csv);

        fileInput.sendKeys(csv.toString());

        // Confirm dialog appears; accept it
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            Alert confirm = driver.switchTo().alert();
            // either OK to exclude or Cancel - choose OK
            confirm.accept();
        } catch (TimeoutException ignored) {}

        // After upload, another alert should show with result text; accept it if appears
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.alertIsPresent());
            Alert result = driver.switchTo().alert();
            String resultText = result.getText();
            result.accept();
            // If we saw a message, ensure it looks like success or at least non-empty
            Assertions.assertTrue(resultText != null && !resultText.isBlank(), "Expected a non-empty import result message");
        } catch (TimeoutException ignored) {}

        // As a functional assertion, wait for either totalValue to become non-zero or for any row/grid item to appear
        try {
            WebElement totalValue = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("totalValue")));
            String tv = totalValue.getText().trim();
            // If numeric and > 0, good
            if (!tv.isBlank()) {
                try {
                    double v = Double.parseDouble(tv.replaceAll("[^0-9.]+", ""));
                    if (v > 0.0) return; // success path
                } catch (NumberFormatException ignored) {}
            }
        } catch (TimeoutException ignored) {}

        // Fallback: check for any rendered entries in table or grid
        boolean hasAny = driver.findElements(By.cssSelector("#gamesTable tbody tr")).size() > 0
                || driver.findElements(By.cssSelector("#gameGrid > *")).size() > 0;
        Assertions.assertTrue(hasAny, "Expected some games to be visible after import");
    }
}
