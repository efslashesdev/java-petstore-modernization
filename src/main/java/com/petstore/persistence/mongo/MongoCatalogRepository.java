package com.petstore.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.petstore.catalog.CatalogData;
import com.petstore.catalog.CatalogRepository;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.ResultPage;
import com.petstore.persistence.mongo.CatalogDocuments.CategoryDocument;
import com.petstore.persistence.mongo.CatalogDocuments.ItemDocument;
import com.petstore.persistence.mongo.CatalogDocuments.LocalizedItem;
import com.petstore.persistence.mongo.CatalogDocuments.ProductDocument;

@Repository
@Profile("mongo")
class MongoCatalogRepository implements CatalogRepository {

    private final MongoTemplate mongo;

    MongoCatalogRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<Category> findCategory(String categoryId, String locale) {
        return Optional.ofNullable(mongo.findById(categoryId, CategoryDocument.class))
                .filter(doc -> doc.details().containsKey(locale))
                .map(doc -> toCategory(doc, locale));
    }

    @Override
    public ResultPage<Category> findCategories(int start, int count, String locale) {
        Query query = Query.query(where(details(locale)).exists(true))
                .with(Sort.by(details(locale) + ".name"));
        return page(query, start, count, CategoryDocument.class, doc -> toCategory(doc, locale));
    }

    @Override
    public Optional<Product> findProduct(String productId, String locale) {
        return Optional.ofNullable(mongo.findById(productId, ProductDocument.class))
                .filter(doc -> doc.details().containsKey(locale))
                .map(doc -> toProduct(doc, locale));
    }

    @Override
    public ResultPage<Product> findProducts(String categoryId, int start, int count, String locale) {
        Query query = Query.query(where("categoryId").is(categoryId).and(details(locale)).exists(true))
                .with(Sort.by(details(locale) + ".name"));
        return page(query, start, count, ProductDocument.class, doc -> toProduct(doc, locale));
    }

    @Override
    public Optional<Item> findItem(String itemId, String locale) {
        return Optional.ofNullable(mongo.findById(itemId, ItemDocument.class))
                .filter(doc -> doc.details().containsKey(locale))
                .map(doc -> toItem(doc, locale));
    }

    @Override
    public ResultPage<Item> findItems(String productId, int start, int count, String locale) {
        Query query = Query.query(where("productId").is(productId).and(details(locale)).exists(true))
                .with(Sort.by("seq"));
        return page(query, start, count, ItemDocument.class, doc -> toItem(doc, locale));
    }

    /**
     * Unanchored, case-insensitive regexes can't use an index, so this is a collection scan. That is
     * fine for 28 items; for a real catalog this becomes an Atlas Search query (see the README).
     */
    @Override
    public ResultPage<Item> searchItems(Set<String> keywords, int start, int count, String locale) {
        String base = details(locale);
        List<Criteria> anyMatch = new ArrayList<>();
        for (String keyword : keywords) {
            Pattern contains = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
            anyMatch.add(where(base + ".productName").regex(contains));
            anyMatch.add(where("categoryId").regex(contains));
            anyMatch.add(where(base + ".description").regex(contains));
        }
        Query query = Query.query(new Criteria().andOperator(
                        where(base).exists(true),
                        new Criteria().orOperator(anyMatch)))
                .with(Sort.by("seq"));
        return page(query, start, count, ItemDocument.class, doc -> toItem(doc, locale));
    }

    @Override
    public long countCategories() {
        return mongo.estimatedCount(CategoryDocument.class);
    }

    @Override
    public void deleteAll() {
        mongo.remove(new Query(), ItemDocument.class);
        mongo.remove(new Query(), ProductDocument.class);
        mongo.remove(new Query(), CategoryDocument.class);
    }

    @Override
    public void saveAll(CatalogData.Snapshot snapshot) {
        List<CategoryDocument> categories = snapshot.categories().stream()
                .map(c -> new CategoryDocument(c.id(), c.details()))
                .toList();

        Map<String, CatalogData.ProductRecord> productsById = new HashMap<>();
        List<ProductDocument> products = new ArrayList<>();
        for (CatalogData.ProductRecord p : snapshot.products()) {
            productsById.put(p.id(), p);
            products.add(new ProductDocument(p.id(), p.categoryId(), p.details()));
        }

        List<ItemDocument> items = new ArrayList<>();
        for (CatalogData.ItemRecord i : snapshot.items()) {
            CatalogData.ProductRecord product = productsById.get(i.productId());
            if (product == null) {
                throw new IllegalArgumentException("Item " + i.id() + " references unknown product " + i.productId());
            }
            Map<String, LocalizedItem> localized = new HashMap<>();
            i.details().forEach((locale, d) -> {
                // Same rule as the legacy join (b.locale = c.locale): no product row in that locale, no item.
                CatalogData.ProductDetails productDetails = product.details().get(locale);
                if (productDetails != null) {
                    localized.put(locale, new LocalizedItem(productDetails.name(), d.listPrice(), d.unitCost(),
                            d.attributes(), d.image(), d.description()));
                }
            });
            items.add(new ItemDocument(i.id(), i.productId(), product.categoryId(), i.seq(), localized));
        }

        mongo.insertAll(categories);
        mongo.insertAll(products);
        mongo.insertAll(items);
    }

    private <D, T> ResultPage<T> page(Query query, int start, int count, Class<D> type,
                                      Function<D, T> mapper) {
        query.skip(start).limit(count + 1);
        List<T> rows = mongo.find(query, type).stream().map(mapper).toList();
        return ResultPage.fromLookAhead(rows, start, count);
    }

    private static String details(String locale) {
        return "details." + locale;
    }

    private static Category toCategory(CategoryDocument doc, String locale) {
        var d = doc.details().get(locale);
        return new Category(doc.id(), d.name(), d.description());
    }

    private static Product toProduct(ProductDocument doc, String locale) {
        var d = doc.details().get(locale);
        return new Product(doc.id(), doc.categoryId(), d.name(), d.description());
    }

    private static Item toItem(ItemDocument doc, String locale) {
        LocalizedItem d = doc.details().get(locale);
        return new Item(doc.id(), doc.productId(), doc.categoryId(), d.productName(), d.attributes(),
                d.description(), d.image(), d.listPrice(), d.unitCost());
    }
}
