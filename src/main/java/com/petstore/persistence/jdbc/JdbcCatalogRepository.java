package com.petstore.persistence.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.petstore.catalog.CatalogData;
import com.petstore.catalog.CatalogRepository;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.ResultPage;

/**
 * The queries are the legacy cloudscape statements from CatalogDAOSQL.xml, with OFFSET/FETCH in place
 * of scrollable result sets and an explicit order on items.
 */
@Repository
@Profile("jdbc")
class JdbcCatalogRepository implements CatalogRepository {

    private static final String ITEM_COLUMNS = """
            d.catid, a.productid, c.name, a.itemid, b.image, b.descn,
            b.attr1, b.attr2, b.attr3, b.attr4, b.attr5, b.listprice, b.unitcost
            """;

    private static final String ITEM_JOIN = """
            from item a
            join item_details b on a.itemid = b.itemid
            join product_details c on a.productid = c.productid and b.locale = c.locale
            join product d on d.productid = c.productid
            """;

    private final JdbcClient jdbc;

    JdbcCatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Category> findCategory(String categoryId, String locale) {
        return jdbc.sql("""
                        select a.catid, b.name, b.descn
                        from category a join category_details b on a.catid = b.catid
                        where b.locale = ? and a.catid = ?
                        """)
                .params(locale, categoryId)
                .query(JdbcCatalogRepository::category)
                .optional();
    }

    @Override
    public ResultPage<Category> findCategories(int start, int count, String locale) {
        List<Category> rows = jdbc.sql("""
                        select a.catid, b.name, b.descn
                        from category a join category_details b on a.catid = b.catid
                        where b.locale = ?
                        order by b.name
                        offset ? rows fetch next ? rows only
                        """)
                .params(locale, start, count + 1)
                .query(JdbcCatalogRepository::category)
                .list();
        return ResultPage.fromLookAhead(rows, start, count);
    }

    @Override
    public Optional<Product> findProduct(String productId, String locale) {
        return jdbc.sql("""
                        select a.productid, a.catid, b.name, b.descn
                        from product a join product_details b on a.productid = b.productid
                        where b.locale = ? and a.productid = ?
                        """)
                .params(locale, productId)
                .query(JdbcCatalogRepository::product)
                .optional();
    }

    @Override
    public ResultPage<Product> findProducts(String categoryId, int start, int count, String locale) {
        List<Product> rows = jdbc.sql("""
                        select a.productid, a.catid, b.name, b.descn
                        from product a join product_details b on a.productid = b.productid
                        where b.locale = ? and a.catid = ?
                        order by b.name
                        offset ? rows fetch next ? rows only
                        """)
                .params(locale, categoryId, start, count + 1)
                .query(JdbcCatalogRepository::product)
                .list();
        return ResultPage.fromLookAhead(rows, start, count);
    }

    @Override
    public Optional<Item> findItem(String itemId, String locale) {
        return jdbc.sql("select " + ITEM_COLUMNS + ITEM_JOIN + " where b.locale = ? and a.itemid = ?")
                .params(locale, itemId)
                .query(JdbcCatalogRepository::item)
                .optional();
    }

    @Override
    public ResultPage<Item> findItems(String productId, int start, int count, String locale) {
        List<Item> rows = jdbc.sql("select " + ITEM_COLUMNS + ITEM_JOIN + """
                         where b.locale = ? and a.productid = ?
                        order by a.seq
                        offset ? rows fetch next ? rows only
                        """)
                .params(locale, productId, start, count + 1)
                .query(JdbcCatalogRepository::item)
                .list();
        return ResultPage.fromLookAhead(rows, start, count);
    }

    @Override
    public ResultPage<Item> searchItems(Set<String> keywords, int start, int count, String locale) {
        StringBuilder where = new StringBuilder(" where b.locale = ? and (");
        List<Object> params = new ArrayList<>();
        params.add(locale);
        String separator = "";
        for (String keyword : keywords) {
            where.append(separator).append("""
                    lower(c.name) like ? escape '\\' or lower(d.catid) like ? escape '\\' or lower(b.descn) like ? escape '\\'
                    """);
            String pattern = "%" + escapeLike(keyword) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            separator = " or ";
        }
        where.append(") order by a.seq offset ? rows fetch next ? rows only");
        params.add(start);
        params.add(count + 1);

        List<Item> rows = jdbc.sql("select " + ITEM_COLUMNS + ITEM_JOIN + where)
                .params(params)
                .query(JdbcCatalogRepository::item)
                .list();
        return ResultPage.fromLookAhead(rows, start, count);
    }

    // In the legacy SQL a keyword like "%" matched everything.
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public long countCategories() {
        return jdbc.sql("select count(*) from category").query(Long.class).single();
    }

    @Override
    @Transactional
    public void deleteAll() {
        for (String table : List.of("item_details", "item", "product_details", "product", "category_details", "category")) {
            jdbc.sql("delete from " + table).update();
        }
    }

    @Override
    @Transactional
    public void saveAll(CatalogData.Snapshot snapshot) {
        for (var c : snapshot.categories()) {
            jdbc.sql("insert into category (catid) values (?)").param(c.id()).update();
            c.details().forEach((locale, d) -> jdbc.sql(
                            "insert into category_details (catid, locale, name, image, descn) values (?, ?, ?, ?, ?)")
                    .params(c.id(), locale, d.name(), d.image(), d.description())
                    .update());
        }
        for (var p : snapshot.products()) {
            jdbc.sql("insert into product (productid, catid) values (?, ?)").params(p.id(), p.categoryId()).update();
            p.details().forEach((locale, d) -> jdbc.sql(
                            "insert into product_details (productid, locale, name, image, descn) values (?, ?, ?, ?, ?)")
                    .params(p.id(), locale, d.name(), d.image(), d.description())
                    .update());
        }
        for (var i : snapshot.items()) {
            jdbc.sql("insert into item (itemid, productid, seq) values (?, ?, ?)")
                    .params(i.id(), i.productId(), i.seq())
                    .update();
            i.details().forEach((locale, d) -> {
                List<String> attrs = d.attributes();
                jdbc.sql("""
                                insert into item_details
                                  (itemid, locale, listprice, unitcost, image, descn, attr1, attr2, attr3, attr4, attr5)
                                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """)
                        .params(i.id(), locale, d.listPrice(), d.unitCost(), d.image(), d.description(),
                                attr(attrs, 0), attr(attrs, 1), attr(attrs, 2), attr(attrs, 3), attr(attrs, 4))
                        .update();
            });
        }
    }

    private static String attr(List<String> attributes, int index) {
        return index < attributes.size() ? attributes.get(index) : null;
    }

    private static Category category(ResultSet rs, int row) throws SQLException {
        return new Category(rs.getString("catid"), rs.getString("name"), rs.getString("descn"));
    }

    private static Product product(ResultSet rs, int row) throws SQLException {
        return new Product(rs.getString("productid"), rs.getString("catid"), rs.getString("name"), rs.getString("descn"));
    }

    private static Item item(ResultSet rs, int row) throws SQLException {
        List<String> attributes = new ArrayList<>(5);
        for (int i = 1; i <= 5; i++) {
            String value = rs.getString("attr" + i);
            if (value != null) {
                attributes.add(value);
            }
        }
        return new Item(rs.getString("itemid"), rs.getString("productid"), rs.getString("catid"),
                rs.getString("name"), attributes, rs.getString("descn"), rs.getString("image"),
                rs.getBigDecimal("listprice"), rs.getBigDecimal("unitcost"));
    }
}
