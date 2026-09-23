package com.demoblaze.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the Demoblaze home page (product listing + category nav).
 */
public class HomePage {

  private final Page page;

  public HomePage(Page page) {
    this.page = page;
  }

  /**
   * Clicks a category link in the left-hand nav (e.g. "Phones", "Laptops",
   * "Monitors") and waits for the product grid to refresh.
   */
  public HomePage selectCategory(String categoryName) {
    Locator categoryLink = page.getByRole(AriaRole.LINK,
        new Page.GetByRoleOptions().setName(categoryName));
    categoryLink.click();
    // Product grid reloads via AJAX; wait for at least one product card.
    page.locator(".card-title").first().waitFor();
    return this;
  }

  /** Returns the visible product name links currently in the grid. */
  public Locator productLinks() {
    return page.locator(".card-title a");
  }

  /** Clicks a product by its exact visible name and lands on the product page. */
  public ProductPage openProduct(String productName) {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(productName)).click();
    page.locator(".name").waitFor();
    return new ProductPage(page);
  }

  /** Navigates to the cart page via the top nav "Cart" link. */
  public CartPage goToCart() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cart")).click();
    page.locator("#tbodyid").waitFor();
    return new CartPage(page);
  }
}
