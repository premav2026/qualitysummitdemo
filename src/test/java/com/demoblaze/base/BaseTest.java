package com.demoblaze.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for all tests: owns the Playwright/Browser lifecycle.
 * A fresh BrowserContext + Page is created per test method so tests don't
 * leak state (cookies, localStorage cart data) into one another.
 */
public class BaseTest {

  protected static final String BASE_URL = "https://demoblaze.com/";

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  protected Page page;

  @BeforeClass
  public void launchBrowser() {
    playwright = Playwright.create();
    boolean headless = !"false".equalsIgnoreCase(System.getProperty("headless", "true"));
    browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(headless));
  }

  @BeforeMethod
  public void newContextAndPage() {
    context = browser.newContext();
    page = context.newPage();

    // Demoblaze uses JS `alert()` on "Add to cart" and other actions.
    // Auto-accept dialogs so they don't block execution; tests that need
    // to assert on the dialog message can override this per-method.
    page.onDialog(dialog -> dialog.accept());

    page.navigate(BASE_URL);
  }

  @AfterMethod
  public void closeContext() {
    if (context != null) {
      context.close();
    }
  }

  @AfterClass
  public void closeBrowser() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }
}
