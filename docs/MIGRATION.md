# Migration notes

Working notes from moving Java Pet Store 1.3.2 to Spring Boot. Legacy paths below are relative to the original
source tree (`src/` in the Sun distribution, mirrored at github.com/rsearls/PetStore, commit `a295aed`).

## What the legacy app is

Four EARs share one source tree:

| EAR | what it is | in scope |
|-----|-----------|----------|
| `petstore.ear` | the storefront: browse, cart, sign-on, account, checkout | **yes** |
| `petstoreadmin.ear` | admin client for order approval (Swing/JNLP + servlets) | no |
| `opc.ear` | order processing center: receives orders over JMS, workflow, mail | no |
| `supplier.ear` | supplier inventory and fulfilment | no |

The storefront is `apps/petstore` (51 classes, 25 English JSPs plus Japanese and Chinese copies of most) plus the shared `components/` it pulls in
(cart, catalog, customer, address, contactinfo, creditcard, signon, uidgen, purchaseorder, lineitem, asyncsender,
servicelocator, encodingfilter, xmldocuments) and the WAF MVC framework in `waf/`.

### Scope

The brief says "petstore only". The storefront's only runtime link to OPC is the JMS message
`OrderEJBAction` sends on checkout. That send is replaced by persisting the order with status `PENDING` and
publishing an in-process event (`OrderFulfillmentNotifier`). Anything that later processes orders reads the
`orders` collection, and the storefront doesn't need to know what that is.

### How a request flowed

`GET /category.screen` → `TemplateServlet` looks the screen up in `screendefinitions_en_US.xml` → renders
`template.jsp` with `category.jsp` as the body → the JSP builds a `CatalogHelper` bean → "fast lane" straight to
`GenericCatalogDAO` → SQL from `CatalogDAOSQL.xml`.

`GET|POST /cart.do` → `MainServlet` → `mappings.xml` → `CartHTMLAction` turns the request into a `CartEvent` →
`ShoppingWebController` → `ShoppingControllerEJB` (stateful) → `mappings.xml` again → `CartEJBAction` →
`ShoppingClientFacadeEJB` (stateful) → `ShoppingCartEJB` (stateful). That is three stateful session beans and two
command layers, all local, all to put an item id in a HashMap.

## Legacy defects found while reading the code

Each one was confirmed in the source, not inferred:

| # | defect | where |
|---|--------|-------|
| L1 | Card **type and expiry date are swapped** whenever an account is created or edited. `CreditCard(number, expiryDate, cardType)` is called as `(number, type, expiry)`. | `components/creditcard/.../CreditCard.java` constructor vs `apps/petstore/.../web/actions/CustomerHTMLAction.java` `extractCreditCard()` |
| L2 | Every order is charged to a hard-coded card `1234-2334 / Duke Express / 10/2001` (with the same argument swap). | `OrderHTMLAction.perform()` |
| L3 | Form validation doesn't stop anything. Missing fields make `extractContactInfo()` return `null`, and the null is passed on, giving a NullPointerException in the EJB tier. | `CustomerHTMLAction`, `OrderHTMLAction` |
| L4 | Card validation is copy-pasted: all four checks test `creditCardNumber`. | `CustomerHTMLAction.extractCreditCard()` |
| L5 | Search is case-sensitive in practice: the SQL does `lower(name) like ?` but the keyword is never lower-cased, so "Fish" finds nothing. | `GenericCatalogDAO.searchItems()` + `CatalogDAOSQL.xml` |
| L6 | Plaintext passwords, compared with `String.equals`. | `signon/.../UserEJB.matchPassword()` |
| L7 | "Password (Repeat)" is never compared. | `CreateUserHTMLAction` |
| L8 | Sign-up creates the login on page one and the customer on page two. Abandoning page two leaves a login whose sign-in fails with a NullPointerException in `SignOnNotifier` (no customer to read the profile from). | `CreateUserEJBAction`, `SignOnNotifier.processEvent()` |
| L9 | Checkout is not protected (only `customer.do` and `signon_welcome.screen` are), so an anonymous shopper can place an order with a null user id. | `signon-config.xml`, `OrderEJBAction` |
| L10 | Cart changes are GET requests (`cart.do?action=purchase&itemId=...`), so any link or image tag can modify a shopper's cart. | `product.jsp`, `item.jsp`, `cart.jsp` |
| L11 | The order confirmation is rendered from the POST, so refresh re-submits. The cart-empty error page even tells the user this may happen. | `mappings.xml` (`order.do` → screen), `cart_empty_order_error.jsp` |
| L12 | If the JMS send fails, the exception is printed, the cart is emptied anyway and the order is lost. | `OrderEJBAction.perform()` |
| L13 | Order totals are summed in `float`. | `OrderEJBAction`, `PurchaseOrder` |
| L14 | The general error page prints the full stack trace to the browser. | `general_error.jsp` |
| L15 | The "Meow Card" option on the sign-up form posts the value `Meow Club`, which the edit form can't re-select, so editing the account silently changes the card type. | `create_customer.jsp` vs `edit_customer.jsp` |
| L16 | Stored values missing from a dropdown (seed state `CA`, expiry year `04`) are replaced by the first option the next time the account is saved. | `edit_customer.jsp` + `waf:select` |
| L17 | `getItems()` puts the product id in the item's *category* field and the category in *productId*. Nothing displays them, so it has never been visible. | `GenericCatalogDAO.getItems()` |
| L18 | Search treats `%` and `_` typed by the user as SQL wildcards. | `GenericCatalogDAO.searchItems()` |

