package com.petstore.populate;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.petstore.catalog.CatalogData.CategoryDetails;
import com.petstore.catalog.CatalogData.CategoryRecord;
import com.petstore.catalog.CatalogData.ItemDetails;
import com.petstore.catalog.CatalogData.ItemRecord;
import com.petstore.catalog.CatalogData.ProductDetails;
import com.petstore.catalog.CatalogData.ProductRecord;
import com.petstore.catalog.CatalogData.Snapshot;
import com.petstore.catalog.SupportedLocales;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.Profile;

/**
 * Reads the legacy {@code Populate-UTF8.xml}, unchanged from the original distribution. Using the
 * same file for both persistence adapters is what lets us compare them (and the legacy app) row for row.
 */
public class SeedDataReader {

    public SeedData read(InputStream xml) throws IOException {
        Element root = parse(xml).getDocumentElement();
        return new SeedData(
                readUsers(child(root, "Users")),
                readCustomers(child(root, "Customers")),
                readCatalog(child(root, "Catalog")));
    }

    private List<SeedData.SeedUser> readUsers(Element users) {
        return children(users, "User").stream()
                .map(u -> new SeedData.SeedUser(u.getAttribute("id"), text(u, "Password")))
                .toList();
    }

    private List<Customer> readCustomers(Element customers) {
        List<Customer> result = new ArrayList<>();
        for (Element c : children(customers, "Customer")) {
            Element account = child(c, "Account");
            Element contact = child(account, "ContactInfo");
            Element address = child(contact, "Address");
            List<Element> streets = children(address, "StreetName");
            Element card = child(account, "CreditCard");
            Element profile = child(c, "Profile");

            ContactInfo contactInfo = new ContactInfo(
                    text(contact, "GivenName"),
                    text(contact, "FamilyName"),
                    new Address(
                            streets.isEmpty() ? null : streets.get(0).getTextContent().trim(),
                            streets.size() < 2 ? null : streets.get(1).getTextContent().trim(),
                            text(address, "City"),
                            text(address, "State"),
                            text(address, "ZipCode"),
                            text(address, "Country")),
                    text(contact, "Email"),
                    text(contact, "Phone"));

            result.add(new Customer(c.getAttribute("id"),
                    new Customer.Account(Customer.ACTIVE, contactInfo,
                            new CreditCard(text(card, "CardNumber"), text(card, "CardType"), text(card, "ExpiryDate"))),
                    new Profile(
                            text(profile, "PreferredLanguage"),
                            text(profile, "FavoriteCategory"),
                            Boolean.parseBoolean(text(profile, "MyListPreference")),
                            Boolean.parseBoolean(text(profile, "BannerPreference")))));
        }
        return result;
    }

    private Snapshot readCatalog(Element catalog) {
        List<CategoryRecord> categories = new ArrayList<>();
        for (Element c : children(child(catalog, "Categories"), "Category")) {
            Map<String, CategoryDetails> details = new LinkedHashMap<>();
            for (Element d : children(c, "CategoryDetails")) {
                details.put(locale(d), new CategoryDetails(text(d, "Name"), text(d, "Image"), text(d, "Description")));
            }
            categories.add(new CategoryRecord(c.getAttribute("id"), details));
        }

        List<ProductRecord> products = new ArrayList<>();
        for (Element p : children(child(catalog, "Products"), "Product")) {
            Map<String, ProductDetails> details = new LinkedHashMap<>();
            for (Element d : children(p, "ProductDetails")) {
                details.put(locale(d), new ProductDetails(text(d, "Name"), text(d, "Image"), text(d, "Description")));
            }
            products.add(new ProductRecord(p.getAttribute("id"), p.getAttribute("category"), details));
        }

        List<ItemRecord> items = new ArrayList<>();
        for (Element i : children(child(catalog, "Items"), "Item")) {
            Map<String, ItemDetails> details = new LinkedHashMap<>();
            for (Element d : children(i, "ItemDetails")) {
                List<String> attributes = children(d, "Attribute").stream()
                        .map(a -> a.getTextContent().trim())
                        .toList();
                details.put(locale(d), new ItemDetails(
                        new BigDecimal(text(d, "ListPrice")),
                        new BigDecimal(text(d, "UnitCost")),
                        attributes,
                        text(d, "Image"),
                        text(d, "Description")));
            }
            items.add(new ItemRecord(i.getAttribute("id"), i.getAttribute("product"), items.size(), details));
        }
        return new Snapshot(categories, products, items);
    }

    // Seed file says xml:lang="en-US"; the database always stored en_US.
    private static String locale(Element details) {
        return SupportedLocales.key(SupportedLocales.parse(details.getAttribute("xml:lang")));
    }

    private static Document parse(InputStream xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // The file declares a DOCTYPE that pulls in DTDs by relative path. We don't validate, and we
            // never want the parser fetching anything, so external DTDs and entities are switched off.
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xml);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Unreadable seed file", e);
        }
    }

    private static Element child(Element parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("<" + parent.getTagName() + "> has no <" + name + ">");
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(name)) {
                result.add(e);
            }
        }
        return result;
    }

    private static String text(Element parent, String name) {
        for (Element e : children(parent, name)) {
            return e.getTextContent().trim();
        }
        return null;
    }
}
