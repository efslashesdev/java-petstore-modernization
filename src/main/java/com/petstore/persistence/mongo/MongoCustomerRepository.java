package com.petstore.persistence.mongo;

import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.petstore.customer.Customer;
import com.petstore.customer.CustomerRepository;
import com.petstore.customer.Profile;

/** One document per customer; account, contact info, address, card and profile are embedded. */
@Repository
@org.springframework.context.annotation.Profile("mongo")
class MongoCustomerRepository implements CustomerRepository {

    private final MongoTemplate mongo;

    MongoCustomerRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<Customer> findById(String userId) {
        return Optional.ofNullable(mongo.findById(userId, CustomerDocument.class)).map(CustomerDocument::toDomain);
    }

    @Override
    public void save(Customer customer) {
        mongo.save(CustomerDocument.from(customer));
    }

    @Override
    public void deleteAll() {
        mongo.remove(new Query(), CustomerDocument.class);
    }

    @Document("customers")
    record CustomerDocument(@Id String userId, Customer.Account account, Profile profile) {

        static CustomerDocument from(Customer c) {
            return new CustomerDocument(c.userId(), c.account(), c.profile());
        }

        Customer toDomain() {
            return new Customer(userId, account, profile);
        }
    }
}
