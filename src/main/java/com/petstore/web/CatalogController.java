package com.petstore.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.petstore.catalog.CatalogService;
import com.petstore.config.PetstoreProperties;

/** Legacy *.screen URLs are kept so bookmarks and links into the store keep working. */
@Controller
class CatalogController {

    private final CatalogService catalog;
    private final int pageSize;

    CatalogController(CatalogService catalog, PetstoreProperties properties) {
        this.catalog = catalog;
        this.pageSize = properties.paging().listSize();
    }

    @GetMapping("/main.screen")
    String main(Model model) {
        return Views.render(model, Screen.MAIN);
    }

    @GetMapping("/signon_welcome.screen")
    String welcome(Model model) {
        return Views.render(model, Screen.SIGNON_WELCOME);
    }

    @GetMapping("/category.screen")
    String category(@RequestParam(name = "category_id", required = false) String categoryId,
                    @RequestParam(defaultValue = "0") int start,
                    @RequestParam(required = false) Integer count,
                    Locale locale, Model model) {
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("page", catalog.products(categoryId, start, pageSize(count), locale));
        return Views.render(model, Screen.CATEGORY);
    }

    @GetMapping("/product.screen")
    String product(@RequestParam(name = "product_id", required = false) String productId,
                   @RequestParam(defaultValue = "0") int start,
                   @RequestParam(required = false) Integer count,
                   Locale locale, Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("page", catalog.items(productId, start, pageSize(count), locale));
        return Views.render(model, Screen.PRODUCT);
    }

    @GetMapping("/item.screen")
    String item(@RequestParam(name = "item_id", required = false) String itemId, Locale locale, Model model) {
        model.addAttribute("item", catalog.item(itemId, locale)
                .orElseThrow(() -> new NotFoundException("item " + itemId)));
        return Views.render(model, Screen.ITEM);
    }

    @GetMapping("/search.screen")
    String search(@RequestParam(required = false) String keywords,
                  @RequestParam(defaultValue = "0") int start,
                  @RequestParam(required = false) Integer count,
                  Locale locale, Model model) {
        model.addAttribute("keywords", keywords);
        model.addAttribute("page", catalog.search(keywords, start, pageSize(count), locale));
        return Views.render(model, Screen.SEARCH);
    }

    // The legacy "Next" link passed count=<size of current page>; honour it like the old JSPs did.
    private int pageSize(Integer requested) {
        return requested == null ? pageSize : requested;
    }
}
