package com.demoblaze.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page object for the Cart page (cart.html).
 */
public class CartPage {

  private final Page page;

  private static final String CART_ROWS = "#tbodyid tr";
  private static final String PLACE_ORDER_BTN = "#orderModalBtn";

  public CartPage(Page page) {
    this.page = page;
  }

  /** Returns the number of line items currently in the cart table. */
  public int itemCount() {
    return page.locator(CART_ROWS).count();
  }

  /** Returns the product name text for a given row index (0-based). */
  public String itemNameAt(int rowIndex) {
    // Cart rows: td[1] = image, td[2] = name, td[3] = price, td[4] = delete link.
    return page.locator(CART_ROWS).nth(rowIndex).locator("td").nth(1).textContent().trim();
  }

  /** Returns the price text for a given row index (0-based). */
  public String itemPriceAt(int rowIndex) {
    return page.locator(CART_ROWS).nth(rowIndex).locator("td").nth(2).textContent().trim();
  }

  public Locator rows() {
    return page.locator(CART_ROWS);
  }

  /** Clicks "Place Order", opening the checkout modal. */
  public CheckoutModal placeOrder() {
    page.locator(PLACE_ORDER_BTN).click();
    page.locator("#orderModal.show").waitFor();
    return new CheckoutModal(page);
  }
}
