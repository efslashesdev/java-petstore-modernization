package com.petstore.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The catalog with every locale's details attached - the shape we load from the seed file and
 * hand to the repositories. Map keys are locale strings ({@code en_US}, {@code ja_JP}, {@code zh_CN}).
 */
public final class CatalogData {

    private CatalogData() {
    }

    public record CategoryDetails(String name, String image, String description) {
    }

    public record ProductDetails(String name, String image, String description) {
    }

    public record ItemDetails(BigDecimal listPrice, BigDecimal unitCost, List<String> attributes,
                              String image, String description) {
        public ItemDetails {
            attributes = attributes == null ? List.of() : List.copyOf(attributes);
        }
    }

    public record CategoryRecord(String id, Map<String, CategoryDetails> details) {
    }

    public record ProductRecord(String id, String categoryId, Map<String, ProductDetails> details) {
    }

    /**
     * {@code seq} is the item's position in the seed file. The legacy item queries had no ORDER BY and
     * leaned on Cloudscape returning rows in insert order; we store that order so EST-9 stays before EST-10.
     */
    public record ItemRecord(String id, String productId, int seq, Map<String, ItemDetails> details) {
    }

    public record Snapshot(List<CategoryRecord> categories, List<ProductRecord> products, List<ItemRecord> items) {
    }
}
