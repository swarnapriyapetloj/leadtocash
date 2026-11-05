package com.salesforce.l2c.pages;

import com.salesforce.l2c.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The classic Salesforce login screen (stable ids: username / password / Login). */
public class LoginPage extends BasePage {

    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By loginButton = By.id("Login");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void login(String user, String pass) {
        type(username, user);
        type(password, pass);
        click(loginButton);
    }
}
