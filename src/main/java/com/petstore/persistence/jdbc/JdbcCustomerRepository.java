package com.petstore.persistence.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerRepository;

@Repository
@Profile("jdbc")
class JdbcCustomerRepository implements CustomerRepository {

    private final JdbcClient jdbc;

    JdbcCustomerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Customer> findById(String userId) {
        return jdbc.sql("select * from customer where userid = ?")
                .param(userId)
                .query(JdbcCustomerRepository::map)
                .optional();
    }

    @Override
    @Transactional
    public void save(Customer customer) {
        jdbc.sql("delete from customer where userid = ?").param(customer.userId()).update();

        ContactInfo contact = customer.contactInfo();
        Address address = contact.address();
        CreditCard card = customer.creditCard();
        var profile = customer.profile();
        jdbc.sql("""
                        insert into customer (userid, status, given_name, family_name, street1, street2, city, state,
                          zip, country, email, phone, card_number, card_type, card_expiry,
                          preferred_language, favorite_category, mylist_on, banners_on)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .params(customer.userId(), customer.account().status(), contact.givenName(), contact.familyName(),
                        address.street1(), address.street2(), address.city(), address.state(), address.zipCode(),
                        address.country(), contact.email(), contact.telephone(),
                        card.number(), card.type(), card.expiry(),
                        profile.preferredLanguage(), profile.favoriteCategory(), profile.myList(), profile.banners())
                .update();
    }

    @Override
    public void deleteAll() {
        jdbc.sql("delete from customer").update();
    }

    private static Customer map(ResultSet rs, int row) throws SQLException {
        ContactInfo contact = new ContactInfo(
                rs.getString("given_name"), rs.getString("family_name"),
                new Address(rs.getString("street1"), rs.getString("street2"), rs.getString("city"),
                        rs.getString("state"), rs.getString("zip"), rs.getString("country")),
                rs.getString("email"), rs.getString("phone"));
        CreditCard card = new CreditCard(rs.getString("card_number"), rs.getString("card_type"), rs.getString("card_expiry"));
        var profile = new com.petstore.customer.Profile(rs.getString("preferred_language"),
                rs.getString("favorite_category"), rs.getBoolean("mylist_on"), rs.getBoolean("banners_on"));
        return new Customer(rs.getString("userid"), new Customer.Account(rs.getString("status"), contact, card), profile);
    }
}
