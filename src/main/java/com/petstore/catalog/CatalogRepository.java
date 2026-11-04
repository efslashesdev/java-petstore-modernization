package com.petstore.catalog;

import java.util.Optional;
import java.util.Set;

/**
 * Same seven reads as the legacy {@code CatalogDAO}. Implemented for MongoDB and for the original
 * relational schema; {@code CatalogRepositoryContract} runs the same assertions against both.
 */
public interface CatalogRepository {

    Optional<Category> findCategory(String categoryId, String locale);

    ResultPage<Category> findCategories(int start, int count, String locale);

    Optional<Product> findProduct(String productId, String locale);

    ResultPage<Product> findProducts(String categoryId, int start, int count, String locale);

    Optional<Item> findItem(String itemId, String locale);

    ResultPage<Item> findItems(String productId, int start, int count, String locale);

    /** Matches ANY keyword against product name, category id or item description. Keywords arrive lower-cased. */
    ResultPage<Item> searchItems(Set<String> keywords, int start, int count, String locale);

    long countCategories();

    void deleteAll();

    void saveAll(CatalogData.Snapshot snapshot);
}
