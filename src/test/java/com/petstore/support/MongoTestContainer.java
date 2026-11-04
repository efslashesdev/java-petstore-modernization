package com.petstore.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * One real MongoDB per test JVM. Spring caches the context, and with it this container, so every
 * Mongo-backed test class shares it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestContainer {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer("mongo:8.0");
    }
}
