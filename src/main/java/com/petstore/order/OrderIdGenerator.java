package com.petstore.order;

/**
 * Legacy UniqueIdGenerator + Counter EJBs: ids are the prefix followed by a per-prefix counter,
 * so prefix "1001" yields 10011, 10012, ... Implementations must be atomic across instances.
 */
public interface OrderIdGenerator {

    String nextId(String prefix);
}
