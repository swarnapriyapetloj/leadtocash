package com.salesforce.l2c.pages;

import com.salesforce.l2c.base.BasePage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;

public class QuotePage extends BasePage {
    private final String lightningBase;
    public QuotePage(WebDriver driver, String lightningBase) {
        super(driver); this.lightningBase = lightningBase;
    }
    private WebElement visible(By loc, int sec) {
        return new WebDriverWait(driver, Duration.ofSeconds(sec))
            .ignoring(StaleElementReferenceException.class)
            .until(d -> { for (WebElement e : d.findElements(loc)) {
                try { if (e.isDisplayed() && e.isEnabled()) return e; } catch (Exception ig) {} }
                return null; });
    }
    private void clickEl(WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", el);
    }

    public void createQuoteFromOpportunity(String oppId, String quoteName) {
        driver.get(lightningBase + "/lightning/r/Opportunity/" + oppId + "/related/Quotes/view");
        clickEl(visible(By.xpath(
            "//button[normalize-space()='New Quote'] | //a[@title='New Quote'] | //div[@title='New Quote']"), 20));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        WebElement name = visible(By.xpath(
            "//label[contains(normalize-space(),'Quote Name')]/following-sibling::div//input"), 15);
        name.click(); name.clear(); name.sendKeys(quoteName); name.sendKeys(Keys.TAB);
        clickEl(visible(By.xpath(
            "//button[@name='SaveEdit'] | //button[normalize-space()='Save']"), 10));
        waitForToast();
    }
}