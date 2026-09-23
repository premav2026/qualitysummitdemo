package com.demoblaze.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Page object for an individual product detail page.
 */
public class ProductPage {

  private final Page page;

  private static final String TITLE = ".name";
  private static final String PRICE = ".price-container";
  private static final String DESCRIPTION = "#more-information p";
  private static final String ADD_TO_CART_BTN = "a.btn.btn-success.btn-lg";

  public ProductPage(Page page) {
    this.page = page;
  }

  public String getTitle() {
    return page.locator(TITLE).textContent().trim();
  }

  public String getPrice() {
    return page.locator(PRICE).textContent().trim();
  }

  public boolean isDescriptionVisible() {
    return page.locator(DESCRIPTION).isVisible();
  }

  public boolean isAddToCartVisible() {
    return page.locator(ADD_TO_CART_BTN).isVisible();
  }

  /**
   * Clicks "Add to cart" and waits for the underlying /addtocart XHR to
   * complete before returning. Demoblaze shows its "Product added" alert
   * asynchronously after this request resolves, so callers that need to
   * assert the alert appeared (see BaseTest.waitForDialog) should still
   * wait for the dialog flag afterwards - this only guarantees the
   * network round-trip has finished, not that the alert has rendered.
   *
   * Assumption: the add-to-cart request URL contains "/addtocart" - this
   * matches Demoblaze's publicly documented API but hasn't been verified
   * live from this environment.
   */
  public ProductPage addToCart() {
    page.waitForResponse(
        resp -> resp.url().contains("/addtocart"),
        () -> page.locator(ADD_TO_CART_BTN).click());
    return this;
  }

  /** Asserts the add-to-cart control is visible before interacting, using
   *  Playwright's web-first (auto-retrying) assertions. */
  public void assertReadyToAddToCart() {
    PlaywrightAssertions.assertThat(page.locator(ADD_TO_CART_BTN)).isVisible();
  }
}
