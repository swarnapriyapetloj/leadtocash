package com.salesforce.l2c.base;

import com.salesforce.l2c.config.ConfigReader;
import com.salesforce.l2c.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.net.URI;
import java.time.Duration;

/**
 * Spins up a fresh browser, logs into Salesforce, and computes the org's
 * Lightning base URL (e.g. https://yourorg.lightning.force.com) so page objects
 * can navigate directly. One driver per @Test method.
 */
public abstract class BaseTest {

    protected WebDriver driver;
    protected String lightningBase;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        String browser = ConfigReader.get("browser");
        boolean headless = Boolean.parseBoolean(ConfigReader.get("headless"));

        if ("firefox".equalsIgnoreCase(browser)) {
            FirefoxOptions opts = new FirefoxOptions();
            if (headless) opts.addArguments("-headless");
            driver = new FirefoxDriver(opts);          // Selenium Manager resolves geckodriver
        } else {
        	ChromeOptions opts = new ChromeOptions();
        	opts.addArguments("--remote-allow-origins=*");
        	opts.addArguments("--user-data-dir=C:\\selenium-chrome-profile");
        	if (headless) opts.addArguments("--headless=new");
        	driver = new ChromeDriver(opts);          // Selenium Manager resolves chromedriver
        }

        // Explicit-wait-only strategy: do NOT set an implicit wait (mixing the two is unreliable).
        driver.manage().window().maximize();

        // Log in
        driver.get(ConfigReader.get("baseUrl"));
        new LoginPage(driver).login(ConfigReader.get("sfUsername"), ConfigReader.get("sfPassword"));

        // Wait for Lightning to finish loading, then derive the org base URL
        new WebDriverWait(driver, Duration.ofSeconds(60))
                .until(ExpectedConditions.urlContains(".lightning.force.com"));
        lightningBase = origin(driver.getCurrentUrl());
    }

    private String origin(String url) {
        try {
            URI u = URI.create(url);
            return u.getScheme() + "://" + u.getAuthority();
        } catch (Exception e) {
            return url;
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
