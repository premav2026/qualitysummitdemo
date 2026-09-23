package com.demoblaze.tests;

import com.demoblaze.base.BaseTest;
import com.demoblaze.pages.CartPage;
import com.demoblaze.pages.CheckoutModal;
import com.demoblaze.pages.ConfirmationModal;
import com.demoblaze.pages.HomePage;
import com.demoblaze.pages.ProductPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Happy-path purchase flow for https://demoblaze.com/
 *
 * Maps to JIRA ticket "Happy Path — Guest user can add a product to cart
 * and complete a purchase":
 *   TC-01 -> AC-1 (category filter)
 *   TC-02 -> AC-2 (product detail page content)
 *   TC-03 -> AC-3 (add to cart confirmation)
 *   TC-04 -> AC-4 (product appears in cart)
 *   TC-05 -> AC-5 (checkout modal opens with expected fields)
 *   TC-06 -> AC-6 (purchase confirmation shows order details)
 *   TC-07 -> AC-7 (OK returns to home, cart cleared)
 *
 * Each step is a separate @Test method (rather than one giant test) so a
 * failure at any stage is individually reportable in Surefire/CI, while
 * still running as a single logical scenario via TestNG's execution order
 * within the class (methods run in the order declared by default when
 * `preserve-order` is not overridden).
 */
public class HappyPathPurchaseTest extends BaseTest {

  private static final String CATEGORY = "Phones";
  private static final String PRODUCT_NAME = "Samsung galaxy s6";

  private HomePage homePage;
  private ProductPage productPage;

  // TC-01
  @Test(description = "Selecting a category filters the product grid")
  public void shouldFilterProductsByCategory() {
    homePage = new HomePage(page);
    homePage.selectCategory(CATEGORY);

    int visibleProducts = homePage.productLinks().count();
    Assert.assertTrue(visibleProducts > 0,
        "Expected at least one product to be listed under category: " + CATEGORY);
  }

  // TC-02
  @Test(description = "Product detail page shows title, price, description, add-to-cart",
      dependsOnMethods = "shouldFilterProductsByCategory")
  public void shouldShowProductDetails() {
    productPage = homePage.openProduct(PRODUCT_NAME);

    Assert.assertTrue(productPage.getTitle().length() > 0, "Product title should not be empty");
    Assert.assertTrue(productPage.getPrice().contains("$"), "Price should be displayed with $ sign");
    Assert.assertTrue(productPage.isDescriptionVisible(), "Product description should be visible");
    Assert.assertTrue(productPage.isAddToCartVisible(), "'Add to cart' control should be visible");
  }

  // TC-03
  @Test(description = "Adding to cart shows a confirmation",
      dependsOnMethods = "shouldShowProductDetails")
  public void shouldConfirmAddToCart() {
    // BaseTest's page.onDialog(dialog -> dialog.accept()) auto-accepts the
    // JS alert Demoblaze shows here. If you need to assert on the alert's
    // exact text instead of just accepting it, override onDialog for this
    // test method specifically before calling addToCart().
    productPage.assertReadyToAddToCart();
    productPage.addToCart();
  }

  // TC-04
  @Test(description = "Added product appears in the cart",
      dependsOnMethods = "shouldConfirmAddToCart")
  public void shouldShowProductInCart() {
    CartPage cartPage = homePage.goToCart();

    Assert.assertTrue(cartPage.itemCount() >= 1, "Cart should contain at least one item");
    Assert.assertEquals(cartPage.itemNameAt(0), PRODUCT_NAME,
        "Cart line item name should match the product that was added");
  }

  // TC-05, TC-06, TC-07 combined into one flow since they're a single
  // continuous modal interaction (checkout -> purchase -> confirm -> OK)
  @Test(description = "Full checkout: place order, purchase, confirm, return home",
      dependsOnMethods = "shouldShowProductInCart")
  public void shouldCompleteCheckoutAndReturnHome() {
    CartPage cartPage = new CartPage(page);

    // TC-05: checkout modal opens with expected fields
    CheckoutModal checkout = cartPage.placeOrder();
    checkout.fillAllDetails(
        "Jane Tester",
        "USA",
        "New York",
        "4111111111111111",
        "12",
        "2027"
    );

    // TC-06: purchase confirmation shows order details
    ConfirmationModal confirmation = checkout.purchase();
    Assert.assertTrue(confirmation.isConfirmationVisible(), "Confirmation dialog should appear");

    String confirmationText = confirmation.getConfirmationText();
    Assert.assertTrue(confirmationText.contains("Id"), "Confirmation should include an order Id");
    Assert.assertTrue(confirmationText.contains("Amount"), "Confirmation should include an amount");

    // TC-07: OK returns to home, cart is cleared
    HomePage home = confirmation.clickOk();
    CartPage clearedCart = home.goToCart();
    Assert.assertEquals(clearedCart.itemCount(), 0,
        "Cart should be empty after a completed purchase");
  }
}
