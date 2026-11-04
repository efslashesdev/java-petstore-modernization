package com.petstore.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void insert(PurchaseOrder order);

    Optional<PurchaseOrder> findById(String orderId);

    List<PurchaseOrder> findByUser(String userId);

    void deleteAll();
}
