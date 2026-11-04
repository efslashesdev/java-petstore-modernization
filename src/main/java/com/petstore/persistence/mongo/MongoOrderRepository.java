package com.petstore.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.petstore.customer.ContactInfo;
import com.petstore.order.OrderRepository;
import com.petstore.order.PurchaseOrder;

/** Line items are embedded: they are always read with their order and never on their own. */
@Repository
@Profile("mongo")
class MongoOrderRepository implements OrderRepository {

    private final MongoTemplate mongo;

    MongoOrderRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void insert(PurchaseOrder order) {
        mongo.insert(OrderDocument.from(order));
    }

    @Override
    public Optional<PurchaseOrder> findById(String orderId) {
        return Optional.ofNullable(mongo.findById(orderId, OrderDocument.class)).map(OrderDocument::toDomain);
    }

    @Override
    public List<PurchaseOrder> findByUser(String userId) {
        Query query = Query.query(where("userId").is(userId)).with(Sort.by(Sort.Direction.DESC, "orderDate"));
        return mongo.find(query, OrderDocument.class).stream().map(OrderDocument::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        mongo.remove(new Query(), OrderDocument.class);
    }

    @Document("orders")
    record OrderDocument(@Id String orderId, String userId, String email, Instant orderDate, String locale,
                         ContactInfo billTo, ContactInfo shipTo, PurchaseOrder.PaymentCard card,
                         List<PurchaseOrder.LineItem> lineItems, BigDecimal totalPrice, PurchaseOrder.Status status) {

        static OrderDocument from(PurchaseOrder o) {
            return new OrderDocument(o.orderId(), o.userId(), o.email(), o.orderDate(), o.locale(), o.billTo(),
                    o.shipTo(), o.card(), o.lineItems(), o.totalPrice(), o.status());
        }

        PurchaseOrder toDomain() {
            return new PurchaseOrder(orderId, userId, email, orderDate, locale, billTo, shipTo, card, lineItems,
                    totalPrice, status);
        }
    }
}
