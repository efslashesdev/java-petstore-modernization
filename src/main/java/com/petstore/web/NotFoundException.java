package com.petstore.web;

class NotFoundException extends RuntimeException {

    NotFoundException(String what) {
        super("Not found: " + what);
    }
}
