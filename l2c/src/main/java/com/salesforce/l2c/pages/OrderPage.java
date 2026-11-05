package com.salesforce.l2c.pages;

import com.salesforce.l2c.base.BasePage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.regex.*;

/**
 * ORDER PAGE — creates a Contract (required on this org's Order layout),
 * then creates and ACTIVATES an Order linked to it.
 *
 * Lookup strategy: type the text, wait, then press DOWN+ENTER to choose the
 * first suggestion via keyboard. This avoids hunting for a dropdown <li> whose
 * markup varies — keyboard selection is far more reliable on Lightning lookups.
 */
public class OrderPage extends BasePage {

    private final String lightningBase;

    public OrderPage(WebDriver driver, String lightningBase) {
        super(driver);
        this.lightningBase = lightningBase;
    }

    private WebElement visible(By loc, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
            .ignoring(StaleElementReferenceException.class)
            .until(d -> {
                for (WebElement e : d.findElements(loc)) {
                    try { if (e.isDisplayed() && e.isEnabled()) return e; } catch (Exception ig) {}
                }
                return null;
            });
    }

    private void clickEl(WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", el);
    }

    /** Lookup: type text, wait for suggestions, pick first via keyboard (DOWN, ENTER). */
    private void fillLookup(String label, String text) {
        WebElement box = visible(By.xpath(
            "//label[contains(normalize-space(),'" + label + "')]/following-sibling::div//input"), 12);
        box.click();
        box.clear();
        box.sendKeys(text);
        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
        box.sendKeys(Keys.ARROW_DOWN);
        box.sendKeys(Keys.ENTER);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
    }

    private void fillDate(String label, String value) {
        By loc = By.xpath(
            "//label[contains(normalize-space(),'" + label + "')]/following-sibling::div//input");
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement d = visible(loc, 12);
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", d);
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                d.clear();
                d.sendKeys(value);
                d.sendKeys(Keys.ESCAPE);
                return;
            } catch (StaleElementReferenceException stale) {
            } catch (ElementNotInteractableException notReady) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Could not fill date: " + label);
    }

    private void fillText(String label, String value) {
        By loc = By.xpath(
            "//label[contains(normalize-space(),'" + label + "')]/following-sibling::div//input");
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement t = visible(loc, 12);   // fresh find each attempt
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", t);
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                t.clear();
                t.sendKeys(value);
                t.sendKeys(Keys.TAB);
                return;   // success
            } catch (StaleElementReferenceException stale) {
                // DOM re-rendered — loop and re-find
            } catch (ElementNotInteractableException notReady) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Could not fill field: " + label);
    }

    private boolean isEmpty(String label) {
        try {
            WebElement el = driver.findElement(By.xpath(
                "//label[contains(normalize-space(),'" + label + "')]/following-sibling::div//input"));
            String v = el.getAttribute("value");
            return v == null || v.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void createAndActivateOrder(String oppId, String startDate) {

    	// 1. Account id + name from the Opportunity (robust: try link, then JS scan)
    	driver.get(lightningBase + "/lightning/r/Opportunity/" + oppId + "/view");
    	String accId = null, accName = "";
    	for (int i = 0; i < 6 && accId == null; i++) {
    	    try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
    	    Matcher m = Pattern.compile("/lightning/r/Account/([a-zA-Z0-9]{15,18})")
    	        .matcher(driver.getPageSource());   // scan whole rendered page source
    	    if (m.find()) accId = m.group(1);
    	}
    	if (accId == null) throw new RuntimeException("Account not found");
        // 2. Create a Contract on the Account
     // 2. Create a Contract — navigate straight to the New Contract page
        driver.get(lightningBase + "/lightning/o/Contract/new");
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.slds-modal__container")));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // Account is NOT pre-filled here (we didn't come from the Account), so always fill it
        fillLookup("Account Name", accName);
        fillDate("Contract Start Date", startDate);
        fillText("Contract Term", "12");

        clickEl(visible(By.xpath("//button[@name='SaveEdit'] | //button[normalize-space()='Save']"), 12));
        waitForToast();

        new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(ExpectedConditions.urlContains("/lightning/r/Contract/"));
        String contractNo = "";
        try {
            contractNo = visible(By.xpath(
                "//lightning-formatted-text | //*[contains(@class,'slds-page-header__title')]"), 8)
                .getText().trim();
        } catch (Exception ignored) {}

        // 3. Create the Order on the Opportunity's Orders related list
        driver.get(lightningBase + "/lightning/r/Opportunity/" + oppId + "/related/Orders/view");
        clickEl(visible(By.xpath(
            "//button[normalize-space()='New Order'] | //a[@title='New Order'] " +
            "| //button[normalize-space()='New']"), 20));
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.slds-modal__container")));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        fillDate("Order Start Date", startDate);
        if (isEmpty("Account Name")) fillLookup("Account Name", accName);
        fillLookup("Contract Number", contractNo.isEmpty() ? "00" : contractNo);

        clickEl(visible(By.xpath("//button[@name='SaveEdit'] | //button[normalize-space()='Save']"), 12));
        waitForToast();

        // 4. Activate the Order
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.urlContains("/lightning/r/Order"));
        } catch (Exception ignored) {}
        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}

        try {
            clickEl(visible(By.xpath(
                "//button[normalize-space()='Activate'] | //button[@name='Activate'] " +
                "| //a[normalize-space()='Activate']"), 15));
        } catch (Exception hidden) {
            clickEl(visible(By.xpath(
                "//button[@title='Show more actions'] | //button[@title='More Actions'] " +
                "| //lightning-button-menu//button"), 15));
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            clickEl(visible(By.xpath(
                "//a[@role='menuitem' and normalize-space()='Activate'] " +
                "| //*[@role='menuitem'][contains(normalize-space(),'Activate')]"), 15));
        }
        try { clickEl(visible(By.xpath(
            "//button[normalize-space()='Activate' or normalize-space()='Confirm']"), 5)); }
        catch (Exception ignored) {}
        waitForToast();
    }
}