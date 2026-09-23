package com.demoblaze.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Page object for the Demoblaze home page (product listing + category nav).
 */
public class HomePage {

  private final Page page;

  // Home-page-only element (Bootstrap carousel banner), used to confirm
  // we're actually on the home page rather than just seeing a
  // ".card-title" that could theoretically render elsewhere.
  private static final String HOME_MARKER = "#carouselExampleIndicators";

  private Set<String> lastCategoryApiProductNames = Collections.emptySet();

  public HomePage(Page page) {
    this.page = page;
  }

  /**
   * Clicks a category link in the left-hand nav (e.g. "Phones", "Laptops",
   * "Monitors"), waits for the product grid to actually reflect that
   * category, and captures the category API response so callers can
   * verify the displayed grid is exactly the category's product set (not
   * a superset/subset).
   *
   * Assumption: Demoblaze's category filter is backed by a POST to an
   * endpoint containing "/bycat" that returns JSON shaped like
   * {"Items": [{"title": "...", ...}, ...]}. This matches the publicly
   * documented Demoblaze API used across existing community automation
   * examples, but hasn't been verified against a live run from this
   * environment - if the shape differs, parseProductTitles below fails
   * loudly with the raw response body rather than a silent NPE.
   */
  public HomePage selectCategory(String categoryName) {
    Response response = page.waitForResponse(
        resp -> resp.url().contains("/bycat"),
        () -> {
          Locator categoryLink = page.getByRole(AriaRole.LINK,
              new Page.GetByRoleOptions().setName(categoryName));
          categoryLink.click();
        });
    lastCategoryApiProductNames = parseProductTitles(response.text());

    // The /bycat response resolving doesn't guarantee the grid has
    // re-rendered yet - the previously-unfiltered cards are already on the
    // page at click time, so waiting on ".card-title" alone (the old
    // behavior) races the re-render: it resolves immediately against the
    // stale, unfiltered grid. Poll until the rendered product-name set
    // actually matches the API's set, with a sensible timeout; if it never
    // converges, fall through and let the caller's own assertion report
    // the real mismatch with a descriptive message, instead of a bare
    // Playwright timeout.
    try {
      page.waitForCondition(
          () -> new LinkedHashSet<>(visibleProductNames()).equals(lastCategoryApiProductNames),
          new Page.WaitForConditionOptions().setTimeout(5000));
    } catch (TimeoutError e) {
      // Intentionally ignored - shouldFilterProductsByCategory's own
      // assertEquals(...) reports the actual mismatched sets clearly.
    }
    return this;
  }

  /** Returns the visible product name links currently in the grid. */
  public Locator productLinks() {
    return page.locator(".card-title a");
  }

  /** Returns the trimmed text of every visible product name currently in the grid. */
  public List<String> visibleProductNames() {
    return productLinks().allTextContents().stream()
        .map(String::trim)
        .collect(Collectors.toList());
  }

  /** Product names returned by the most recent category-filter API call (see selectCategory). */
  public Set<String> lastCategoryApiProductNames() {
    return lastCategoryApiProductNames;
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

  /**
   * True if the current page looks like the Demoblaze home page (not the
   * cart or a product detail page) - used for AC-7, which requires
   * actually landing back on the home page, not just any page that
   * happens to reuse the ".card-title" class.
   */
  public boolean isOnHomePage() {
    String url = page.url();
    boolean urlLooksLikeHome = !url.contains("cart.html") && !url.contains("prod.html");
    boolean hasHomeMarker = page.locator(HOME_MARKER).isVisible();
    return urlLooksLikeHome && hasHomeMarker;
  }

  private static Set<String> parseProductTitles(String jsonBody) {
    try {
      JsonObject root = JsonParser.parseString(jsonBody).getAsJsonObject();
      JsonArray items = root.getAsJsonArray("Items");
      Set<String> names = new LinkedHashSet<>();
      for (JsonElement item : items) {
        names.add(item.getAsJsonObject().get("title").getAsString().trim());
      }
      return names;
    } catch (RuntimeException e) {
      // Gson's failure modes here are all unchecked (JsonSyntaxException for
      // malformed JSON, IllegalStateException if "Items" isn't an array,
      // NullPointerException if "title" is missing) - catch broadly so any
      // parsing failure surfaces the raw response body instead of an opaque
      // stack trace.
      throw new AssertionError(
          "Could not parse the expected {\"Items\":[{\"title\":...}]} shape from the "
              + "/bycat response - Demoblaze's API format may differ from what this test "
              + "assumes. Raw response body: " + jsonBody, e);
    }
  }
}
