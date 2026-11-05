package com.salesforce.l2c.tests;

import com.salesforce.l2c.base.BaseTest;
import com.salesforce.l2c.pages.LeadPage;
import com.salesforce.l2c.pages.OpportunityPage;
import com.salesforce.l2c.pages.OrderPage;
import com.salesforce.l2c.pages.QuotePage;
import org.testng.annotations.Test;

/**
 * Full end-to-end Lead-to-Cash flow:
 *   Lead -> Convert -> Opportunity (+Product) -> Closed Won -> Quote -> Order -> Activate
 */
public class LeadToCashTest extends BaseTest {

    @Test(description = "Full Lead-to-Cash happy path")
    public void leadToCashEndToEnd() {
        long stamp = System.currentTimeMillis();
        String company  = "Acme Steel " + stamp;
        String lastName = "Tester" + stamp;

        // 1. LEAD + CONVERT (lands on the new Opportunity)
        LeadPage lead = new LeadPage(driver, lightningBase);
        lead.createLead("Ms.", "Auto", lastName, company, "Open - Not Contacted");
        lead.convertLead();

        // 2. OPPORTUNITY: capture id, add product, close won
        OpportunityPage opp = new OpportunityPage(driver, lightningBase);
        String oppId = opp.getCurrentOpportunityId();
        opp.addProduct("GenWatt Diesel 1000kW", "100");  // product in your Standard Price Book
        opp.markClosedWon(oppId);

        // 3. QUOTE
        QuotePage quote = new QuotePage(driver, lightningBase);
        quote.createQuoteFromOpportunity(oppId, "Quote-" + stamp);

        // 4. ORDER + ACTIVATE (the "Cash" step) — created from the Account
        OrderPage order = new OrderPage(driver, lightningBase);
        order.createAndActivateOrder(oppId, "1/1/2026");
    }
}