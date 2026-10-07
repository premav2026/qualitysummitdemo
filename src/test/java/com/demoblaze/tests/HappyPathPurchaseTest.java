package com.demoblaze.tests;

import com.demoblaze.base.BaseTest;
import com.demoblaze.pages.CartPage;
import com.demoblaze.pages.CheckoutModal;
import com.demoblaze.pages.ConfirmationModal;
import com.demoblaze.pages.HomePage;
import com.demoblaze.pages.ProductPage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Happy-path purchase flow for https://demoblaze.com/
 *
 * Maps to SCRUM-1 "Guest user can add a product to cart and complete a
 * purchase":
 *   TC-01 -> AC-1 (category filter)
 *   TC-02 -> AC-2 (product detail page content)
 *   TC-03 -> AC-3 (add to cart confirmation)
 *   TC-04 -> AC-4 (product appears in cart with correct name and price)
 *   TC-05 -> AC-5 (checkout modal opens with expected fields)
 *   TC-06 -> AC-6 (purchase confirmation shows order details)
 *   TC-07 -> AC-7 (OK returns to home, cart cleared)
 *
 * Each step is a separate @Test method (rather than one giant test) so a
 * failure at any stage is individually reportable in Surefire/CI. The steps
 * are chained with dependsOnMethods and share one page/cart, because
 * BaseTest creates a single BrowserContext per test class.
 *
 * TODOs marked "Open Question #N" refer to the SCRUM-1 requirement brief:
 * the ticket does not define those values, so the assertions stop short of
 * guessing them.
 */
public class HappyPathPurchaseTest extends BaseTest {

  private static final String CATEGORY = "Phones";
  private static final String PRODUCT_NAME = "Samsung galaxy s6";
  private static final String CUSTOMER_NAME = "Jane Tester";

  private HomePage homePage;
  private ProductPage productPage;
  private CheckoutModal checkout;
  private ConfirmationModal confirmation;
  private String productPrice;

  // TC-01
  @Test(description = "Selecting a category filters the product grid")
  public void shouldFilterProductsByCategory() {
    homePage = new HomePage(page);
    homePage.productLinks().first().waitFor();
    List<String> allProducts = homePage.productNames();

    homePage.selectCategory(CATEGORY);
    // The grid reloads via AJAX; wait until it differs from the unfiltered list.
    page.waitForCondition(() -> !homePage.productNames().equals(allProducts));

    List<String> filtered = homePage.productNames();
    Assert.assertTrue(filtered.size() > 0,
        "Expected at least one product to be listed under category: " + CATEGORY);
    Assert.assertTrue(filtered.contains(PRODUCT_NAME),
        "Expected " + PRODUCT_NAME + " under category " + CATEGORY + ", got: " + filtered);
    // TODO(SCRUM-1, Open Question #3): assert the grid contains *only* Phones
    // once an expected product list is defined, and decide whether Laptops /
    // Monitors should be checked too.
  }

  // TC-02
  @Test(description = "Product detail page shows title, price, description, add-to-cart",
      dependsOnMethods = "shouldFilterProductsByCategory")
  public void shouldShowProductDetails() {
    productPage = homePage.openProduct(PRODUCT_NAME);

    Assert.assertEquals(productPage.getTitle(), PRODUCT_NAME, "Product title should match");
    productPrice = numericPrice(productPage.getPrice());
    Assert.assertTrue(productPage.isDescriptionVisible(), "Product description should be visible");
    Assert.assertTrue(productPage.isAddToCartVisible(), "'Add to cart' control should be visible");
    // TODO(SCRUM-1, Open Question #4): assert product image and exact price
    // format once defined.
  }

  // TC-03
  @Test(description = "Adding to cart shows a confirmation",
      dependsOnMethods = "shouldShowProductDetails")
  public void shouldConfirmAddToCart() {
    productPage.assertReadyToAddToCart();
    dialogMessages.clear();
    productPage.addToCart();

    // The alert fires after an AJAX call, so wait for it instead of reading
    // immediately. This also ensures the item is saved before TC-04 opens the cart.
    page.waitForCondition(() -> !dialogMessages.isEmpty());
    Assert.assertFalse(dialogMessages.get(0).trim().isEmpty(),
        "Add-to-cart confirmation alert should have a message");
    // TODO(SCRUM-1, Open Question #5): assert the exact confirmation text, and
    // confirm a browser alert (vs. an in-page message) is the intended UI.
  }

  // TC-04
  @Test(description = "Added product appears in the cart with correct name and price",
      dependsOnMethods = "shouldConfirmAddToCart")
  public void shouldShowProductInCart() {
    CartPage cartPage = homePage.goToCart();
    // Cart rows are loaded via AJAX after the table body appears.
    cartPage.rows().first().waitFor();

    Assert.assertEquals(cartPage.itemCount(), 1, "Cart should contain exactly the one added item");
    Assert.assertEquals(cartPage.itemNameAt(0), PRODUCT_NAME,
        "Cart line item name should match the product that was added");
    Assert.assertEquals(numericPrice(cartPage.itemPriceAt(0)), productPrice,
        "Cart line item price should match the product page price");
  }

  // TC-05
  @Test(description = "Place Order opens the checkout modal with all required fields",
      dependsOnMethods = "shouldShowProductInCart")
  public void shouldOpenCheckoutModalWithAllFields() {
    checkout = new CartPage(page).placeOrder();

    Assert.assertEquals(checkout.missingFields(), List.of(),
        "Checkout modal should show Name, Country, City, Credit Card, Month and Year");
  }

  // TC-06
  @Test(description = "Purchase shows an order confirmation with order details",
      dependsOnMethods = "shouldOpenCheckoutModalWithAllFields")
  public void shouldShowOrderConfirmation() {
    checkout.fillAllDetails(
        CUSTOMER_NAME,
        "USA",
        "New York",
        "4111111111111111",
        "12",
        "2027"
    );
    confirmation = checkout.purchase();
    Assert.assertTrue(confirmation.isConfirmationVisible(), "Confirmation dialog should appear");

    String confirmationText = confirmation.getConfirmationText();
    for (String label : List.of("Id", "Amount", "Card Number", "Name", "Date")) {
      Assert.assertTrue(confirmationText.contains(label),
          "Confirmation should include " + label + ", got: " + confirmationText);
    }
    Assert.assertTrue(confirmationText.contains(CUSTOMER_NAME),
        "Confirmation should show the customer name, got: " + confirmationText);
    // TODO(SCRUM-1, Open Question #6): assert order Id format, amount vs.
    // product price, card number masking and date format once defined.
  }

  // TC-07
  @Test(description = "OK returns to the home page and the cart is empty",
      dependsOnMethods = "shouldShowOrderConfirmation")
  public void shouldReturnHomeWithEmptyCart() {
    HomePage home = confirmation.clickOk();
    Assert.assertTrue(home.productLinks().count() > 0,
        "Home page product grid should be shown after clicking OK");

    CartPage clearedCart = home.goToCart();
    Assert.assertEquals(clearedCart.itemCount(), 0,
        "Cart should be empty after a completed purchase");
    // TODO(SCRUM-1, Open Question #7): confirm this is the intended way to
    // verify the cart is cleared (rows load via AJAX, so an empty table could
    // also mean "not loaded yet"), and whether home should reset to the
    // default category.
  }

  /** Extracts the first number from a price string, e.g. "$360 *includes tax" -> "360". */
  private static String numericPrice(String text) {
    Matcher matcher = Pattern.compile("\\d+").matcher(text);
    Assert.assertTrue(matcher.find(), "No numeric price found in: " + text);
    return matcher.group();
  }
}
