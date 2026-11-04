package com.petstore.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.petstore.order.OrderRepository;
import com.petstore.order.PurchaseOrder;

class CheckoutIT extends BrowserTest {

    @Autowired
    OrderRepository orders;

    @Test
    void anonymousShopperSignsInAtCheckoutAndPlacesTheOrder() {
        shopper.addItemToCart("EST-6");
        shopper.addItemToCart("EST-9");
        shopper.setQuantity("EST-9", 2).updateCart();
        page.locator("#checkout").click();

        assertThat(page).hasURL(Pattern.compile(".*/signon\\.screen$"));
        shopper.signIn("j2ee", "j2ee");

        // back on the billing/shipping page, pre-filled from the account
        assertThat(page).hasURL(Pattern.compile(".*/enter_order_information\\.screen.*"));
        assertThat(page.locator("[id='billTo.givenName']")).hasValue("XYZ");
        assertThat(page.locator("[id='shipTo.city']")).hasValue("Palo Alto");

        page.locator("[id='shipTo.givenName']").fill("Gift");
        page.locator("#order-submit").click();

        assertThat(page).hasURL(Pattern.compile(".*/order_complete\\.screen$"));
        String orderId = page.locator("#order-id").getAttribute("data-order-id");
        assertThat(orderId).matches("1001\\d+");

        PurchaseOrder saved = orders.findById(orderId).orElseThrow();
        assertThat(saved.userId()).isEqualTo("j2ee");
        assertThat(saved.shipTo().givenName()).isEqualTo("Gift");
        assertThat(saved.lineItems()).extracting(PurchaseOrder.LineItem::itemId).containsExactly("EST-6", "EST-9");
        assertThat(saved.totalPrice()).isEqualByComparingTo("55.50");
        assertThat(saved.card().maskedNumber()).isEqualTo("**** 6789");
        assertThat(saved.card().type()).isEqualTo("Meow Card");

        shopper.visit("/cart.do");
        assertThat(page.locator("#cart-empty")).isVisible();
    }

    @Test
    void refreshingTheConfirmationDoesNotPlaceASecondOrder() {
        shopper.visit("/signon.screen").signIn("shopper", "j2ee");
        shopper.addItemToCart("EST-1");
        page.locator("#checkout").click();
        page.locator("#order-submit").click();
        String orderId = page.locator("#order-id").getAttribute("data-order-id");
        int before = orders.findByUser("shopper").size();

        page.reload();

        assertThat(page).hasURL(Pattern.compile(".*/main\\.screen$"));
        assertThat(orders.findByUser("shopper")).hasSize(before);
        assertThat(orders.findByUser("shopper").getFirst().orderId()).isEqualTo(orderId);
    }

    @Test
    void missingAddressFieldsKeepYouOnTheForm() {
        shopper.visit("/signon.screen").signIn("j2ee", "j2ee");
        shopper.addItemToCart("EST-1");
        page.locator("#checkout").click();

        page.locator("[id='billTo.givenName']").fill("");
        page.locator("[id='shipTo.zipCode']").fill("");
        page.locator("#order-submit").click();

        assertThat(page.locator(".form-errors")).isVisible();
        assertThat(page.locator("[id='billTo.givenName']")).hasClass(Pattern.compile("invalid"));
        assertThat(page.locator("[id='shipTo.zipCode']")).hasClass(Pattern.compile("invalid"));
        // what was typed elsewhere is still there
        assertThat(page.locator("[id='billTo.familyName']")).hasValue("ABC");
    }

    @Test
    void checkingOutAnEmptyCartShowsTheLegacyErrorPage() {
        shopper.visit("/signon.screen").signIn("j2ee", "j2ee");
        shopper.visit("/enter_order_information.screen");

        page.locator("#order-submit").click();

        assertThat(page.locator("#empty-cart-error")).isVisible();
    }
}
