package com.petstore.catalog;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    static final int MAX_PAGE_SIZE = 100;

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "categories", key = "#locale + ':' + #start + ':' + #count")
    public ResultPage<Category> categories(int start, int count, Locale locale) {
        return repository.findCategories(start(start), count(count), SupportedLocales.key(locale));
    }

    public Optional<Category> category(String categoryId, Locale locale) {
        return repository.findCategory(categoryId, SupportedLocales.key(locale));
    }

    @Cacheable(cacheNames = "products", key = "#categoryId + ':' + #locale + ':' + #start + ':' + #count")
    public ResultPage<Product> products(String categoryId, int start, int count, Locale locale) {
        if (categoryId == null || categoryId.isBlank()) {
            return ResultPage.empty();
        }
        return repository.findProducts(categoryId, start(start), count(count), SupportedLocales.key(locale));
    }

    public Optional<Product> product(String productId, Locale locale) {
        return repository.findProduct(productId, SupportedLocales.key(locale));
    }

    @Cacheable(cacheNames = "items", key = "#productId + ':' + #locale + ':' + #start + ':' + #count")
    public ResultPage<Item> items(String productId, int start, int count, Locale locale) {
        if (productId == null || productId.isBlank()) {
            return ResultPage.empty();
        }
        return repository.findItems(productId, start(start), count(count), SupportedLocales.key(locale));
    }

    public Optional<Item> item(String itemId, Locale locale) {
        if (itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        return repository.findItem(itemId, SupportedLocales.key(locale));
    }

    public ResultPage<Item> search(String query, int start, int count, Locale locale) {
        Set<String> keywords = keywords(query);
        if (keywords.isEmpty()) {
            return ResultPage.empty();
        }
        return repository.searchItems(keywords, start(start), count(count), SupportedLocales.key(locale));
    }

    // The legacy DAO lowered the columns but not the keywords, so "Fish" found nothing. Fixed here.
    static Set<String> keywords(String query) {
        Set<String> keywords = new LinkedHashSet<>();
        if (query != null) {
            for (String token : query.trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    keywords.add(token.toLowerCase(Locale.ROOT));
                }
            }
        }
        return keywords;
    }

    private static int start(int start) {
        return Math.max(start, 0);
    }

    private static int count(int count) {
        return Math.clamp(count, 1, MAX_PAGE_SIZE);
    }
}
