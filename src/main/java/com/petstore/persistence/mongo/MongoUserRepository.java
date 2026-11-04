package com.petstore.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.petstore.signon.DuplicateAccountException;
import com.petstore.signon.UserAccount;
import com.petstore.signon.UserRepository;

/** Kept apart from customers so credential reads during sign-in never load (or log) profile data. */
@Repository
@Profile("mongo")
class MongoUserRepository implements UserRepository {

    private final MongoTemplate mongo;

    MongoUserRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(mongo.findById(username, UserDocument.class))
                .map(u -> new UserAccount(u.username(), u.passwordHash()));
    }

    @Override
    public boolean exists(String username) {
        return mongo.exists(Query.query(where("_id").is(username)), UserDocument.class);
    }

    @Override
    public void insert(UserAccount user) {
        try {
            mongo.insert(new UserDocument(user.username(), user.passwordHash()));
        } catch (DuplicateKeyException e) {
            throw new DuplicateAccountException(user.username());
        }
    }

    @Override
    public void delete(String username) {
        mongo.remove(Query.query(where("_id").is(username)), UserDocument.class);
    }

    @Override
    public void deleteAll() {
        mongo.remove(new Query(), UserDocument.class);
    }

    @Document("users")
    record UserDocument(@Id String username, String passwordHash) {
    }
}
