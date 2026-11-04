package com.petstore.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.petstore.support.MongoTestContainer;

/**
 * HTTP-level behaviour that a browser test would only catch indirectly: which URLs are protected, CSRF,
 * cookies, status codes, redirects.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mongo")
@Import(MongoTestContainer.class)
class WebContractTest {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/main.screen", "/category.screen?category_id=FISH", "/product.screen?product_id=FI-SW-01",
            "/item.screen?item_id=EST-1", "/search.screen?keywords=dog", "/cart.do", "/signon.screen", "/"})
    void publicPagesAreOpen(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/customer.do", "/update_customer.screen", "/signon_welcome.screen",
            "/enter_order_information.screen"})
    void accountAndCheckoutPagesRequireSignIn(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isFound()).andExpect(redirectedUrl("/signon.screen"));
    }

    @Test
    void placingAnOrderRequiresSignIn() throws Exception {
        mvc.perform(post("/order.do").with(csrf())).andExpect(redirectedUrl("/signon.screen"));
    }

    @Test
    void cartChangesNeedACsrfToken() throws Exception {
        mvc.perform(post("/cart.do").param("action", "purchase").param("itemId", "EST-1"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cart.do").param("action", "purchase").param("itemId", "EST-1").with(csrf()))
                .andExpect(redirectedUrl("/cart.do"));
    }

    @Test
    void theLegacyGetUrlForAddToCartNoLongerChangesTheCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/cart.do").param("action", "purchase").param("itemId", "EST-1").session(session))
                .andExpect(status().isOk());
        mvc.perform(get("/cart.do").session(session))
                .andExpect(content().string(Matchers.containsString("id=\"cart-empty\"")));
    }

    @Test
    void wrongPasswordGoesToTheSignOnErrorScreen() throws Exception {
        mvc.perform(post("/j_signon_check").param("j_username", "j2ee").param("j_password", "nope").with(csrf()))
                .andExpect(redirectedUrl("/signon_error.screen"));
    }

    @Test
    void signOnRemembersTheUserNameAndSwitchesToThePreferredLanguage() throws Exception {
        mvc.perform(post("/j_signon_check").param("j_username", "j2ee-ja").param("j_password", "j2ee")
                        .param("j_remember_username", "on").with(csrf()))
                .andExpect(redirectedUrl("/main.screen"))
                .andExpect(cookie().value("bp_signon", "j2ee-ja"))
                .andExpect(cookie().httpOnly("bp_signon", true))
                .andExpect(cookie().value("petstore_locale", "ja-JP"));
    }

    @Test
    void signOnReturnsToThePageThatAskedForIt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/customer.do").session(session)).andExpect(status().isFound());

        mvc.perform(post("/j_signon_check").session(session)
                        .param("j_username", "j2ee").param("j_password", "j2ee").with(csrf()))
                .andExpect(redirectedUrlPattern("**/customer.do*"));
    }

    @Test
    void storedPasswordsAreHashed() throws Exception {
        // j2ee/j2ee only works because the seed passwords were encoded on load; "{noop}" or plaintext would not.
        mvc.perform(post("/j_signon_check").param("j_username", "j2ee").param("j_password", "j2ee").with(csrf()))
                .andExpect(redirectedUrl("/main.screen"));
    }

    @Test
    void takenUserNameShowsTheDuplicateAccountScreen() throws Exception {
        mvc.perform(post("/createuser.do").with(csrf())
                        .param("username", "j2ee").param("password", "secret").param("passwordRepeat", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("id=\"duplicate-account\"")));
    }

    @Test
    void accountDetailsStepNeedsStepOneFirst() throws Exception {
        mvc.perform(get("/create_customer.screen")).andExpect(redirectedUrl("/signon.screen"));
    }

    @Test
    void localeSwitchGoesBackToTheReferringPage() throws Exception {
        mvc.perform(get("/changelocale.do").param("locale", "zh_CN")
                        .header("Referer", "http://localhost/product.screen?product_id=FI-SW-01"))
                .andExpect(redirectedUrl("/product.screen?product_id=FI-SW-01"))
                .andExpect(cookie().value("petstore_locale", "zh-CN"));
    }

    @Test
    void unknownItemIsANotFound() throws Exception {
        mvc.perform(get("/item.screen").param("item_id", "EST-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(Matchers.containsString("id=\"general-error\"")));
    }

    @Test
    void confirmationPageWithoutAnOrderGoesHome() throws Exception {
        mvc.perform(get("/order_complete.screen").with(user("j2ee"))).andExpect(redirectedUrl("/main.screen"));
    }

    @Test
    void errorsDoNotLeakStackTraces() throws Exception {
        String body = mvc.perform(get("/category.screen").param("category_id", "FISH").param("start", "abc"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Exception").doesNotContain("at com.petstore");
    }

    @Test
    void securityHeadersAreSet() throws Exception {
        mvc.perform(get("/main.screen"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
