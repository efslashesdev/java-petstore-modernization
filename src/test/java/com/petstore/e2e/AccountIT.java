package com.petstore.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class AccountIT extends BrowserTest {

    @Test
    void newCustomerSignsUpAndIsSignedIn() {
        String username = shopper.registerNewCustomer();

        assertThat(page).hasURL(Pattern.compile(".*/main\\.screen$"));
        assertThat(page.getByText("Sign out")).isVisible();

        shopper.visit("/customer.do");
        assertThat(page.locator("#given-name")).hasText("Duke");
        assertThat(page.locator("#favorite-category")).hasText("BIRDS");

        // a brand-new session can sign in with the chosen password
        page.context().clearCookies();
        shopper.visit("/signon.screen").signIn(username, "s3cret!");
        assertThat(page.getByText("Sign out")).isVisible();
    }

    @Test
    void editingTheAccountKeepsCardTypeAndExpiryWhereTheyBelong() {
        shopper.registerNewCustomer();
        shopper.visit("/update_customer.screen");

        page.locator("#cardType").selectOption("Duke Express");
        page.locator("#expiryMonth").selectOption("07");
        page.locator("#customer-submit").click();

        assertThat(page.locator("#card-type")).hasText("Duke Express");
        assertThat(page.locator("#card-expiry")).containsText("07 /");
    }

    @Test
    void preferencesChangeLanguageMyListAndBanner() {
        shopper.registerNewCustomer();
        shopper.visit("/update_customer.screen");

        page.locator("#language").selectOption("ja_JP");
        page.locator("#favoriteCategory").selectOption("CATS");
        page.locator("#banners1").check();
        page.locator("#myList1").check();
        page.locator("#customer-submit").click();

        assertThat(page.locator("#body")).containsText("顧客情報");
        shopper.visit("/main.screen");
        assertThat(page.locator("#advice-banner")).hasAttribute("src", Pattern.compile(".*banner_cats\\.gif$"));
        assertThat(page.locator("#mylist")).containsText("ペルシアネコ");
    }

    @Test
    void invalidAccountDetailsAreReportedNotSaved() {
        shopper.registerNewCustomer();
        shopper.visit("/update_customer.screen");

        page.locator("[id='contact.city']").fill("");
        page.locator("[id='contact.email']").fill("not-an-email");
        page.locator("#customer-submit").click();

        assertThat(page.locator(".form-errors")).isVisible();
        assertThat(page.locator("[id='contact.email']")).hasClass(Pattern.compile("invalid"));

        shopper.visit("/customer.do");
        assertThat(page.locator("#city")).hasText("Sunville");
    }

    @Test
    void passwordsMustMatchOnSignUp() {
        shopper.visit("/signon.screen");
        page.locator("#username").fill("mismatch-user");
        page.locator("#password").fill("one-password");
        page.locator("#passwordRepeat").fill("another-one");
        page.locator("#newuser-submit").click();

        assertThat(page.locator("#newuser-form")).containsText("Passwords do not match");
    }

    @Test
    void takenUserNameIsRejected() {
        shopper.visit("/signon.screen");
        page.locator("#username").fill("j2ee");
        page.locator("#password").fill("whatever");
        page.locator("#passwordRepeat").fill("whatever");
        page.locator("#newuser-submit").click();

        assertThat(page.locator("#duplicate-account")).isVisible();
    }
}
