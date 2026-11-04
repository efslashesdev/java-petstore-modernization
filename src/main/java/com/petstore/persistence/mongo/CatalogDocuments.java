package com.petstore.persistence.mongo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.petstore.catalog.CatalogData.CategoryDetails;
import com.petstore.catalog.CatalogData.ProductDetails;

/**
 * Six relational tables (category, product, item and a *_details table per locale for each) collapse
 * into three collections. Localized fields sit under {@code details.<locale>}, so one document read
 * replaces the legacy join on {@code (id, locale)}.
 */
final class CatalogDocuments {

    static final String CATEGORIES = "categories";
    static final String PRODUCTS = "products";
    static final String ITEMS = "items";

    private CatalogDocuments() {
    }

    @Document(CATEGORIES)
    record CategoryDocument(@Id String id, Map<String, CategoryDetails> details) {
    }

    @Document(PRODUCTS)
    record ProductDocument(@Id String id, String categoryId, Map<String, ProductDetails> details) {
    }

    /**
     * {@code categoryId} and the localized {@code productName} are copied from the product. Item lists,
     * item pages, the cart and search all need them, and the catalog is only written by the populator,
     * so the copy never drifts. In exchange every item read is a single-collection query.
     */
    @Document(ITEMS)
    record ItemDocument(@Id String id, String productId, String categoryId, int seq,
                        Map<String, LocalizedItem> details) {
    }

    record LocalizedItem(String productName, BigDecimal listPrice, BigDecimal unitCost,
                         List<String> attributes, String image, String description) {
    }
}
