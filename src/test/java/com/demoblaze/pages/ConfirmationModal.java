package com.demoblaze.pages;

import com.microsoft.playwright.Page;

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
