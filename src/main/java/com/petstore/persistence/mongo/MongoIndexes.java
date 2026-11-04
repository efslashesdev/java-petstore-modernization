package com.petstore.persistence.mongo;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Indexes are declared here rather than through auto-index-creation, so the full set is visible in
 * one place and matches the queries in the repositories. createIndex is idempotent.
 */
@Component
@Profile("mongo")
class MongoIndexes {

    private final MongoTemplate mongo;

    MongoIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @EventListener(ApplicationStartedEvent.class)
    @Order(0)
    void ensureIndexes() {
        // product list per category, sorted by name in the shopper's locale
        for (String locale : new String[] {"en_US", "ja_JP", "zh_CN"}) {
            mongo.indexOps(CatalogDocuments.PRODUCTS).createIndex(
                    new Index().on("categoryId", Sort.Direction.ASC).on("details." + locale + ".name", Sort.Direction.ASC));
        }
        // item list per product, in catalog order
        mongo.indexOps(CatalogDocuments.ITEMS).createIndex(
                new Index().on("productId", Sort.Direction.ASC).on("seq", Sort.Direction.ASC));
        // order history
        mongo.indexOps("orders").createIndex(
                new Index().on("userId", Sort.Direction.ASC).on("orderDate", Sort.Direction.DESC));
    }
}
