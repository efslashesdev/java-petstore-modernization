package com.petstore.parity;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.petstore.catalog.CatalogData;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.ResultPage;

/**
 * Answers catalog questions the way the legacy app did: the Cloudscape DDL and INSERTs from the original
 * PopulateSQL.xml and the DAO queries from the original CatalogDAOSQL.xml, both copied verbatim into
 * src/test/resources/legacy, executed on an in-memory H2 database. Statement assembly and paging are
 * ported line for line from GenericCatalogDAO.
 */
final class LegacyCatalogOracle implements AutoCloseable {

    private static final String DATABASE = "cloudscape";

    private final Connection connection;
    private final Map<String, List<Fragment>> daoStatements;

    private record Fragment(String text, int parameters, boolean variable) {
    }

    LegacyCatalogOracle(CatalogData.Snapshot seed) throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:legacy-" + UUID.randomUUID());
        daoStatements = readDaoStatements();
        createSchemaAndLoad(seed);
    }

    // --- the seven legacy DAO calls ------------------------------------------------------------

    Optional<Category> getCategory(String id, String locale) throws SQLException {
        List<Category> rows = query("GET_CATEGORY", List.of(locale, id),
                rs -> new Category(id, rs.getString(1), rs.getString(2)));
        return rows.stream().findFirst();
    }

    ResultPage<Category> getCategories(int start, int count, String locale) throws SQLException {
        return page(query("GET_CATEGORIES", List.of(locale),
                rs -> new Category(rs.getString(1).trim(), rs.getString(2), rs.getString(3))), start, count);
    }

    Optional<Product> getProduct(String id, String locale) throws SQLException {
        return query("GET_PRODUCT", List.of(locale, id), rs -> new Product(id, null, rs.getString(1), rs.getString(2)))
                .stream().findFirst();
    }

    ResultPage<Product> getProducts(String categoryId, int start, int count, String locale) throws SQLException {
        return page(query("GET_PRODUCTS", List.of(locale, categoryId),
                rs -> new Product(rs.getString(1).trim(), categoryId, rs.getString(2), rs.getString(3))), start, count);
    }

    Optional<Item> getItem(String itemId, String locale) throws SQLException {
        // columns: catid, a.productid, name, b.image, b.descn, attr1..5, listprice, unitcost
        return query("GET_ITEM", List.of(locale, itemId), rs -> new Item(itemId, rs.getString(2).trim(),
                rs.getString(1).trim(), rs.getString(3), attributes(rs, 6), rs.getString(5), rs.getString(4).trim(),
                rs.getBigDecimal(11), rs.getBigDecimal(12))).stream().findFirst();
    }

    /** Unordered: the legacy query has no ORDER BY. */
    List<Item> getItems(String productId, String locale) throws SQLException {
        // columns: catid, name, a.itemid, b.image, b.descn, attr1..5, listprice, unitcost
        return query("GET_ITEMS", List.of(locale, productId), rs -> new Item(rs.getString(3).trim(), productId,
                rs.getString(1).trim(), rs.getString(2), attributes(rs, 6), rs.getString(5), rs.getString(4).trim(),
                rs.getBigDecimal(11), rs.getBigDecimal(12)));
    }

    /** Unordered. Keywords are passed through untouched, exactly like the legacy DAO (no lower-casing). */
    List<Item> searchItems(List<String> keywords, String locale) throws SQLException {
        List<String> params = new ArrayList<>();
        params.add(locale);
        for (String k : keywords) {
            params.add("%" + k + "%");
            params.add("%" + k + "%");
            params.add("%" + k + "%");
        }
        // columns: catid, a.productid, name, a.itemid, b.image, b.descn, attr1..5, listprice, unitcost
        return query("SEARCH_ITEMS", params, rs -> new Item(rs.getString(4).trim(), rs.getString(2).trim(),
                rs.getString(1).trim(), rs.getString(3), attributes(rs, 7), rs.getString(6), rs.getString(5).trim(),
                rs.getBigDecimal(12), rs.getBigDecimal(13)));
    }

    // --- GenericCatalogDAO internals -----------------------------------------------------------

    /** The legacy do/while over a scrollable result set: absolute(start + 1), then up to count rows. */
    static <T> ResultPage<T> page(List<T> all, int start, int count) {
        if (start < 0 || start >= all.size()) {
            return ResultPage.empty();
        }
        List<T> rows = new ArrayList<>();
        int cursor = start;
        boolean hasNext;
        do {
            rows.add(all.get(cursor));
            cursor++;
            hasNext = cursor < all.size();
        } while (hasNext && (--count > 0));
        return new ResultPage<>(rows, start, hasNext);
    }

    /** buildSQLStatement(): fixed fragments once, VARIABLE fragments repeated while parameters remain. */
    private String buildSql(String method, int parameterCount) {
        StringBuilder sql = new StringBuilder();
        int remaining = parameterCount;
        for (Fragment fragment : daoStatements.get(method)) {
            if (fragment.variable()) {
                while (remaining > 0 && remaining >= fragment.parameters()) {
                    sql.append(fragment.text());
                    remaining -= fragment.parameters();
                }
            } else {
                sql.append(fragment.text());
                remaining -= fragment.parameters();
            }
        }
        return sql.toString();
    }

    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private <T> List<T> query(String method, List<String> params, RowMapper<T> mapper) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(buildSql(method, params.size()))) {
            for (int i = 0; i < params.size(); i++) {
                statement.setString(i + 1, params.get(i));
            }
            List<T> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
            }
            return rows;
        }
    }

    private static List<String> attributes(ResultSet rs, int firstColumn) throws SQLException {
        List<String> attributes = new ArrayList<>();
        for (int i = firstColumn; i < firstColumn + 5; i++) {
            String value = rs.getString(i);
            if (value != null) {
                attributes.add(value);
            }
        }
        return attributes;
    }

    // --- loading ---------------------------------------------------------------------------------

    private void createSchemaAndLoad(CatalogData.Snapshot seed) throws Exception {
        Map<String, String> creates = new HashMap<>();
        Map<String, String> inserts = new HashMap<>();
        Element db = databaseElement(parse("/legacy/PopulateSQL.xml"), "DatabaseStatements");
        NodeList tables = db.getElementsByTagName("TableStatements");
        for (int i = 0; i < tables.getLength(); i++) {
            Element table = (Element) tables.item(i);
            creates.put(table.getAttribute("table"), text(table, "CreateStatement"));
            inserts.put(table.getAttribute("table"), text(table, "InsertStatement"));
        }
        for (String table : List.of("category", "category_details", "product", "product_details", "item", "item_details")) {
            try (var statement = connection.createStatement()) {
                statement.execute(creates.get(table));
            }
        }

        for (var c : seed.categories()) {
            insert(inserts.get("category"), c.id());
            for (var e : c.details().entrySet()) {
                var d = e.getValue();
                insert(inserts.get("category_details"), c.id(), d.name(), d.image(), d.description(), e.getKey());
            }
        }
        for (var p : seed.products()) {
            insert(inserts.get("product"), p.id(), p.categoryId());
            for (var e : p.details().entrySet()) {
                var d = e.getValue();
                insert(inserts.get("product_details"), p.id(), e.getKey(), d.name(), d.image(), d.description());
            }
        }
        for (var i : seed.items()) {
            insert(inserts.get("item"), i.id(), i.productId());
            for (var e : i.details().entrySet()) {
                var d = e.getValue();
                List<String> a = d.attributes();
                insert(inserts.get("item_details"), i.id(), d.listPrice(), d.unitCost(), e.getKey(),
                        at(a, 0), at(a, 1), at(a, 2), at(a, 3), at(a, 4), d.image(), d.description());
            }
        }
    }

    private void insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof BigDecimal decimal) {
                    statement.setBigDecimal(i + 1, decimal);
                } else {
                    statement.setString(i + 1, (String) values[i]);
                }
            }
            statement.executeUpdate();
        }
    }

    private Map<String, List<Fragment>> readDaoStatements() throws Exception {
        Map<String, List<Fragment>> statements = new HashMap<>();
        Element db = databaseElement(parse("/legacy/CatalogDAOSQL.xml"), "DAOStatements");
        NodeList list = db.getElementsByTagName("SQLStatement");
        for (int i = 0; i < list.getLength(); i++) {
            Element statement = (Element) list.item(i);
            List<Fragment> fragments = new ArrayList<>();
            NodeList parts = statement.getElementsByTagName("SQLFragment");
            for (int j = 0; j < parts.getLength(); j++) {
                Element part = (Element) parts.item(j);
                fragments.add(new Fragment(" " + part.getTextContent().trim() + " ",
                        Integer.parseInt(part.getAttribute("parameterNb")),
                        "VARIABLE".equals(part.getAttribute("occurrence"))));
            }
            statements.put(statement.getAttribute("method"), fragments);
        }
        return statements;
    }

    private static Document parse(String resource) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try (InputStream in = LegacyCatalogOracle.class.getResourceAsStream(resource)) {
            return factory.newDocumentBuilder().parse(in);
        }
    }

    private static Element databaseElement(Document doc, String tag) {
        NodeList list = doc.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++) {
            Element e = (Element) list.item(i);
            if (DATABASE.equals(e.getAttribute("database"))) {
                return e;
            }
        }
        throw new IllegalStateException("No " + tag + " for " + DATABASE);
    }

    private static String text(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent().trim();
    }

    private static String at(List<String> list, int index) {
        return index < list.size() ? list.get(index) : null;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
