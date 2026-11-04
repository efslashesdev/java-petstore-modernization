package com.petstore.persistence.jdbc;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.petstore.order.OrderIdGenerator;

/** Same table-based counter as the legacy Counter CMP bean, with the row lock doing the serialisation. */
@Component
@Profile("jdbc")
class JdbcOrderIdGenerator implements OrderIdGenerator {

    private final JdbcClient jdbc;

    JdbcOrderIdGenerator(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public String nextId(String prefix) {
        int updated = jdbc.sql("update counter set counter_value = counter_value + 1 where name = ?").param(prefix).update();
        if (updated == 0) {
            jdbc.sql("insert into counter (name, counter_value) values (?, 1)").param(prefix).update();
        }
        long value = jdbc.sql("select counter_value from counter where name = ?").param(prefix).query(Long.class).single();
        return prefix + value;
    }
}
