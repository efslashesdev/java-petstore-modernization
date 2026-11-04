package com.petstore.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

class BrowsingIT extends BrowserTest {

    @Test
    void fromTheLandingPageToAnItem() {
        shopper.visit("/");
        page.locator("#enter-store").click();
        assertThat(page).hasTitle("Welcome to the BluePrints Petstore");

        shopper.openCategory("Fish").openProduct("Angelfish");
        assertThat(page.locator("#items")).containsText("Large Angelfish");
        assertThat(page.locator("#items")).containsText("$16.50");

        page.getByText("Large Angelfish").click();
        assertThat(page.locator("#item-name")).hasText("Large Angelfish");
        assertThat(page.locator("#list-price")).hasText("$16.50");
        assertThat(page.locator("#your-price")).hasText("$10.00");
    }

    @Test
    void splashImageMapLeadsToTheCategory() {
        shopper.visit("/main.screen");
        // <area> elements have no box of their own; click the image where the Reptiles hotspot is (140,270)-(210,340)
        page.locator("img[usemap='#petmap']").click(new Locator.ClickOptions().setPosition(175, 305));

        assertThat(page.locator("#products")).containsText("Iguana");
        assertThat(page.locator("#products")).containsText("Rattlesnake");
    }

    @Test
    void productsPageTwoAtATime() {
        shopper.visit("/main.screen").openCategory("Dogs");
        assertThat(page.locator("#products tr")).hasCount(2);
        assertThat(page.locator("#products")).containsText("Bulldog");
        assertThat(page.locator("#page-previous")).hasCount(0);

        page.locator("#page-next").click();
        assertThat(page.locator("#products")).containsText("Dalmation");
        page.locator("#page-next").click();
        assertThat(page.locator("#products")).containsText("Poodle");
        assertThat(page.locator("#page-next")).hasCount(0);

        page.locator("#page-previous").click();
        assertThat(page.locator("#products")).containsText("Golden Retriever");
    }

    @Test
    void searchIgnoresCase() {
        shopper.visit("/main.screen").search("Angelfish");

        assertThat(page.locator("#results tr")).hasCount(2);
        assertThat(page.locator("#body")).containsText("Items matching any of: Angelfish");
    }

    @Test
    void searchWithNoHitsSaysSo() {
        shopper.visit("/main.screen").search("unicorn");

        assertThat(page.locator("#no-results")).isVisible();
    }

    @Test
    void japaneseStoreShowsJapaneseCatalogAndYen() {
        shopper.visit("/item.screen?item_id=EST-1").switchLanguage("ja_JP");

        assertThat(page).hasURL(Pattern.compile(".*/item\\.screen\\?item_id=EST-1$"));
        assertThat(page.locator("#item-name")).hasText("大 エンゼルフィッシュ");
        assertThat(page.locator("#list-price")).containsText("1,951");
        assertThat(page.locator("#sidebar")).containsText("魚");

        shopper.visit("/main.screen");
        assertThat(page.locator("#sidebar")).containsText("ペット");
    }

    @Test
    void chineseStoreAndBackToEnglish() {
        shopper.visit("/category.screen?category_id=BIRDS").switchLanguage("zh_CN");
        assertThat(page.locator("#body")).containsText("这个种类的商品");

        shopper.switchLanguage("en_US");
        assertThat(page.locator("#body")).containsText("Products for this Category");
    }
}
