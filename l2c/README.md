# Selenium + Salesforce: Lead-to-Cash Automation Framework

A Maven + Java + Selenium 4 + TestNG framework that automates the standard
Sales Cloud **Lead-to-Cash** journey using the Page Object Model (POM) and
produces an ExtentReports HTML report.

```
Lead -> Convert -> Account + Contact + Opportunity
     -> Add Products (Price Book) -> Quote -> Closed Won -> Order -> Activate
```

---

## Tech stack

| Layer        | Choice                                  |
|--------------|-----------------------------------------|
| Language     | Java 17                                 |
| Build        | Maven                                   |
| Automation   | Selenium 4 (Selenium Manager auto-driver)|
| Test runner  | TestNG                                   |
| Reporting    | ExtentReports (Spark HTML)               |
| Design       | Page Object Model, explicit waits only   |

---

## PHASE 1 - Prepare a Salesforce org (one-time)

1. **Get a free Developer Edition org**: https://developer.salesforce.com/signup
   (full org, never expires, safe to break).
2. **Enable the L2C objects** in *Setup* (Quick Find):
   - *Quote Settings* -> Enable Quotes.
   - *Order Settings* -> Enable Orders and Negative Quantities (optional).
3. **Create test data so products can be sold**:
   - Create a **Product** (e.g. "GenWatt Diesel 1000kW"), mark it Active.
   - Open the **Standard Price Book** -> Add that product with an active
     **list price**. (No PricebookEntry = nothing to add on the Opportunity.)
4. **Add related lists to layouts**: on the Opportunity (and/or Account) page
   layout, add the **Quotes** and **Orders** related lists.
5. **Make the login automatable** (avoid MFA/identity-verification prompts):
   - Create a dedicated automation user, OR use your own.
   - On that user's **Profile -> Login IP Ranges**, add your machine's IP so
     Salesforce trusts it and skips the email verification code. (For CI you
     relax this for the runner's IP, or use a connected app for API-based login.)
   - Note the password; if your org forces MFA you may need to set
     **Session Settings** / use a permission set that allows the IP-relaxed login.

> Why this matters: Salesforce will block a brand-new IP with an "Verify your
> identity" screen. Relaxing the login IP range for the test user is the standard
> way to keep Selenium UI logins working.

---

## PHASE 2 - Local environment (one-time)

1. **JDK 17** -> `java -version` should print 17.
2. **Maven** -> `mvn -version`.
3. **Google Chrome** installed (Selenium Manager downloads the matching driver
   automatically - no manual chromedriver setup).
4. **IDE**: IntelliJ IDEA or VS Code (with Java + Maven extensions).

---

## PHASE 3 - Configure the project

1. Open the project folder in your IDE (import as a Maven project).
2. Edit `src/test/resources/config.properties`:
   ```properties
   baseUrl=https://login.salesforce.com   # test.salesforce.com for sandboxes
   sfUsername=your.user@yourorg.com
   sfPassword=YourPasswordHere
   browser=chrome
   headless=false
   ```
   For security, prefer passing secrets at runtime instead of committing them:
   ```bash
   mvn test -DsfUsername=you@org.com -DsfPassword=secret
   ```
3. In `LeadToCashTest.java`, change the product name in `addProduct(...)` to a
   product that exists in **your** Standard Price Book.

---

## PHASE 4 - Run

```bash
# whole suite (login smoke test + full L2C flow)
mvn clean test

# just the smoke test
mvn clean test -Dtest=LoginTest

# headless
mvn clean test -Dheadless=true
```

Open the report at: `target/extent-report/index.html`

---

## PHASE 5 - How it's organized

```
src/main/java/com/salesforce/l2c
  config/ConfigReader.java     # reads config.properties (+ -D overrides)
  base/BasePage.java           # waits + Lightning helpers (setText, selectPicklist, ...)
  pages/                       # one Page Object per area
    LoginPage, LeadPage, OpportunityPage, QuotePage, OrderPage

src/test/java/com/salesforce/l2c
  base/BaseTest.java           # driver setup, login, derive org URL, teardown
  tests/LoginTest.java         # smoke test
  tests/LeadToCashTest.java    # end-to-end L2C flow
  listeners/ExtentReportListener.java
testng.xml                     # suite definition + listener
```

**Page Object Model**: every screen is a class exposing business actions
(`createLead`, `convertLead`, `addProduct`...). Tests read like the process and
never touch raw locators - so when Salesforce changes a screen you fix it in
exactly one place.

---

## PHASE 6 - Salesforce automation gotchas (important)

Salesforce auto-generates much of its HTML and it changes between releases and
orgs. This framework already uses the most stable anchors, but you should still
verify locators in your org with DevTools (F12). Key techniques used here:

- **Navigate by URL**, not the App Launcher: `/lightning/o/Lead/list`.
- **Anchor inputs to visible labels**: `//label[.='Last Name']/following-sibling::div//input`.
- **Use stable attributes**: the Save button is `button[name='SaveEdit']`;
  picklist options carry `data-value`.
- **Explicit waits only** - never `Thread.sleep`, never mix implicit + explicit.
- **JS click** when an overlay intercepts a normal click (`BasePage.jsClick`).
- Some components live in **iframes** (Visualforce) or **shadow DOM** (a few
  LWCs) - switch frames / use `getShadowRoot()` if you hit one.

If a step can't find an element, open that screen manually, inspect the real
markup, and update the locator in the relevant Page Object.

---

## PHASE 7 - Where to take it next

- **Data-driven**: feed lead/product data from a `@DataProvider`, Excel, or JSON.
- **Hybrid setup**: create prerequisite records via the Salesforce REST/Bulk API
  (fast, reliable) and reserve Selenium for genuine UI validation. This is the
  professional pattern - pure UI E2E on Salesforce is slow and brittle.
- **CI**: run `mvn test -Dheadless=true` in GitHub Actions; publish the Extent
  report as an artifact.
- **Parallel**: TestNG `parallel="methods"` + a ThreadLocal<WebDriver>.
- **Screenshots on failure**: capture in `ITestListener.onTestFailure` and embed
  in the Extent report.
