package com.petstore.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SignOnIT extends BrowserTest {

    @Test
    void wrongPasswordShowsTheErrorScreen() {
        shopper.visit("/main.screen").signInFromBanner().signIn("j2ee", "wrong");

        assertThat(page.locator("#signon-error")).isVisible();
    }

    @Test
    void signedInCustomerSeesMyListForTheirFavouriteCategory() {
        shopper.visit("/main.screen").signInFromBanner().signIn("j2ee", "j2ee");

        assertThat(page).hasTitle("Welcome");
        // j2ee's favourite category is REPTILES with My List on and banners off
        assertThat(page.locator("#mylist")).containsText("Iguana");
        assertThat(page.locator("#mylist")).containsText("Rattlesnake");
        assertThat(page.locator("#advice-banner")).hasCount(0);
        assertThat(page.getByText("Sign out")).isVisible();
    }

    @Test
    void japaneseCustomerGetsTheJapaneseStoreOnSignIn() {
        shopper.visit("/signon.screen").signIn("j2ee-ja", "j2ee");

        assertThat(page.locator("#sidebar")).containsText("ペット");
    }

    @Test
    void protectedPageSendsYouToSignInAndThenBack() {
        shopper.visit("/customer.do");
        assertThat(page).hasURL(Pattern.compile(".*/signon\\.screen$"));

        shopper.signIn("shopper", "j2ee");

        assertThat(page).hasURL(Pattern.compile(".*/customer\\.do.*"));
        assertThat(page.locator("#given-name")).hasText("XYZ");
        assertThat(page.locator("#card-number")).hasText("**** 6789");
    }

    @Test
    void signOutKeepsTheLanguage() {
        shopper.visit("/signon.screen").signIn("j2ee", "j2ee");
        shopper.switchLanguage("zh_CN");

        page.locator("form[action$='/signoff.do'] button").click();

        assertThat(page.locator("#signed-off")).isVisible();
        assertThat(page.locator("#body")).containsText("谢谢光临");
        assertThat(page.locator("a[href$='/signon_welcome.screen']").first()).isVisible();
    }

    @Test
    void rememberMyUserName() {
        shopper.visit("/signon.screen");
        page.locator("#j_username").fill("shopper");
        page.locator("#j_password").fill("j2ee");
        page.locator("#j_remember_username").check();
        page.locator("#signon-submit").click();
        shopper.signOut();

        shopper.visit("/signon.screen");

        assertThat(page.locator("#j_username")).hasValue("shopper");
        assertThat(page.locator("#j_remember_username")).isChecked();
    }
}
