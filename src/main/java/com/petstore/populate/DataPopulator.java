package com.petstore.populate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.petstore.catalog.CatalogRepository;
import com.petstore.config.PetstoreProperties;
import com.petstore.customer.CustomerRepository;
import com.petstore.order.OrderRepository;
import com.petstore.signon.UserAccount;
import com.petstore.signon.UserRepository;

/**
 * Loads the demo data on first start-up, like the legacy "enter the store" link did on first use.
 * A forced repopulate wipes customers and orders too.
 */
@Component
public class DataPopulator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataPopulator.class);

    private final PetstoreProperties properties;
    private final CatalogRepository catalog;
    private final CustomerRepository customers;
    private final UserRepository users;
    private final OrderRepository orders;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager caches;

    public DataPopulator(PetstoreProperties properties, CatalogRepository catalog, CustomerRepository customers,
                         UserRepository users, OrderRepository orders, PasswordEncoder passwordEncoder,
                         CacheManager caches) {
        this.properties = properties;
        this.catalog = catalog;
        this.customers = customers;
        this.users = users;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
        this.caches = caches;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.populate().onStartup()) {
            populate(false);
        }
    }

    public synchronized boolean populate(boolean force) {
        if (!force && catalog.countCategories() > 0) {
            return false;
        }
        SeedData seed = read(properties.populate().seedFile());

        orders.deleteAll();
        customers.deleteAll();
        users.deleteAll();
        catalog.deleteAll();

        catalog.saveAll(seed.catalog());
        seed.users().forEach(u -> users.insert(new UserAccount(u.username(), passwordEncoder.encode(u.password()))));
        seed.customers().forEach(customers::save);

        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        log.info("Populated {} categories, {} products, {} items, {} customers",
                seed.catalog().categories().size(), seed.catalog().products().size(),
                seed.catalog().items().size(), seed.customers().size());
        return true;
    }

    private static SeedData read(Resource seedFile) {
        try (InputStream in = seedFile.getInputStream()) {
            return new SeedDataReader().read(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read seed file " + seedFile, e);
        }
    }
}
