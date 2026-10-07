package com.demoblaze.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for all tests: owns the Playwright/Browser lifecycle.
 * One BrowserContext + Page is created per test class, so test methods that
 * are chained with dependsOnMethods share the same page and cart state as a
 * single scenario, while separate test classes stay isolated from each other.
 */
public class BaseTest {

  protected static final String BASE_URL = "https://demoblaze.com/";

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  protected Page page;

  /** Messages of the JS dialogs (alert/confirm) shown on the page, in order. */
  protected final List<String> dialogMessages = new ArrayList<>();

  @BeforeClass
  public void launchBrowserAndPage() {
    playwright = Playwright.create();
    boolean headless = !"false".equalsIgnoreCase(System.getProperty("headless", "true"));
    browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(headless));

    context = browser.newContext();
    page = context.newPage();

    // Demoblaze uses JS `alert()` on "Add to cart" and other actions.
    // Record each message so tests can assert on it, then accept the dialog
    // so it doesn't block execution.
    page.onDialog(dialog -> {
      dialogMessages.add(dialog.message());
      dialog.accept();
    });

    page.navigate(BASE_URL);
  }

  @AfterClass
  public void closeBrowser() {
    if (context != null) {
      context.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }
}
