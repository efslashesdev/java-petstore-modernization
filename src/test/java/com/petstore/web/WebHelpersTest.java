package com.petstore.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class WebHelpersTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    {
        request.setServerName("localhost");
    }

    @Test
    void localeSwitchReturnsToThePageYouWereOn() {
        assertThat(LocaleController.sameSitePath("http://localhost:8080/product.screen?product_id=FI-SW-01", request))
                .isEqualTo("/product.screen?product_id=FI-SW-01");
    }

    @Test
    void localeSwitchNeverRedirectsOffSite() {
        assertThat(LocaleController.sameSitePath("https://evil.example/phish", request)).isEqualTo("/main.screen");
        assertThat(LocaleController.sameSitePath("//evil.example/phish", request)).isEqualTo("/main.screen");
        assertThat(LocaleController.sameSitePath("not a uri", request)).isEqualTo("/main.screen");
        assertThat(LocaleController.sameSitePath(null, request)).isEqualTo("/main.screen");
    }

    @Test
    void bannerMatchesTheLegacyHelper() {
        assertThat(LayoutModelInterceptor.bannerFor("REPTILES")).isEqualTo("banner_reptiles.gif");
        assertThat(LayoutModelInterceptor.bannerFor("DOGS")).isEqualTo("banner_dogs.gif");
        assertThat(LayoutModelInterceptor.bannerFor("UNKNOWN")).isEqualTo("banner_dogs.gif");
    }
}
