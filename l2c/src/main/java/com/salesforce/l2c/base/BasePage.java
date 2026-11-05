package com.salesforce.l2c.base;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;

import java.time.Duration;

/**
 * ============================================================
 *  BASE PAGE  —  Parent class for ALL Page Objects
 * ============================================================
 *
 * CONCEPT (Interview):
 *   BasePage follows the "Don't Repeat Yourself" (DRY) principle.
 *   Every page in Salesforce needs to click buttons, type text,
 *   wait for elements. Instead of writing those helpers in every
 *   page class, we write them ONCE here, and every page class
 *   "extends BasePage" to inherit them automatically.
 *
 * WHY EXPLICIT WAITS ONLY:
 *   Salesforce Lightning is a Single Page Application (SPA).
 *   Elements load asynchronously — the DOM changes after every
 *   action. Thread.sleep() is a blind pause. Explicit waits
 *   poll the DOM up to a max time and return the MOMENT the
 *   condition is true — faster and more reliable.
 *
 * SALESFORCE QUIRK — TWO TYPES OF DROPDOWNS:
 *   1. Lightning Combobox  → used by Lead Status, Stage, etc.
 *      Renders as a <button> that opens a listbox popup.
 *      → Use selectPicklist()
 *
 *   2. Native HTML <select> → used by Salutation (compound Name field)
 *      Renders as a standard browser dropdown.
 *      → Use selectNativeDropdown()
 *
 *   selectDropdown() tries BOTH automatically — use this when
 *   you're not sure which type a field uses.
 * ============================================================
 */
public abstract class BasePage {

    // ── Core fields every page needs ─────────────────────────
    protected final WebDriver      driver;
    protected final WebDriverWait  wait;
    protected final JavascriptExecutor js;

