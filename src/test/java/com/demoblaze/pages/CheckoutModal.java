package com.demoblaze.pages;

import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Page object for the "Place Order" checkout modal.
 */
public class CheckoutModal {

  private final Page page;

  private static final String NAME_INPUT = "#name";
  private static final String COUNTRY_INPUT = "#country";
  private static final String CITY_INPUT = "#city";
  private static final String CARD_INPUT = "#card";
  private static final String MONTH_INPUT = "#month";
  private static final String YEAR_INPUT = "#year";
  private static final String PURCHASE_BTN = "button[onclick='purchaseOrder()']";

  public CheckoutModal(Page page) {
    this.page = page;
  }

  /** Returns the labels of any checkout fields that are not visible (empty if all are shown). */
  public List<String> missingFields() {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("Name", NAME_INPUT);
    fields.put("Country", COUNTRY_INPUT);
    fields.put("City", CITY_INPUT);
    fields.put("Credit Card", CARD_INPUT);
    fields.put("Month", MONTH_INPUT);
    fields.put("Year", YEAR_INPUT);

    List<String> missing = new ArrayList<>();
    fields.forEach((label, selector) -> {
      if (!page.locator(selector).isVisible()) {
        missing.add(label);
      }
    });
    return missing;
  }

  public CheckoutModal fillName(String name) {
    page.fill(NAME_INPUT, name);
    return this;
  }

  public CheckoutModal fillCountry(String country) {
    page.fill(COUNTRY_INPUT, country);
    return this;
  }

  public CheckoutModal fillCity(String city) {
    page.fill(CITY_INPUT, city);
    return this;
  }

  public CheckoutModal fillCard(String card) {
    page.fill(CARD_INPUT, card);
    return this;
  }

  public CheckoutModal fillMonth(String month) {
    page.fill(MONTH_INPUT, month);
    return this;
  }

  public CheckoutModal fillYear(String year) {
    page.fill(YEAR_INPUT, year);
    return this;
  }

  /** Convenience method to fill every field for the happy-path case. */
  public CheckoutModal fillAllDetails(String name, String country, String city,
                                       String card, String month, String year) {
    return fillName(name).fillCountry(country).fillCity(city)
        .fillCard(card).fillMonth(month).fillYear(year);
  }

  /** Clicks "Purchase" and returns the confirmation dialog page object. */
  public ConfirmationModal purchase() {
    page.locator(PURCHASE_BTN).click();
    page.locator(".sweet-alert.showSweetAlert").waitFor();
    return new ConfirmationModal(page);
  }
}
