package com.demoblaze.pages;

import com.microsoft.playwright.Page;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Page object for the SweetAlert order-confirmation dialog shown after a
 * successful purchase.
 */
public class ConfirmationModal {

  private final Page page;

  private static final String CONFIRMATION_TEXT = ".sweet-alert p";
  private static final String OK_BTN = ".sweet-alert .confirm";

  public ConfirmationModal(Page page) {
    this.page = page;
  }

  /** Returns the full confirmation message (order Id, amount, card, name, date). */
  public String getConfirmationText() {
    return page.locator(CONFIRMATION_TEXT).textContent().trim();
  }

  /**
   * Parses the confirmation message into label -> value pairs (e.g.
   * "Id" -> "91", "Amount" -> "360 USD"). Relies on the rendered text
   * having each field on its own line (innerText respects <br> as a line
   * break, unlike textContent). AC-6 doesn't specify the exact field
   * format, so this only supports presence/non-empty checks, not format
   * validation - callers should include getConfirmationText() in failure
   * messages so a live-format mismatch is visible on the first CI run.
   */
  public Map<String, String> getConfirmationDetails() {
    String raw = page.locator(CONFIRMATION_TEXT).innerText();
    Map<String, String> details = new LinkedHashMap<>();
    for (String line : raw.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      int idx = trimmed.indexOf(':');
      if (idx > 0) {
        String key = trimmed.substring(0, idx).trim();
        String value = trimmed.substring(idx + 1).trim();
        details.put(key, value);
      }
    }
    return details;
  }

  public boolean isConfirmationVisible() {
    return page.locator(".sweet-alert.showSweetAlert").isVisible();
  }

  /** Dismisses the confirmation, returning to the home page. */
  public HomePage clickOk() {
    page.locator(OK_BTN).click();
    page.locator(".card-title").first().waitFor();
    return new HomePage(page);
  }
}
