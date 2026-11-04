package com.petstore.customer;

/**
 * @param favoriteCategory drives the "My List" sidebar and the pet-tips banner
 */
public record Profile(String preferredLanguage, String favoriteCategory, boolean myList, boolean banners) {
}
