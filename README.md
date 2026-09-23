# qa-automation-demoblaze

Page Object Model test automation framework for https://demoblaze.com/,
covering the happy-path purchase flow only.

**Stack:** Java 21 · Maven · TestNG · Playwright (Java bindings)

## Project structure

```
src/test/java/com/demoblaze/
  base/
    BaseTest.java          # Shared Playwright/Browser/Context/Page lifecycle (TestNG hooks)
  pages/
    HomePage.java          # category selection, product links, cart nav
    ProductPage.java       # title/price/description, add-to-cart
    CartPage.java          # cart line items, place-order trigger
    CheckoutModal.java     # order detail form fields, purchase action
    ConfirmationModal.java # order confirmation text, OK dismissal
  tests/
    HappyPathPurchaseTest.java  # TC-01..TC-07, mapped to JIRA AC-1..AC-7
testng.xml                 # TestNG suite definition
pom.xml
```

### Test session lifecycle

`BaseTest` opens one `BrowserContext`/`Page` per **test class**
(`@BeforeClass`/`@AfterClass`), not per test method. `HappyPathPurchaseTest`'s
TC-01..TC-07 are chained steps of a single guest session (cart state has to
carry across them), so they intentionally share cookies/localStorage rather
than each getting an isolated context. One consequence: since later steps
`dependsOnMethods` on earlier ones, a failure partway through reports the
remaining steps as **skipped**, not failed - a genuinely green run means
0 failed *and* 0 skipped, not just 0 failed.

## First-time setup

```bash
mvn install
# Install Playwright's browser binaries (one-time, or after upgrading the
# playwright dependency version):
mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"
```

## Running the tests

```bash
mvn test
```

By default the browser runs **headless**. To watch it run (useful while
developing/debugging locators):

```bash
mvn test -Dheadless=false
```

## What's covered

Maps directly to the "Happy Path — Guest user can add a product to cart
and complete a purchase" JIRA ticket:

| Test method | Ticket AC | What it checks |
|---|---|---|
| `shouldFilterProductsByCategory` | AC-1 | Category nav filters the product grid to exactly that category's products (compared against the `/bycat` API response) |
| `shouldShowProductDetails` | AC-2 | Product page shows title/price/description/Add to cart |
| `shouldConfirmAddToCart` | AC-3 | Add to cart triggers a confirmation alert |
| `shouldShowProductInCart` | AC-4 | Product appears correctly in the cart table (name and price) |
| `shouldCompleteCheckoutAndReturnHome` | AC-5, AC-6, AC-7 | Checkout modal → purchase → confirmation (id/amount/card/name/date) → return home with empty cart |

## Known simplifications (given "happy path only" scope)

- No negative-path coverage (invalid checkout fields, empty-cart checkout)
  — those were intentionally scoped to separate JIRA tickets.
- Product name (`Samsung galaxy s6`) and category (`Phones`) are hardcoded
  as constants in the test class. If you want to parameterize across
  multiple products/categories, that's a natural next step
  (`@DataProvider` in TestNG) but was left out to keep this strictly to the
  happy-path scenario as requested.
- Locators (`#name`, `#card`, `.sweet-alert`, etc.) are based on
  Demoblaze's current DOM structure. This is a public demo site that can
  change without notice — if a test starts failing on an element-not-found
  error, check the actual page markup first before assuming a real app bug.
- `BaseTest` auto-accepts all JS dialogs via `page.onDialog(dialog ->
  dialog.accept())`. This is convenient for the happy path, and
  `shouldConfirmAddToCart` asserts a dialog fired, but no test asserts on
  the *exact text* of the "Product added" alert — add that if a future
  ticket requires verifying alert copy specifically.

## Extending this for the multi-agent pipeline

This repo structure is what `test-design` (the Test Design Agent) should
treat as the existing convention going forward — new tests should follow
the same Page Object + one-`@Test`-per-acceptance-criterion pattern shown
in `HappyPathPurchaseTest`, rather than falling back to the generic Java
baseline convention, now that a real example exists to copy from.
