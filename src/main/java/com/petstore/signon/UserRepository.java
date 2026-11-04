package com.petstore.signon;

import java.util.Optional;

public interface UserRepository {

    Optional<UserAccount> findByUsername(String username);

    boolean exists(String username);

    /** Relies on the store's unique key, so two concurrent sign-ups can't both win. */
    void insert(UserAccount user) throws DuplicateAccountException;

    void delete(String username);

    void deleteAll();
}
