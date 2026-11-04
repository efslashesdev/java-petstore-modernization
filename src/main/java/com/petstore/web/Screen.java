package com.petstore.web;

/**
 * The legacy screendefinitions_*.xml in code: every screen is the same template with a body, and
 * the definition says whether the "My List" column and the pet-tips banner appear on it.
 */
public enum Screen {

    MAIN("main", "title.main", true, true),
    SIGNON_WELCOME("main", "title.welcome", true, false),
    CATEGORY("category", "title.category", true, false),
    PRODUCT("product", "title.product", true, false),
    ITEM("item", "title.item", true, false),
    SEARCH("search", "title.search", true, false),
    CART("cart", "title.cart", true, true),
    SIGNON("signon", "title.signon", false, false),
    SIGNON_ERROR("signon_failed", "title.signonError", false, false),
    SIGNOFF("signoff", "title.signoff", false, false),
    DUPLICATE_ACCOUNT("duplicate_account", "title.duplicateAccount", false, false),
    CREATE_CUSTOMER("create_customer", "title.createCustomer", true, false),
    CUSTOMER("customer", "title.customer", true, false),
    UPDATE_CUSTOMER("edit_customer", "title.updateCustomer", true, false),
    ENTER_ORDER_INFORMATION("enter_order_information", "title.enterOrder", false, false),
    ORDER_COMPLETE("order_completed", "title.orderComplete", true, false),
    CART_EMPTY_ORDER_ERROR("cart_empty_order_error", "title.cartEmptyError", false, false),
    GENERAL_ERROR("general_error", "title.generalError", false, false);

    public static final String MODEL_KEY = "screen";
    public static final String LAYOUT = "layout";

    private final String body;
    private final String titleKey;
    private final boolean myList;
    private final boolean adviceBanner;

    Screen(String body, String titleKey, boolean myList, boolean adviceBanner) {
        this.body = body;
        this.titleKey = titleKey;
        this.myList = myList;
        this.adviceBanner = adviceBanner;
    }

    public String getBody() {
        return "screens/" + body;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public boolean isMyList() {
        return myList;
    }

    public boolean isAdviceBanner() {
        return adviceBanner;
    }
}
