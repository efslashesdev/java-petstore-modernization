package com.petstore.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerRepository;
import com.petstore.customer.Profile;
import com.petstore.order.OrderIdGenerator;
import com.petstore.order.OrderRepository;
import com.petstore.order.PurchaseOrder;
import com.petstore.signon.DuplicateAccountException;
import com.petstore.signon.UserAccount;
import com.petstore.signon.UserRepository;

/** Users, customers, orders and the order-id counter. Every test uses its own ids, so they can share a database. */
public abstract class StoreRepositoriesContract {

    @Autowired
    protected UserRepository users;
    @Autowired
    protected CustomerRepository customers;
    @Autowired
    protected OrderRepository orders;
    @Autowired
    protected OrderIdGenerator orderIds;

    @Test
    void usernameIsUnique() {
        String name = unique("dup");
        users.insert(new UserAccount(name, "{noop}a"));

        assertThatThrownBy(() -> users.insert(new UserAccount(name, "{noop}b")))
                .isInstanceOf(DuplicateAccountException.class);
        assertThat(users.findByUsername(name)).get().extracting(UserAccount::passwordHash).isEqualTo("{noop}a");
    }

    @Test
    void userCanBeRemoved() {
        String name = unique("gone");
        users.insert(new UserAccount(name, "{noop}x"));

        users.delete(name);

        assertThat(users.exists(name)).isFalse();
    }

    @Test
    void customerRoundTripsAndSaveReplaces() {
        String id = unique("cust");
        Customer original = customer(id, "Duke", null);
        customers.save(original);
        assertThat(customers.findById(id)).contains(original);

        Customer edited = customer(id, "Duchess", "Unit 9");
        customers.save(edited);

        assertThat(customers.findById(id)).contains(edited);
    }

    @Test
    void orderRoundTripsWithExactMoney() {
        String user = unique("buyer");
        PurchaseOrder order = order(orderIds.nextId("TEST"), user, Instant.now().truncatedTo(ChronoUnit.MILLIS));

        orders.insert(order);

        assertThat(orders.findById(order.orderId())).get().satisfies(found -> {
            assertThat(found.totalPrice()).isEqualByComparingTo("35.00");
            assertThat(found.lineItems()).hasSize(2);
            assertThat(found.lineItems().get(1).unitPrice()).isEqualByComparingTo("18.50");
            assertThat(found.billTo()).isEqualTo(order.billTo());
            assertThat(found.card().maskedNumber()).isEqualTo("**** 0001");
        });
    }

    @Test
    void orderHistoryIsNewestFirst() {
        String user = unique("history");
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        PurchaseOrder older = order(orderIds.nextId("TEST"), user, now.minusSeconds(60));
        PurchaseOrder newer = order(orderIds.nextId("TEST"), user, now);
        orders.insert(older);
        orders.insert(newer);

        assertThat(orders.findByUser(user)).extracting(PurchaseOrder::orderId)
                .containsExactly(newer.orderId(), older.orderId());
    }

    @Test
    void orderIdsArePrefixPlusCounter() {
        String prefix = "P" + UUID.randomUUID().toString().substring(0, 6);

        assertThat(orderIds.nextId(prefix)).isEqualTo(prefix + "1");
        assertThat(orderIds.nextId(prefix)).isEqualTo(prefix + "2");
    }

    @Test
    void orderIdsStayUniqueUnderConcurrency() throws Exception {
        String prefix = "C" + UUID.randomUUID().toString().substring(0, 6);
        orderIds.nextId(prefix); // make sure the counter row exists before the race
        Set<String> seen = ConcurrentHashMap.newKeySet();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 200; i++) {
                pool.submit(() -> seen.add(orderIds.nextId(prefix)));
            }
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(seen).hasSize(200);
    }

    private static String unique(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Customer customer(String id, String givenName, String street2) {
        ContactInfo contact = new ContactInfo(givenName, "Blueprints",
                new Address("1 Moon Way", street2, "Sunville", "California", "10001", "USA"),
                "duke@example.com", "555-0100");
        return new Customer(id, new Customer.Account(Customer.ACTIVE, contact,
                new CreditCard("0100-001-0001", "Duke Express", "07/2029")),
                new Profile("ja_JP", "CATS", true, false));
    }

    private static PurchaseOrder order(String orderId, String user, Instant when) {
        ContactInfo contact = new ContactInfo("Duke", "Blueprints",
                new Address("1 Moon Way", null, "Sunville", "California", "10001", "USA"), "duke@example.com", "555-0100");
        return new PurchaseOrder(orderId, user, "duke@example.com", when, "en_US", contact, contact,
                new PurchaseOrder.PaymentCard("Duke Express", "**** 0001", "07/2029"),
                List.of(new PurchaseOrder.LineItem(0, "FISH", "FI-SW-01", "EST-1", 1, new BigDecimal("16.50")),
                        new PurchaseOrder.LineItem(1, "FISH", "FI-SW-02", "EST-3", 1, new BigDecimal("18.50"))),
                new BigDecimal("35.00"), PurchaseOrder.Status.PENDING);
    }
}
