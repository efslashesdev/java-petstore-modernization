package com.petstore.persistence.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.order.OrderRepository;
import com.petstore.order.PurchaseOrder;

@Repository
@Profile("jdbc")
class JdbcOrderRepository implements OrderRepository {

    private final JdbcClient jdbc;

    JdbcOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void insert(PurchaseOrder order) {
        jdbc.sql("""
                        insert into purchase_order (orderid, userid, email, order_date, locale,
                          card_type, card_masked, card_expiry, total_price, status)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .params(order.orderId(), order.userId(), order.email(), Timestamp.from(order.orderDate()),
                        order.locale(), order.card().type(), order.card().maskedNumber(), order.card().expiry(),
                        order.totalPrice(), order.status().name())
                .update();
        insertContact(order.orderId(), "BILL", order.billTo());
        insertContact(order.orderId(), "SHIP", order.shipTo());
        for (PurchaseOrder.LineItem line : order.lineItems()) {
            jdbc.sql("""
                            insert into line_item (orderid, linenum, catid, productid, itemid, quantity, unitprice)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """)
                    .params(order.orderId(), line.lineNumber(), line.categoryId(), line.productId(), line.itemId(),
                            line.quantity(), line.unitPrice())
                    .update();
        }
    }

    @Override
    public Optional<PurchaseOrder> findById(String orderId) {
        return jdbc.sql("select * from purchase_order where orderid = ?")
                .param(orderId)
                .query(this::map)
                .optional();
    }

    @Override
    public List<PurchaseOrder> findByUser(String userId) {
        return jdbc.sql("select * from purchase_order where userid = ? order by order_date desc")
                .param(userId)
                .query(this::map)
                .list();
    }

    @Override
    @Transactional
    public void deleteAll() {
        jdbc.sql("delete from line_item").update();
        jdbc.sql("delete from order_contact").update();
        jdbc.sql("delete from purchase_order").update();
    }

    private void insertContact(String orderId, String kind, ContactInfo c) {
        Address a = c.address();
        jdbc.sql("""
                        insert into order_contact (orderid, kind, given_name, family_name, street1, street2, city,
                          state, zip, country, email, phone)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .params(orderId, kind, c.givenName(), c.familyName(), a.street1(), a.street2(), a.city(),
                        a.state(), a.zipCode(), a.country(), c.email(), c.telephone())
                .update();
    }

    private PurchaseOrder map(ResultSet rs, int row) throws SQLException {
        String orderId = rs.getString("orderid");
        Map<String, ContactInfo> contacts = jdbc.sql("select * from order_contact where orderid = ?")
                .param(orderId)
                .query((c, n) -> Map.entry(c.getString("kind"), new ContactInfo(
                        c.getString("given_name"), c.getString("family_name"),
                        new Address(c.getString("street1"), c.getString("street2"), c.getString("city"),
                                c.getString("state"), c.getString("zip"), c.getString("country")),
                        c.getString("email"), c.getString("phone"))))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<PurchaseOrder.LineItem> lines = jdbc.sql("select * from line_item where orderid = ? order by linenum")
                .param(orderId)
                .query((l, n) -> new PurchaseOrder.LineItem(l.getInt("linenum"), l.getString("catid"),
                        l.getString("productid"), l.getString("itemid"), l.getInt("quantity"), l.getBigDecimal("unitprice")))
                .list();
        return new PurchaseOrder(orderId, rs.getString("userid"), rs.getString("email"),
                rs.getTimestamp("order_date").toInstant(), rs.getString("locale"),
                contacts.get("BILL"), contacts.get("SHIP"),
                new PurchaseOrder.PaymentCard(rs.getString("card_type"), rs.getString("card_masked"), rs.getString("card_expiry")),
                lines, rs.getBigDecimal("total_price"), PurchaseOrder.Status.valueOf(rs.getString("status")));
    }
}