    // Default wait = 30 seconds. Long enough for slow Lightning
    // pages, short enough to fail fast when something is broken.
    private static final int WAIT_SECONDS = 30;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));
        this.js     = (JavascriptExecutor) driver;
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 1 — Core Wait Helpers
    //  These are the building blocks all other methods use.
    // ══════════════════════════════════════════════════════════

    /**
     * Waits until the element is VISIBLE on screen.
     * "Visible" = exists in DOM AND has non-zero size AND is not hidden.
     */
    protected WebElement waitVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until the element is CLICKABLE.
     * "Clickable" = visible AND enabled (not greyed out).
     * Always use this before clicking — avoids "element not interactable" errors.
     */
    protected WebElement waitClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 2 — Interaction Helpers
    // ══════════════════════════════════════════════════════════

    /**
     * Standard click — waits for clickable first.
     * Use this by default.
     */
    protected void click(By locator) {
        waitClickable(locator).click();
    }

    /**
     * JavaScript click — bypasses visibility and overlap checks.
     *
     * WHY THIS EXISTS:
     *   In Salesforce Lightning, overlays (spinners, dropdowns, modals)
     *   sometimes sit on top of an element. A normal click throws
     *   "ElementClickInterceptedException". JS click fires directly on
     *   the element in the DOM, bypassing whatever is on top.
     *   Use as a fallback when normal click fails.
     */
    protected void jsClick(By locator) {
        WebElement el = waitVisible(locator);
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); " +
                         "arguments[0].click();", el);
    }

    /**
     * Scrolls element into the centre of the viewport then JS-clicks it.
     * Needed when Salesforce fixed headers cover elements at the top of the page.
     */
    protected void jsClick(WebElement el) {
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); " +
                         "arguments[0].click();", el);
    }

    /**
     * Clears an input field then types the given text.
     * The scroll ensures the field is visible before typing.
     */
    protected void type(By locator, String text) {
        WebElement el = waitVisible(locator);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        el.clear();
        el.sendKeys(text);
    }

    /** Returns the visible text of an element. */
    protected String getText(By locator) {
        return waitVisible(locator).getText();
    }

    /**
     * Returns true if the element is visible right now.
     * Does NOT throw if the element doesn't exist — returns false instead.
     * Useful for conditional logic: "if this screen is showing, do X".
     */
    protected boolean isDisplayed(By locator) {
        try {
            // Short 3-second wait — we don't want to wait 30s just to check
            return new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 3 — Salesforce Lightning Form Helpers
    //
    //  IMPORTANT — WHY WE USE contains() NOT equals():
    //
    //  Salesforce injects a red asterisk * into required field labels:
    //    <label>* Last Name</label>
    //
    //  So exact XPath  text()='Last Name'  → NEVER matches
    //  But contains()  contains(.,'Last Name') → ALWAYS matches
    //
    //  This is the #1 most common Salesforce automation bug.
    // ══════════════════════════════════════════════════════════

    /**
     * Fills a text/textarea input on a Lightning record form by its visible label.
     *
     * XPath breakdown:
     *   //label[contains(normalize-space(),'Last Name')]
     *     → find the label whose text contains "Last Name" (handles the * prefix)
     *   /following-sibling::div
     *     → go to the div next to that label (they are siblings under lightning-input)
     *   //input
     *     → find the input inside that div
     */
    protected void setText(String label, String value) {
        By loc = By.xpath(
            "//label[contains(normalize-space(),'" + label + "')]/following-sibling::div//input " +
            "| //label[contains(normalize-space(),'" + label + "')]/following-sibling::div//textarea");
        WebElement el = waitVisible(loc);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        el.clear();
        el.sendKeys(value);
        el.sendKeys(Keys.TAB);   // KEY FIX: blur the field so Lightning commits the value
    }

    /**
     * Selects a value from a LIGHTNING COMBOBOX (button-based dropdown).
     * Used for: Lead Status, Opportunity Stage, and most picklist fields.
     *
     * How Lightning combobox works:
     *   Step 1: Click the trigger button → dropdown list appears
     *   Step 2: Click the option with matching data-value or visible text
     */
    protected void selectPicklist(String label, String value) {
        // The Lightning combobox trigger is an <input>, found like a text field
        By trigger = By.xpath(
            "//label[contains(normalize-space(),'" + label + "')]/following-sibling::div//input");
        WebElement triggerEl = waitClickable(trigger);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", triggerEl);
        triggerEl.click();   // opens the dropdown

        // Pick the option by its value/title
        By option = By.xpath(
            "//lightning-base-combobox-item[@data-value=\"" + value + "\"] " +
            "| //lightning-base-combobox-item[.//span[@title=\"" + value + "\"]] " +
            "| //span[@class='slds-truncate' and @title=\"" + value + "\"]");
        waitClickable(option).click();
    }

    /**
     * Selects a value from a NATIVE HTML <select> dropdown.
     * Used for: Salutation field (inside the compound Name field).
     *
     * Why Salutation is different:
     *   The Name field (Salutation + First + Last) is a compound field
     *   that predates Lightning's combobox component. It still uses a
     *   native browser <select> element. Selenium's Select class is
     *   designed specifically to handle <select> elements.
     */
    protected void selectNativeDropdown(String label, String visibleText) {
        By loc = By.xpath(
            "//label[contains(normalize-space(),'" + label + "')]" +
            "/following-sibling::div//select " +
            // Also handles cases where select is a direct sibling of label
            "| //label[contains(normalize-space(),'" + label + "')]" +
            "/following-sibling::select"
        );
        WebElement selectEl = waitVisible(loc);
        new Select(selectEl).selectByVisibleText(visibleText);
    }

    /**
     * Smart dropdown selector — tries native <select> first, then Lightning combobox.
     * Use this when you are not sure which type a field uses in your org.
     *
     * INTERVIEW TIP: This is a "fallback chain" pattern — resilient to
     * Salesforce UI differences across releases and org configurations.
     */
    protected void selectDropdown(String label, String value) {
        try {
            selectNativeDropdown(label, value);
            return; // worked — stop here
        } catch (Exception ignored) {
            // not a native select — try Lightning combobox next
        }
        selectPicklist(label, value);
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 4 — Button Helpers
    // ══════════════════════════════════════════════════════════

    /**
     * Clicks a button by its stable name= attribute.
     * Many Lightning modal buttons have: button[name='SaveEdit'], button[name='Cancel']
     */
    protected void clickButtonByName(String name) {
        click(By.cssSelector("button[name='" + name + "']"));
    }

    /**
     * Clicks a page-level action button (highlights panel, related list header)
     * by its title= attribute or visible text.
     * Uses jsClick because these buttons are sometimes partially covered.
     */
    protected void clickAction(String titleOrText) {
        By loc = By.xpath(
            "//button[@title='" + titleOrText + "'] " +
            "| //button[normalize-space()='" + titleOrText + "'] " +
            "| //a[@title='" + titleOrText + "']"
        );
        jsClick(loc);
    }

    /**
     * Clicks the Save button in a Lightning record modal.
     *
     * WHY A FALLBACK CHAIN:
     *   The Save button's name/title attribute varies between:
     *   - Standard edit modal   → name='SaveEdit'
     *   - Some custom layouts   → name='Save'
     *   - Some modals           → title='Save'
     *   - All of the above      → text 'Save'
     *
     *   We try each locator with a SHORT 3-second wait so we fail fast
     *   and move to the next option. The last one in the list is the
     *   broadest — matches any blue brand button in the modal footer.
     *
     *   IMPORTANT: Salesforce uses a <footer> tag (not <div>) for
     *   the modal footer — a common source of locator failures.
     */
    protected void clickModalSave() {
        By[] saveLocators = {
            // 1. Standard Lightning modal Save (most common)
            By.cssSelector("button[name='SaveEdit']"),

            // 2. Some layouts use name='Save'
            By.cssSelector("button[name='Save']"),

            // 3. title attribute
            By.xpath("//button[@title='Save']"),

            // 4. Exact text match — [last()] picks Save, not Save & New
            By.xpath("(//button[normalize-space()='Save'])[last()]"),

            // 5. Last resort — blue button in the modal footer
            //    NOTE: Salesforce uses <footer> not <div> for modal footer
            By.xpath(
                "(//footer[contains(@class,'slds-modal__footer')] " +
                "| //div[contains(@class,'slds-modal__footer')])" +
                "//button[contains(@class,'slds-button_brand')]"
            )
        };

        for (By locator : saveLocators) {
            try {
                WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.elementToBeClickable(locator));
                js.executeScript("arguments[0].click();", btn);
                return; // clicked successfully — stop trying
            } catch (Exception ignored) {
                // this locator didn't match — try next one
            }
        }

        // All locators failed — give a clear, actionable error
        throw new RuntimeException(
            "clickModalSave() could not find the Save button after trying all locators.\n" +
            "ACTION: Open the Lead New modal, press F12, inspect the Save button,\n" +
            "and check its name/title/class attributes, then add a locator here."
        );
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 5 — Page State Helpers
    // ══════════════════════════════════════════════════════════

    /**
     * Waits for the success toast that Salesforce shows after a save,
     * OR waits for the modal Save button to disappear (modal closed = saved).
     * Either condition means the save completed.
     */
    protected void waitForToast() {
        By successToast = By.cssSelector(".slds-notify_toast.slds-theme_success, .forceToastMessage .slds-theme_success");
        By errorToast   = By.cssSelector(".slds-notify_toast.slds-theme_error, .forceToastMessage .slds-theme_error");
        By saveButton   = By.cssSelector("button[name='SaveEdit'], button[name='Save']");
        By inlineError  = By.cssSelector(".slds-has-error");

        // Wait for something conclusive: success, error, or the modal closing
        wait.until(ExpectedConditions.or(
            ExpectedConditions.visibilityOfElementLocated(successToast),
            ExpectedConditions.visibilityOfElementLocated(errorToast),
            ExpectedConditions.visibilityOfElementLocated(inlineError),
            ExpectedConditions.invisibilityOfElementLocated(saveButton)
        ));

        // If an error is showing, the save FAILED — report the real reason
        if (isDisplayed(errorToast)) {
            throw new RuntimeException("SAVE FAILED — Salesforce error: " + safeText(errorToast));
        }
        if (isDisplayed(inlineError)) {
            throw new RuntimeException("SAVE FAILED — field validation error: " + safeText(inlineError));
        }
    }

    private String safeText(By locator) {
        try { return waitVisible(locator).getText().replaceAll("\\s+", " ").trim(); }
        catch (Exception e) { return "(could not read the error text)"; }
    }

    /** Navigates to a URL — shortcut so page objects can call open() instead of driver.get() */
    protected void open(String url) {
        driver.get(url);
    }
}