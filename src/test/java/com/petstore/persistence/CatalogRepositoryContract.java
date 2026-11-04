package com.petstore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.petstore.catalog.CatalogRepository;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.ResultPage;

/**
 * Behaviour both catalog adapters must share. Runs against the data the populator loads from the
 * legacy seed file at start-up.
 */
public abstract class CatalogRepositoryContract {

    @Autowired
    protected CatalogRepository repository;

    @Test
    void categoriesAreSortedByLocalizedName() {
        ResultPage<Category> page = repository.findCategories(0, 10, "en_US");

        assertThat(page.list()).extracting(Category::name).containsExactly("Birds", "Cats", "Dogs", "Fish", "Reptiles");
        assertThat(page.isNextPageAvailable()).isFalse();
    }

    @Test
    void findsASingleCategoryInEachLocale() {
        assertThat(repository.findCategory("FISH", "en_US")).get().extracting(Category::name).isEqualTo("Fish");
        assertThat(repository.findCategory("FISH", "zh_CN")).get().extracting(Category::name).isEqualTo("鱼");
        assertThat(repository.findCategory("FISH", "de_DE")).isEmpty();
        assertThat(repository.findCategory("NOPE", "en_US")).isEmpty();
    }

    @Test
    void productsPageThroughACategoryTwoAtATime() {
        ResultPage<Product> first = repository.findProducts("DOGS", 0, 2, "en_US");
        ResultPage<Product> second = repository.findProducts("DOGS", first.getStartOfNextPage(), 2, "en_US");
        ResultPage<Product> last = repository.findProducts("DOGS", 4, 2, "en_US");

        assertThat(first.list()).extracting(Product::name).containsExactly("Bulldog", "Chihuahua");
        assertThat(first.isNextPageAvailable()).isTrue();
        assertThat(second.list()).extracting(Product::name).containsExactly("Dalmation", "Golden Retriever");
        assertThat(last.list()).extracting(Product::name).containsExactly("Labrador Retriever", "Poodle");
        assertThat(last.isNextPageAvailable()).isFalse();
    }

    @Test
    void startBeyondTheEndIsAnEmptyPage() {
        ResultPage<Product> page = repository.findProducts("DOGS", 50, 2, "en_US");

        assertThat(page.list()).isEmpty();
        assertThat(page.isNextPageAvailable()).isFalse();
    }

    @Test
    void itemsComeBackInSeedOrderNotStringOrder() {
        assertThat(repository.findItems("K9-DL-01", 0, 10, "en_US").list())
                .extracting(Item::itemId).containsExactly("EST-9", "EST-10");
    }

    @Test
    void itemCarriesProductAndCategoryFromTheJoin() {
        Item item = repository.findItem("EST-1", "en_US").orElseThrow();

        assertThat(item.productId()).isEqualTo("FI-SW-01");
        assertThat(item.categoryId()).isEqualTo("FISH");
        assertThat(item.productName()).isEqualTo("Angelfish");
        assertThat(item.attributes()).containsExactly("Large", "Cuddly");
        assertThat(item.listPrice()).isEqualByComparingTo("16.50");
        assertThat(item.unitCost()).isEqualByComparingTo("10.00");
        assertThat(item.image()).isEqualTo("fish3.gif");
        assertThat(item.description()).isEqualTo("Fresh Water fish from Japan");
    }

    @Test
    void pricesAreLocaleSpecific() {
        assertThat(repository.findItem("EST-1", "ja_JP").orElseThrow().listPrice()).isEqualByComparingTo("1951");
        assertThat(repository.findItem("EST-1", "zh_CN").orElseThrow().listPrice()).isEqualByComparingTo("142");
    }

    @Test
    void searchMatchesProductNameCategoryOrDescription() {
        assertThat(ids(repository.searchItems(Set.of("angelfish"), 0, 50, "en_US"))).containsExactly("EST-1", "EST-2");
        assertThat(ids(repository.searchItems(Set.of("reptiles"), 0, 50, "en_US")))
                .containsExactly("EST-11", "EST-12", "EST-13");
        assertThat(ids(repository.searchItems(Set.of("fire station"), 0, 50, "en_US"))).containsExactly("EST-9", "EST-10");
        assertThat(ids(repository.searchItems(Set.of("unicorn"), 0, 50, "en_US"))).isEmpty();
    }

    @Test
    void searchKeywordsAreOred() {
        assertThat(ids(repository.searchItems(Set.of("angelfish", "dalmation"), 0, 50, "en_US")))
                .containsExactly("EST-1", "EST-2", "EST-9", "EST-10");
    }

    @Test
    void searchTreatsWildcardCharactersLiterally() {
        assertThat(repository.searchItems(Set.of("%"), 0, 50, "en_US").list()).isEmpty();
        assertThat(repository.searchItems(Set.of("_"), 0, 50, "en_US").list()).isEmpty();
        assertThat(repository.searchItems(Set.of(".*"), 0, 50, "en_US").list()).isEmpty();
    }

    @Test
    void searchWorksInJapanese() {
        assertThat(ids(repository.searchItems(Set.of("エンゼル"), 0, 50, "ja_JP"))).containsExactly("EST-1", "EST-2");
    }

    @Test
    void searchResultsPage() {
        ResultPage<Item> first = repository.searchItems(Set.of("dog"), 0, 2, "en_US");

        assertThat(first.getSize()).isEqualTo(2);
        assertThat(first.isNextPageAvailable()).isTrue();
    }

    private static java.util.List<String> ids(ResultPage<Item> page) {
        return page.list().stream().map(Item::itemId).toList();
    }
}
