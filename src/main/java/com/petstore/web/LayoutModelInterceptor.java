package com.petstore.web;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Product;
import com.petstore.config.PetstoreProperties;
import com.petstore.customer.Customer;
import com.petstore.customer.Profile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Fills in what every screen around the body needs: the sidebar categories, the sign-in state, and the
 * My List / pet-tips columns when both the screen and the customer's profile ask for them.
 */
@Component
class LayoutModelInterceptor implements HandlerInterceptor {

    private static final int SIDEBAR_SIZE = 5;

    private final CatalogService catalog;
    private final CurrentCustomer currentCustomer;
    private final PetstoreProperties properties;

    LayoutModelInterceptor(CatalogService catalog, CurrentCustomer currentCustomer, PetstoreProperties properties) {
        this.catalog = catalog;
        this.currentCustomer = currentCustomer;
        this.properties = properties;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView mav) {
        if (mav == null || !(mav.getModel().get(Screen.MODEL_KEY) instanceof Screen screen)) {
            return;
        }
        Locale locale = RequestContextUtils.getLocale(request);
        mav.addObject("sidebarCategories", catalog.categories(0, SIDEBAR_SIZE, locale).list());
        mav.addObject("signedOn", currentCustomer.isSignedOn());

        Profile profile = currentCustomer.get().map(Customer::profile).orElse(null);
        if (profile == null || profile.favoriteCategory() == null) {
            return;
        }
        if (screen.isMyList() && profile.myList()) {
            List<Product> myList = catalog.products(profile.favoriteCategory(), 0,
                    properties.paging().myListSize(), locale).list();
            mav.addObject("myList", myList);
        }
        if (screen.isAdviceBanner() && profile.banners()) {
            mav.addObject("adviceBanner", bannerFor(profile.favoriteCategory()));
        }
    }

    // Legacy BannerHelper: one banner per category, dogs as the fallback.
    static String bannerFor(String categoryId) {
        return switch (categoryId.toLowerCase(Locale.ROOT)) {
            case "cats", "reptiles", "birds", "fish" -> "banner_" + categoryId.toLowerCase(Locale.ROOT) + ".gif";
            default -> "banner_dogs.gif";
        };
    }
}
