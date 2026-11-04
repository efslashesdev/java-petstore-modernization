package com.petstore.parity;

import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.petstore.support.MongoTestContainer;

class LegacyParityTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("mongo")
    @Import(MongoTestContainer.class)
    class MongoAdapter extends LegacyParityContract {
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("jdbc")
    class JdbcAdapter extends LegacyParityContract {
    }
}
