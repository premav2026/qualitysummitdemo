package com.demoblaze.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for chained, single-scenario test classes (like
 * HappyPathPurchaseTest): one Playwright Browser, BrowserContext and Page
 * are created ONCE per test class (@BeforeClass) and reused across every
 * @Test method in that class, instead of a fresh context per method.
 *
 * Why: SCRUM-1's acceptance criteria describe one continuous guest session
 * (browse -> add to cart -> checkout -> purchase -> confirm), and the cart
 * is server-side session state tied to the browser's cookies. The
 * previous per-method (@BeforeMethod/@AfterMethod) design created a brand
 * new, empty-cart BrowserContext before every @Test method and closed it
 * after - which both breaks page objects captured by earlier methods
 * (they hold a reference to an already-closed Page) and resets the cart
 * between chained steps even when it doesn't throw outright.
 *
 * Trade-off: @Test methods in a class extending this BaseTest are NOT
 * isolated from each other - they intentionally share cookies/
 * localStorage/cart state, and must run in the order enforced by
 * dependsOnMethods in the test class itself. If a future test class needs
 * independent, isolated test methods instead of one chained scenario,
 * don't extend this BaseTest as-is - give it its own per-method context
 * lifecycle instead.
 */
public class BaseTest {

  protected static final String BASE_URL = "https://www.demoblaze.com/";

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  protected Page page;

  // Tracks whether a JS dialog (e.g. the "Product added" alert) fired,
  // without asserting its exact text - AC-3's exact alert wording is an
  // open question, so tests only verify a confirmation dialog appeared.
  private final AtomicBoolean dialogAppeared = new AtomicBoolean(false);

  @BeforeClass
  public void launchBrowserAndOpenSession() {
    playwright = Playwright.create();
    boolean headless = !"false".equalsIgnoreCase(System.getProperty("headless", "true"));
    browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(headless));

    context = browser.newContext();
    page = context.newPage();

    // Demoblaze uses JS `alert()` on "Add to cart" and other actions.
    // Auto-accept dialogs so they don't block execution, while recording
    // that one fired so tests can assert presence (not exact text).
    page.onDialog(dialog -> {
      dialogAppeared.set(true);
      dialog.accept();
    });

    page.navigate(BASE_URL);
  }

  @AfterClass
  public void closeSessionAndBrowser() {
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

  protected void resetDialogFlag() {
    dialogAppeared.set(false);
  }

  protected boolean wasDialogShown() {
    return dialogAppeared.get();
  }

  /**
   * Polls until a dialog has fired (see resetDialogFlag/wasDialogShown) or
   * throws a PlaywrightException if none fires within timeoutMillis.
   * Demoblaze shows its "Product added" alert asynchronously after the
   * /addtocart XHR resolves, so a bare assertion right after click() can
   * race ahead of the alert - this waits for it explicitly instead.
   */
  protected void waitForDialog(double timeoutMillis) {
    page.waitForCondition(dialogAppeared::get,
        new Page.WaitForConditionOptions().setTimeout(timeoutMillis));
  }
}
