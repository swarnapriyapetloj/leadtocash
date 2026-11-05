package com.salesforce.l2c.pages;

import com.salesforce.l2c.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebElement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.Duration;

/**
 * LEAD PAGE — create a Lead and convert it.
 * (Only createLead and convertLead shown; identical structure to before,
 *  with convertLead now navigating to the new Opportunity after conversion.)
 */
public class LeadPage extends BasePage {

    private final String lightningBase;

    private final By newButton = By.xpath(
        "//div[@title='New'] | //a[@title='New'] | //button[@name='New'] | //button[normalize-space()='New']");
    private final By modalContainer = By.cssSelector("div.slds-modal__container");

    public LeadPage(WebDriver driver, String lightningBase) {
        super(driver);
        this.lightningBase = lightningBase;
    }

    public void createLead(String salutation, String firstName,
                           String lastName, String company, String status) {
        driver.get(lightningBase + "/lightning/o/Lead/list?filterName=__Recent");
        click(newButton);
        new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.visibilityOfElementLocated(modalContainer));

        // Salutation & Lead Status are optional (Status defaults). We fill only required fields.
        setText("First Name", firstName);
        setText("Last Name",  lastName);   // required
        setText("Company",    company);    // required

        clickModalSave();
        waitForToast();
    }

    /**
     * Converts the Lead, then clicks the new Opportunity link in the success
     * modal so the test lands on the Opportunity record.
     */
    public void convertLead() {
        // Convert is a direct button in the Lead highlights panel
        By convertBtn = By.xpath(
            "//button[normalize-space()='Convert'] | //button[@name='Convert'] | //a[normalize-space()='Convert']");
        waitClickable(convertBtn);
        jsClick(convertBtn);

        // Convert Lead modal: accept defaults and confirm
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(
            ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[normalize-space()='Convert Lead']")));
        By confirm = By.xpath(
            "//div[contains(@class,'slds-modal__footer')]//button[normalize-space()='Convert'] " +
            "| //footer//button[normalize-space()='Convert'] " +
            "| (//button[normalize-space()='Convert'])[last()]");
        waitClickable(confirm);
        jsClick(confirm);

        // Success modal shows Account/Contact/Opportunity links — click the Opportunity
     // Wait for the "Your lead has been converted" success screen to appear
     // Wait for the "Your lead has been converted" success screen
     // Wait for conversion to finish — the success screen appears
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(normalize-space(),'lead has been converted')]")));

        // Go to the Opportunities list, sorted by most recently created (newest first)
        driver.get(lightningBase + "/lightning/o/Opportunity/list?filterName=Recent");

        // Sort by Created Date desc isn't guaranteed via URL, so instead open the
        // opportunity by its NAME, which we know = company name + "-".
        // Wait for the list grid, then click the first data row's name link.
        By firstRowNameLink = By.xpath(
            "//table[contains(@class,'slds-table')]//tbody//tr[1]//th//a " +
            "| //table[contains(@class,'slds-table')]//tbody//tr[1]//td//a[1] " +
            "| (//a[@data-refid='recordId'])[1]");
        WebElement row = new WebDriverWait(driver, Duration.ofSeconds(25))
            .until(ExpectedConditions.elementToBeClickable(firstRowNameLink));
        jsClick(row);

        new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.urlContains("/lightning/r/Opportunity/"));
    }
}