Data quirks, kept as-is:

- Item `EST-15` (a Manx) has no Japanese row, so the Japanese store lists one Manx and the others list two.
- Product `FI-SW-01` points at `fish1.jpg`, which isn't in the distribution. Product images are never shown.
- Seed card expiry dates are `MM/YY`; anything entered through the web is `MM/YYYY`.

## Behaviour changes

The rule was to keep observable behaviour unless it is a security issue, corrupts or loses data, or crashes. Every
change from the list above:

| change | fixes | tested by |
|--------|-------|-----------|
| Card fields are named, so they can't be swapped | L1 | `CreditCardTest`, `AccountIT.editingTheAccountKeepsCardTypeAndExpiryWhereTheyBelong` |
| Orders are charged to the card on the customer's account; only the masked number is stored on the order | L2 | `CheckoutIT` |
| Bean Validation on every form; errors are shown next to the field and nothing is saved | L3, L4 | `FormValidationTest`, `CheckoutIT`, `AccountIT` |
| Keywords are lower-cased | L5 | `LegacyParityContract.capitalisedKeywordsNowMatchWhereLegacyFoundNothing`, `BrowsingIT.searchIgnoresCase` |
| Passwords are hashed (BCrypt via Spring's delegating encoder) | L6 | `WebContractTest.storedPasswordsAreHashed` |
| Password repeat must match; minimum length 4 (the demo password is 4 characters) | L7 | `FormValidationTest`, `AccountIT.passwordsMustMatchOnSignUp` |
| Login and customer are written together at the end of sign-up; the session holds only a password hash in between | L8 | `AccountIT.newCustomerSignsUpAndIsSignedIn` |
| Checkout, account edit and order confirmation require sign-in | L9 | `WebContractTest.accountAndCheckoutPagesRequireSignIn` |
| Cart changes are CSRF-protected POSTs that redirect (the buttons still look like links) | L10 | `WebContractTest.cartChangesNeedACsrfToken`, `CartIT.refreshingAfterAddingDoesNotAddAgain` |
| Post/redirect/get on checkout | L11 | `CheckoutIT.refreshingTheConfirmationDoesNotPlaceASecondOrder` |
| Order is saved before the cart is emptied; hand-off happens after | L12 | `CheckoutIT` |
| Money is `BigDecimal` / `Decimal128` | L13 | `MongoRepositoriesTest.pricesAreStoredAsDecimal128NotStringsOrDoubles` |
| Error pages show no internals | L14 | `WebContractTest.errorsDoNotLeakStackTraces` |
| One value for "Meow Card"; stored values outside a dropdown stay selectable | L15, L16 | `FormValidationTest.storedValuesOutsideTheDropDownStaySelectable` |
| Items carry the right product and category ids | L17 | `CatalogRepositoryContract.itemCarriesProductAndCategoryFromTheJoin` |
| Search keywords are matched literally | L18 | `CatalogRepositoryContract.searchTreatsWildcardCharactersLiterally` |
| Item order is the seed order, explicitly (legacy SQL had no ORDER BY) | - | `CatalogRepositoryContract.itemsComeBackInSeedOrderNotStringOrder` |
| Sign-on page suggests the demo user name but no longer pre-fills the password | - | - |
| Remember-me cookie is HttpOnly, SameSite=Lax | - | `WebContractTest` |
| Sign-out is a POST | - | `SignOnIT` |
| `count` request parameter capped at 100 rows | - | `CatalogServiceTest` |

Kept on purpose, although they look wrong. These are business calls:

- "Your Price" on the item page is the unit cost, the cart charges list price, and search results show unit cost.
- Two rows per page on category, product and search pages (`petstore.paging.list-size`).
- Adding an item that is already in the cart sets its quantity back to 1.
- The locale defaults to en_US regardless of the browser's language.

## How equivalence was checked

1. **Reading.** Descriptors first, then one request traced through the framework (above), then each action and EJB.
2. **Characterisation in tests.** Unit and contract tests pin down the rules found in step 1: paging arithmetic,
   ordering, which fields search covers, cart pricing, the sign-on redirects.
3. **Differential test against the legacy SQL** (`parity/`). The original DDL and queries run on H2 and are
   compared with both new adapters over the whole catalog. Two findings:
   - *Paging past the end.* The legacy DAO returned its shared `EMPTY_PAGE` (start 0, so no "Previous" link).
     The first version of `ResultPage` kept the requested start, which would have shown a "Previous" link on an
     empty page. Fixed to match.
   - *EST-15.* The test's own assumption that every item exists in every locale was wrong (see the data quirks).
     Both implementations already agreed; the test now compares presence as well as content.
4. **Browser tests** for the journeys a shopper actually takes, run against the real app and a real MongoDB.

What was **not** done: running the legacy EAR side by side and diffing HTML. That needs a J2EE 1.3 container,
and the parity oracle covers the data side. With more time, the next step would be the legacy EAR on JBoss 4 in
Docker, plus a crawler that compares the text content of every screen between the two.

## MongoDB model

```
categories  { _id: "FISH", details: { en_US: { name, image, description }, ja_JP: {...}, zh_CN: {...} } }
products    { _id: "FI-SW-01", categoryId: "FISH", details: { <locale>: { name, image, description } } }
items       { _id: "EST-1", productId, categoryId, seq,
              details: { <locale>: { productName, listPrice: Decimal128, unitCost: Decimal128,
                                     attributes: [..], image, description } } }
customers   { _id: "j2ee", account: { status, contactInfo: { ..., address: {...} }, creditCard: {...} },
              profile: { preferredLanguage, favoriteCategory, myList, banners } }
users       { _id: "j2ee", passwordHash }
orders      { _id: "10011", userId, email, orderDate, locale, billTo, shipTo, card: { type, maskedNumber, expiry },
              lineItems: [ { lineNumber, categoryId, productId, itemId, quantity, unitPrice } ], totalPrice, status }
counters    { _id: "1001", value }
```

Indexes (`MongoIndexes`): `products {categoryId, details.<locale>.name}` per locale, `items {productId, seq}`,
`orders {userId, orderDate}`. `MongoRepositoriesTest.productQueriesUseTheCategoryIndex` checks the product listing
query plan is an index scan.

## Problems hit along the way

- **Getting the source.** The Oracle download page blocks automated fetches. The original 1.3.2 source came from a
  public mirror instead (commit `a295aed`, "original source code for petstore1.3.2", Sun copyright headers intact).
- **Spring Boot 4 moved things.** `spring.data.mongodb.uri` is now `spring.mongodb.uri`, the auto-configuration
  classes moved to new packages (needed for the per-profile excludes), and `@AutoConfigureMockMvc` lives in
  `spring-boot-webmvc-test`. Each of these was checked against the jar metadata, not guessed.
- **H2 reserves `value`**, which broke the counter table on first run. The column is `counter_value`.
- **Testcontainers bean name.** A `@Bean MongoDBContainer mongo()` collides with Boot's own `mongo` client bean.
- **`<area>` can't be clicked** by Playwright (it has no layout box). The test clicks the image at the hotspot's
  coordinates, which is what a person does anyway.
