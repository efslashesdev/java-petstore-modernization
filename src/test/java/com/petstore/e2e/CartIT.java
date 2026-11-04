package com.petstore.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

class CartIT extends BrowserTest {

    @Test
    void addUpdateAndRemove() {
        shopper.visit("/product.screen?product_id=FI-SW-01").addToCart("EST-1");
        shopper.addItemToCart("EST-3");

        assertThat(page.locator("#cart-lines tr[data-item]")).hasCount(2);
        assertThat(page.locator("#cart-total")).hasText("$35.00");

        shopper.setQuantity("EST-3", 3).updateCart();
        assertThat(page.locator("#cart-total")).hasText("$72.00");

        shopper.remove("EST-1");
        assertThat(page.locator("#cart-lines tr[data-item]")).hasCount(1);
        assertThat(page.locator("#cart-total")).hasText("$55.50");

        shopper.setQuantity("EST-3", 0).updateCart();
        assertThat(page.locator("#cart-empty")).isVisible();
    }

    @Test
    void cartIsPricedInTheCurrentLanguage() {
        shopper.addItemToCart("EST-1");
        assertThat(page.locator("#cart-total")).hasText("$16.50");

        shopper.switchLanguage("ja_JP");
        assertThat(page.locator("#cart-total")).containsText("1,951");
    }

    @Test
    void refreshingAfterAddingDoesNotAddAgain() {
        shopper.addItemToCart("EST-1");
        page.reload();

        assertThat(page.locator("#cart-lines tr[data-item]")).hasCount(1);
        assertThat(page.locator("input[name=itemQuantity_EST-1]")).hasValue("1");
    }

    @Test
    void cartBelongsToTheBrowserSession() {
        shopper.addItemToCart("EST-1");

        page.context().clearCookies();
        shopper.visit("/cart.do");

        assertThat(page.locator("#cart-empty")).isVisible();
    }
}
