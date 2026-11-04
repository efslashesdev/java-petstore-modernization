package com.petstore.parity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.petstore.catalog.CatalogData;
import com.petstore.catalog.CatalogRepository;
import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.ResultPage;
import com.petstore.populate.SeedDataReader;

/**
 * Differential test: every catalog read the legacy DAO could answer, asked of both the legacy SQL (via
 * {@link LegacyCatalogOracle}) and the migrated repository, for every id and every locale.
 *
 * <p>Where the legacy SQL had no ORDER BY (items, search) results are compared as sets; the new code adds
 * an order on purpose.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LegacyParityContract {

    private static final List<String> LOCALES = List.of("en_US", "ja_JP", "zh_CN");
    private static final Comparator<Item> BY_ID = Comparator.comparing(Item::itemId);

    @Autowired
    protected CatalogRepository repository;

    @Autowired
    protected CatalogService service;

    private CatalogData.Snapshot seed;
    private LegacyCatalogOracle legacy;

    @BeforeAll
    void startOracle() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/seed/Populate-UTF8.xml")) {
            seed = new SeedDataReader().read(in).catalog();
        }
        legacy = new LegacyCatalogOracle(seed);
    }

    @AfterAll
    void stopOracle() throws Exception {
        legacy.close();
    }

    @Test
    void categoryListsMatch() throws Exception {
        for (String locale : LOCALES) {
            assertThat(repository.findCategories(0, 100, locale).list())
                    .as("categories in %s", locale)
                    .isEqualTo(legacy.getCategories(0, 100, locale).list());
        }
    }

    @Test
    void everyCategoryMatches() throws Exception {
        for (String locale : LOCALES) {
            for (var category : seed.categories()) {
                assertThat(repository.findCategory(category.id(), locale))
                        .as("%s in %s", category.id(), locale)
                        .isEqualTo(legacy.getCategory(category.id(), locale));
            }
        }
    }

    @Test
    void everyProductListMatchesIncludingOrder() throws Exception {
        for (String locale : LOCALES) {
            for (var category : seed.categories()) {
                List<Product> expected = legacy.getProducts(category.id(), 0, 100, locale).list();
                assertThat(repository.findProducts(category.id(), 0, 100, locale).list())
                        .as("products of %s in %s", category.id(), locale)
                        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("categoryId")
                        .containsExactlyElementsOf(expected);
            }
        }
    }

    @Test
    void everyProductMatches() throws Exception {
        for (String locale : LOCALES) {
            for (var product : seed.products()) {
                var expected = legacy.getProduct(product.id(), locale).orElseThrow();
                var actual = repository.findProduct(product.id(), locale).orElseThrow();
                assertThat(actual.name()).as("%s in %s", product.id(), locale).isEqualTo(expected.name());
                assertThat(actual.description()).isEqualTo(expected.description());
            }
        }
    }

    @Test
    void everyItemListMatches() throws Exception {
        for (String locale : LOCALES) {
            for (var product : seed.products()) {
                assertThat(sorted(repository.findItems(product.id(), 0, 100, locale).list()))
                        .as("items of %s in %s", product.id(), locale)
                        .usingRecursiveFieldByFieldElementComparator(moneyByValue())
                        .containsExactlyElementsOf(sorted(legacy.getItems(product.id(), locale)));
            }
        }
    }

    @Test
    void everyItemMatches() throws Exception {
        for (String locale : LOCALES) {
            for (var item : seed.items()) {
                // EST-15 has no ja_JP row in the seed, so both sides must agree it doesn't exist there.
                var expected = legacy.getItem(item.id(), locale);
                var actual = repository.findItem(item.id(), locale);
                assertThat(actual.isPresent()).as("%s exists in %s", item.id(), locale).isEqualTo(expected.isPresent());
                expected.ifPresent(e -> assertThat(actual.orElseThrow())
                        .as("%s in %s", item.id(), locale)
                        .usingRecursiveComparison(moneyByValue())
                        .isEqualTo(e));
            }
        }
    }

    @Test
    void pagingMatchesTheLegacyCursorLogic() throws Exception {
        for (var category : seed.categories()) {
            List<Product> all = legacy.getProducts(category.id(), 0, 100, "en_US").list();
            for (int count = 1; count <= 3; count++) {
                for (int start = 0; start <= all.size(); start++) {
                    ResultPage<Product> expected = LegacyCatalogOracle.page(all, start, count);
                    ResultPage<Product> actual = repository.findProducts(category.id(), start, count, "en_US");
                    assertThat(actual.list()).extracting(Product::id)
                            .as("%s start=%d count=%d", category.id(), start, count)
                            .isEqualTo(expected.list().stream().map(Product::id).toList());
                    assertThat(actual.isNextPageAvailable()).isEqualTo(expected.isNextPageAvailable());
                    assertThat(actual.getStartOfNextPage()).isEqualTo(expected.getStartOfNextPage());
                    assertThat(actual.isPreviousPageAvailable()).isEqualTo(expected.isPreviousPageAvailable());
                }
            }
        }
    }

    /**
     * Every word that appears anywhere in the searchable fields, lower-cased - the only form of keyword the
     * legacy query could ever match - plus multi-keyword combinations.
     */
    @Test
    void searchMatchesForEveryWordInTheCatalog() throws Exception {
        for (String locale : LOCALES) {
            Set<String> words = searchableWords(locale);
            assertThat(words).hasSizeGreaterThan(10);
            for (String word : words) {
                assertSearchMatches(List.of(word), locale);
            }
            List<String> list = new ArrayList<>(words);
            for (int i = 0; i + 1 < list.size(); i += 3) {
                assertSearchMatches(List.of(list.get(i), list.get(i + 1)), locale);
            }
        }
    }

    /** The one place we intentionally differ: the legacy DAO never lower-cased keywords. */
    @Test
    void capitalisedKeywordsNowMatchWhereLegacyFoundNothing() throws Exception {
        assertThat(legacy.searchItems(List.of("Angelfish"), "en_US")).isEmpty();
        assertThat(service.search("Angelfish", 0, 10, Locale.US).list()).extracting(Item::itemId)
                .containsExactly("EST-1", "EST-2");
    }

    private void assertSearchMatches(List<String> keywords, String locale) throws Exception {
        List<String> expected = legacy.searchItems(keywords, locale).stream().map(Item::itemId).sorted().toList();
        List<String> actual = repository.searchItems(Set.copyOf(keywords), 0, 100, locale).list().stream()
                .map(Item::itemId).sorted().toList();
        assertThat(actual).as("search %s in %s", keywords, locale).isEqualTo(expected);
    }

    private Set<String> searchableWords(String locale) {
        Set<String> words = new TreeSet<>();
        seed.categories().forEach(c -> words.add(c.id().toLowerCase(Locale.ROOT)));
        seed.products().forEach(p -> addWords(words, p.details().get(locale).name()));
        seed.items().stream().map(i -> i.details().get(locale)).filter(java.util.Objects::nonNull)
                .forEach(d -> addWords(words, d.description()));
        return words;
    }

    private static void addWords(Set<String> words, String text) {
        if (text == null) {
            return;
        }
        for (String w : text.toLowerCase(Locale.ROOT).split("\\s+")) {
            // A literal % or _ meant "wildcard" to the legacy LIKE; skip words where the two would disagree.
            if (!w.isBlank() && !w.contains("%") && !w.contains("_")) {
                words.add(w);
            }
        }
    }

    private static List<Item> sorted(List<Item> items) {
        return items.stream().sorted(BY_ID).toList();
    }

    private static org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration moneyByValue() {
        return org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration.builder()
                .withComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .build();
    }
}
