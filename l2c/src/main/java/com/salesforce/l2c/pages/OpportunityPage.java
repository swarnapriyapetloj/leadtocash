package com.salesforce.l2c.pages;

import com.salesforce.l2c.base.BasePage;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.regex.*;

public class OpportunityPage extends BasePage {

    private final String lightningBase;

    public OpportunityPage(WebDriver driver, String lightningBase) {
        super(driver);
        this.lightningBase = lightningBase;
    }

    public String getCurrentOpportunityId() {
        new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.urlContains("/lightning/r/Opportunity/"));
        Matcher m = Pattern.compile("/Opportunity/([a-zA-Z0-9]{15,18})")
            .matcher(driver.getCurrentUrl());
        if (m.find()) return m.group(1);
        throw new RuntimeException("No Opp id in URL: " + driver.getCurrentUrl());
    }

    private WebElement visible(By locator, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds))
            .ignoring(StaleElementReferenceException.class)
            .until(d -> {
                for (WebElement el : d.findElements(locator)) {
                    try { if (el.isDisplayed() && el.isEnabled()) return el; }
                    catch (Exception ignored) {}
                }
                return null;
            });
    }

    private void clickEl(WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", el);
    }

    public void addProduct(String productName, String quantity) {
        String oppId = getCurrentOpportunityId();
        driver.get(lightningBase + "/lightning/r/Opportunity/" + oppId +
                   "/related/OpportunityLineItems/view");

        clickEl(visible(By.xpath(
            "//button[normalize-space()='Add Products'] | //a[@title='Add Products'] " +
            "| //div[@title='Add Products']"), 20));

        By anySave = By.xpath("//button[contains(@class,'test-saveButton') " +
            "or normalize-space()='Save' or .//span[normalize-space()='Save']]");
        try { clickEl(visible(anySave, 10)); } catch (Exception noModal) {}

        // Search (no ENTER), dismiss autocomplete
        WebElement search = visible(
            By.xpath("//input[contains(@placeholder,'Search Products')]"), 20);
        search.click(); search.clear(); search.sendKeys(productName);
        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
        search.sendKeys(Keys.ESCAPE);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Focus checkbox + SPACE (trusted toggle), Actions-click fallback
        WebElement cb = new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//tr[.//a[normalize-space()='" + productName + "']]//input[@type='checkbox']")));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", cb);
        cb.sendKeys(Keys.SPACE);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> cb.isSelected());
        } catch (Exception e) {
            new Actions(driver).moveToElement(cb).click().perform();
        }

        // Wait Next enabled, click it
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            for (WebElement b : d.findElements(By.xpath(
                    "//button[normalize-space()='Next' or .//span[normalize-space()='Next']]")))
                try { if (b.isDisplayed() && b.isEnabled()) return true; } catch (Exception ig) {}
            return false;
        });
        clickEl(visible(By.xpath(
            "//button[normalize-space()='Next' or .//span[normalize-space()='Next']]"), 15));

        // EDIT SELECTED PRODUCTS: Quantity = the FIRST EMPTY visible textbox
        // (Sales Price is pre-filled; Date/Description come after Quantity).
     // EDIT SELECTED PRODUCTS grid is keyboard-navigable. After Next, focus is on
     // the (locked) Product cell. TAB moves to Quantity. Type into the focused
     // element — no locator hunting needed.
     // EDIT SELECTED PRODUCTS is an inline-edit virtual data table:
     // DOUBLE-CLICK the Quantity cell to activate its editor, then type.
     try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

     // The row containing our product
     WebElement row = visible(By.xpath(
         "//tr[.//a[normalize-space()='" + productName + "'] " +
         "or .//*[contains(normalize-space(),'" + productName + "')]]"), 15);

     // Find which column is Quantity from the headers, then grab that cell
     java.util.List<WebElement> headers = driver.findElements(By.xpath("//thead//th"));
     int qtyCol = -1;
     for (int i = 0; i < headers.size(); i++) {
         String t = headers.get(i).getText() + " " + headers.get(i).getAttribute("title")
                  + " " + headers.get(i).getAttribute("aria-label");
         if (t != null && t.contains("Quantity")) { qtyCol = i; break; }
     }
     java.util.List<WebElement> cells = row.findElements(By.xpath("./th | ./td"));
     WebElement qtyCell = (qtyCol >= 0 && qtyCol < cells.size())
             ? cells.get(qtyCol)
             : cells.get(2);   // fallback: 3rd cell (Item#, Product, Quantity)

     // Double-click to activate the inline editor
     new Actions(driver).moveToElement(qtyCell).doubleClick().perform();
     try { Thread.sleep(600); } catch (InterruptedException ignored) {}

     // Type into the editor input that appeared (or the focused element)
     WebElement editor;
     try {
         editor = qtyCell.findElement(By.xpath(".//input"));
     } catch (Exception e) {
         editor = driver.switchTo().activeElement();
     }
     editor.sendKeys(quantity);
     editor.sendKeys(Keys.TAB);   // commit the cell
     try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        clickEl(visible(anySave, 15));   // Save
        waitForToast();
    }

    public void markClosedWon(String oppId) {
        driver.get(lightningBase + "/lightning/r/Opportunity/" + oppId + "/view");
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // PLAN A: sales path — click "Closed Won" step, then the Mark button
        try {
            clickEl(visible(By.xpath(
                "//li[.//span[@title='Closed Won']] | //a[@title='Closed Won'] " +
                "| //span[@title='Closed Won']"), 10));
            clickEl(visible(By.xpath(
                "//button[contains(.,'Mark as Current Stage')] " +
                "| //button[contains(.,'Select Closed Stage')]"), 8));
            // a confirm dialog may ask for the closed stage — pick Closed Won + Save
            try {
                clickEl(visible(By.xpath("//button[contains(@class,'slds-combobox__input') " +
                    "or @role='combobox']"), 5));
                clickEl(visible(By.xpath("//*[@title='Closed Won' or normalize-space()='Closed Won']"), 5));
                clickEl(visible(By.xpath("//button[normalize-space()='Save' or normalize-space()='Done']"), 5));
            } catch (Exception noDialog) {}
            waitForToast();
            return;
        } catch (Exception pathFailed) {}

        // PLAN B: Edit modal — Stage combobox is a BUTTON trigger, not an input
        clickEl(visible(By.xpath("//button[@name='Edit'] | //button[normalize-space()='Edit']"), 10));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        clickEl(visible(By.xpath(
            "//label[contains(normalize-space(),'Stage')]/following-sibling::div//button " +
            "| //label[contains(normalize-space(),'Stage')]/following-sibling::div//input " +
            "| //lightning-combobox[.//label[contains(.,'Stage')]]//button"), 15));
        clickEl(visible(By.xpath(
            "//lightning-base-combobox-item[@data-value='Closed Won'] " +
            "| //*[@title='Closed Won'] | //span[normalize-space()='Closed Won']"), 10));
        clickEl(visible(By.xpath(
            "//button[@name='SaveEdit'] | //button[normalize-space()='Save']"), 10));
        waitForToast();
    }
}