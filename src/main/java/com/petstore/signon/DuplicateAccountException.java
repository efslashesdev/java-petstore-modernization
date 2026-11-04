package com.petstore.signon;

public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException(String username) {
        super("User name already in use: " + username);
    }
}
