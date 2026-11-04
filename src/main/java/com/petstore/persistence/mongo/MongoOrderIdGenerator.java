package com.petstore.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.petstore.order.OrderIdGenerator;

/** A single findAndModify with $inc and upsert: atomic across app instances without any locking of ours. */
@Component
@Profile("mongo")
class MongoOrderIdGenerator implements OrderIdGenerator {

    private final MongoTemplate mongo;

    MongoOrderIdGenerator(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nextId(String prefix) {
        Counter counter = mongo.findAndModify(
                Query.query(where("_id").is(prefix)),
                new Update().inc("value", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Counter.class);
        return prefix + counter.value();
    }

    @Document("counters")
    record Counter(@Id String name, long value) {
    }
}
