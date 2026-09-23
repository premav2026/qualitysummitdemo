package com.demoblaze.tests;

import com.demoblaze.base.BaseTest;
import com.demoblaze.pages.CartPage;
import com.demoblaze.pages.CheckoutModal;
import com.demoblaze.pages.ConfirmationModal;
import com.demoblaze.pages.HomePage;
import com.demoblaze.pages.ProductPage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Happy-path purchase flow for https://www.demoblaze.com/
 *
 * Maps to JIRA ticket SCRUM-1 "Guest user can add a product to cart and
 * complete a purchase":
 *   TC-01 -> AC-1 (category filter shows only that category's products)
 *   TC-02 -> AC-2 (product detail page content)
 *   TC-03 -> AC-3 (add to cart confirmation dialog appears)
 *   TC-04 -> AC-4 (product appears in cart with correct name and price)
 *   TC-05 -> AC-5 (checkout modal opens with expected fields)
 *   TC-06 -> AC-6 (purchase confirmation shows order id/amount/card/name/date)
 *   TC-07 -> AC-7 (OK returns to the home page, cart cleared)
 *
 * Each step is a separate @Test method (so a failure at any stage is
 * individually reportable in Surefire/CI) chained via dependsOnMethods,
 * sharing ONE BrowserContext/Page for the whole class (see BaseTest) since
 * this is a single continuous guest session, not independent tests.
 */
public class HappyPathPurchaseTest extends BaseTest {

  private static final String CATEGORY = "Phones";
  private static final String PRODUCT_NAME = "Samsung galaxy s6";
  private static final String CHECKOUT_NAME = "Jane Tester";
  private static final double ADD_TO_CART_DIALOG_TIMEOUT_MS = 5000;

  private HomePage homePage;
  private ProductPage productPage;
  private String productPriceOnDetailPage;

  // TC-01
  @Test(description = "Selecting a category filters the product grid to only that category")
  public void shouldFilterProductsByCategory() {
    homePage = new HomePage(page);
    homePage.selectCategory(CATEGORY);

    List<String> visibleNames = homePage.visibleProductNames();
    Assert.assertFalse(visibleNames.isEmpty(),
        "Expected at least one product to be listed under category: " + CATEGORY);

    Set<String> apiNames = homePage.lastCategoryApiProductNames();
    Assert.assertFalse(apiNames.isEmpty(),
        "Expected the /bycat API response to list at least one product for category: " + CATEGORY);

    // AC-1 requires the grid to show ONLY items in the selected category -
    // compare the displayed set against what the category API itself
    // returned, rather than hardcoding the site's catalog.
    Assert.assertEquals(new LinkedHashSet<>(visibleNames), apiNames,
        "Displayed product grid should show exactly the products the /bycat API returned "
            + "for category '" + CATEGORY + "' (no more, no fewer). API returned: " + apiNames
            + ", grid showed: " + visibleNames);
  }

  // TC-02
  @Test(description = "Product detail page shows title, price, description, add-to-cart",
      dependsOnMethods = "shouldFilterProductsByCategory")
  public void shouldShowProductDetails() {
    productPage = homePage.openProduct(PRODUCT_NAME);

    Assert.assertTrue(productPage.getTitle().length() > 0, "Product title should not be empty");
    String price = productPage.getPrice();
    Assert.assertTrue(price.contains("$"), "Price should be displayed with $ sign");
    Assert.assertTrue(productPage.isDescriptionVisible(), "Product description should be visible");
    Assert.assertTrue(productPage.isAddToCartVisible(), "'Add to cart' control should be visible");

    // Captured here, on the product page, so TC-04 can compare against it
    // after navigating away (calling productPage.getPrice() again from the
    // cart would read the wrong page's price).
    productPriceOnDetailPage = price;
  }

  // TC-03
  @Test(description = "Adding to cart shows a confirmation dialog",
      dependsOnMethods = "shouldShowProductDetails")
  public void shouldConfirmAddToCart() {
    productPage.assertReadyToAddToCart();
    resetDialogFlag();
    productPage.addToCart(); // waits for the /addtocart XHR response internally
    waitForDialog(ADD_TO_CART_DIALOG_TIMEOUT_MS); // then waits for the alert itself

    // AC-3 doesn't specify exact alert wording (open question), so only
    // presence of a confirmation dialog is asserted, not its text.
    Assert.assertTrue(wasDialogShown(),
        "Expected a confirmation dialog to appear when adding to cart");
  }

  // TC-04
  @Test(description = "Added product appears in the cart with correct name and price",
      dependsOnMethods = "shouldConfirmAddToCart")
  public void shouldShowProductInCart() {
    CartPage cartPage = homePage.goToCart();

    Assert.assertTrue(cartPage.itemCount() >= 1, "Cart should contain at least one item");
    Assert.assertEquals(cartPage.itemNameAt(0), PRODUCT_NAME,
        "Cart line item name should match the product that was added");

    String productPriceDigits = productPriceOnDetailPage.replaceAll("\\D+", "");
    String cartPrice = cartPage.itemPriceAt(0);
    String cartPriceDigits = cartPrice.replaceAll("\\D+", "");
    Assert.assertEquals(cartPriceDigits, productPriceDigits,
        "Cart line item price should match the product page price (captured in TC-02: '"
            + productPriceOnDetailPage + "'), but cart showed: '" + cartPrice + "'");
  }

  // TC-05, TC-06, TC-07 combined into one flow since they're a single
  // continuous modal interaction (checkout -> purchase -> confirm -> OK)
  @Test(description = "Full checkout: place order, purchase, confirm, return home",
      dependsOnMethods = "shouldShowProductInCart")
  public void shouldCompleteCheckoutAndReturnHome() {
    CartPage cartPage = new CartPage(page);
    String cartPriceDigits = cartPage.itemPriceAt(0).replaceAll("\\D+", "");

    // TC-05: checkout modal opens with expected fields (page.fill throws
    // loudly if a field is missing, which doubles as an existence check)
    CheckoutModal checkout = cartPage.placeOrder();
    checkout.fillAllDetails(
        CHECKOUT_NAME,
        "USA",
        "New York",
        "4111111111111111",
        "12",
        "2027"
    );

    // TC-06: purchase confirmation shows order details
    ConfirmationModal confirmation = checkout.purchase();
    Assert.assertTrue(confirmation.isConfirmationVisible(), "Confirmation dialog should appear");

    String rawConfirmationText = confirmation.getConfirmationText();
    Map<String, String> details = confirmation.getConfirmationDetails();
    for (String expectedField : new String[]{"Id", "Amount", "Card Number", "Name", "Date"}) {
      boolean present = details.containsKey(expectedField) && !details.get(expectedField).isEmpty();
      Assert.assertTrue(present,
          "Confirmation should include a non-empty '" + expectedField + "' field. "
              + "Parsed fields: " + details + ". Raw confirmation text: \"" + rawConfirmationText + "\"");
    }
    Assert.assertEquals(details.get("Name"), CHECKOUT_NAME,
        "Confirmation name should match the name entered at checkout. "
            + "Raw confirmation text: \"" + rawConfirmationText + "\"");
    String confirmedAmountDigits = details.get("Amount").replaceAll("\\D+", "");
    Assert.assertEquals(confirmedAmountDigits, cartPriceDigits,
        "Confirmation amount should match the cart price. "
            + "Raw confirmation text: \"" + rawConfirmationText + "\"");

    // TC-07: OK returns to home, cart is cleared
    HomePage home = confirmation.clickOk();
    Assert.assertTrue(home.isOnHomePage(),
        "Expected to be back on the Demoblaze home page after clicking OK, but URL was: " + page.url());
    CartPage clearedCart = home.goToCart();
    Assert.assertEquals(clearedCart.itemCount(), 0,
        "Cart should be empty after a completed purchase");
  }
}
