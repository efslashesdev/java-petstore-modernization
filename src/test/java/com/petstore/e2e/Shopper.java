package com.petstore.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.UUID;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** What a person does in the store, so the tests read as journeys rather than selectors. */
final class Shopper {

    private final Page page;

    Shopper(Page page) {
        this.page = page;
    }

    Shopper visit(String path) {
        page.navigate(path);
        return this;
    }

    Shopper openCategory(String name) {
        page.locator("#sidebar").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(name).setExact(true)).click();
        return this;
    }

    Shopper openProduct(String name) {
        page.locator("#body").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(name).setExact(true)).click();
        return this;
    }

    Shopper addToCart(String itemId) {
        page.locator("button[data-item='" + itemId + "']").first().click();
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/cart\\.do$"));
        return this;
    }

    Shopper addItemToCart(String itemId) {
        visit("/item.screen?item_id=" + itemId);
        return addToCart(itemId);
    }

    Shopper setQuantity(String itemId, int quantity) {
        page.locator("input[name='itemQuantity_" + itemId + "']").fill(String.valueOf(quantity));
        return this;
    }

    Shopper updateCart() {
        page.locator("#cart-form input[type=submit]").click();
        return this;
    }

    Shopper remove(String itemId) {
        page.locator("tr[data-item='" + itemId + "'] button").click();
        return this;
    }

    Shopper search(String keywords) {
        page.locator("input[name=keywords]").fill(keywords);
        page.locator("form[role=search] input[type=submit]").click();
        return this;
    }

    Shopper signInFromBanner() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in")).click();
        return this;
    }

    Shopper signIn(String username, String password) {
        page.locator("#j_username").fill(username);
        page.locator("#j_password").fill(password);
        page.locator("#signon-submit").click();
        return this;
    }

    Shopper signOut() {
        page.locator("form[action$='/signoff.do'] button").click();
        return this;
    }

    Shopper switchLanguage(String localeKey) {
        page.locator("a[href*='changelocale.do?locale=" + localeKey + "']").click();
        return this;
    }

    /** Steps one and two of sign-up, keeping the demo values the form is pre-filled with. */
    String registerNewCustomer() {
        String username = "e2e" + UUID.randomUUID().toString().substring(0, 8);
        visit("/signon.screen");
        page.locator("#username").fill(username);
        page.locator("#password").fill("s3cret!");
        page.locator("#passwordRepeat").fill("s3cret!");
        page.locator("#newuser-submit").click();
        page.locator("#customer-submit").click();
        return username;
    }

    Locator body() {
        return page.locator("#body");
    }
}
