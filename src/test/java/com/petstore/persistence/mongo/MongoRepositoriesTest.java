package com.petstore.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.petstore.persistence.CatalogRepositoryContract;
import com.petstore.persistence.StoreRepositoriesContract;
import com.petstore.support.MongoTestContainer;

class MongoRepositoriesTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("mongo")
    @Import(MongoTestContainer.class)
    class Catalog extends CatalogRepositoryContract {

        @Autowired
        MongoTemplate mongo;

        @Test
        void pricesAreStoredAsDecimal128NotStringsOrDoubles() {
            Document est1 = mongo.getCollection("items").find(new Document("_id", "EST-1")).first();

            Object listPrice = est1.get("details", Document.class).get("en_US", Document.class).get("listPrice");
            assertThat(listPrice).isInstanceOf(Decimal128.class);
        }

        @Test
        void productQueriesUseTheCategoryIndex() {
            Document plan = mongo.getCollection("products")
                    .find(new Document("categoryId", "DOGS"))
                    .sort(new Document("details.en_US.name", 1))
                    .explain();

            assertThat(plan.toJson()).contains("IXSCAN").doesNotContain("COLLSCAN");
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("mongo")
    @Import(MongoTestContainer.class)
    class Store extends StoreRepositoriesContract {
    }
}
