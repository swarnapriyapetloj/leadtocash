package com.salesforce.l2c.tests;

import com.salesforce.l2c.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Smoke test: confirms login succeeded by checking we reached a Lightning URL. */
public class LoginTest extends BaseTest {

    @Test(description = "Verify Salesforce login is successful")
    public void loginShouldSucceed() {
        Assert.assertTrue(driver.getCurrentUrl().contains(".lightning.force.com"),
                "Expected to be on a Lightning page after login. Current URL: " + driver.getCurrentUrl());
    }
}
