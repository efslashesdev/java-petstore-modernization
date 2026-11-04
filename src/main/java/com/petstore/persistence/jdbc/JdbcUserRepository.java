package com.petstore.persistence.jdbc;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.petstore.signon.DuplicateAccountException;
import com.petstore.signon.UserAccount;
import com.petstore.signon.UserRepository;

@Repository
@Profile("jdbc")
class JdbcUserRepository implements UserRepository {

    private final JdbcClient jdbc;

    JdbcUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jdbc.sql("select username, password_hash from signon where username = ?")
                .param(username)
                .query((rs, row) -> new UserAccount(rs.getString(1), rs.getString(2)))
                .optional();
    }

    @Override
    public boolean exists(String username) {
        return jdbc.sql("select count(*) from signon where username = ?").param(username)
                .query(Integer.class).single() > 0;
    }

    @Override
    public void insert(UserAccount user) {
        try {
            jdbc.sql("insert into signon (username, password_hash) values (?, ?)")
                    .params(user.username(), user.passwordHash())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateAccountException(user.username());
        }
    }

    @Override
    public void delete(String username) {
        jdbc.sql("delete from signon where username = ?").param(username).update();
    }

    @Override
    public void deleteAll() {
        jdbc.sql("delete from signon").update();
    }
}
