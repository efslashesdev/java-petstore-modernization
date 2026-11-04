package com.petstore.persistence.jdbc;

import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.petstore.persistence.CatalogRepositoryContract;
import com.petstore.persistence.StoreRepositoriesContract;

class JdbcRepositoriesTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("jdbc")
    class Catalog extends CatalogRepositoryContract {
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("jdbc")
    class Store extends StoreRepositoriesContract {
    }
}
