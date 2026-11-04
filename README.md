# Java Pet Store, migrated

Sun's Java Pet Store 1.3.2 (J2EE 1.3, EJB 2.0 CMP, JSP/JSTL, Cloudscape) moved to **Spring Boot 4.1 on Java 21**,
with **MongoDB** as the primary database. Only the `petstore` storefront is in scope; the admin client, the order
processing center (OPC) and the supplier application are separate apps in the original distribution and were left
out on purpose (see [docs/MIGRATION.md](docs/MIGRATION.md#scope)).

The storefront behaves like the original: same screens, same URLs (`/category.screen`, `/cart.do`, ...), same three
languages, same demo data loaded from the original `Populate-UTF8.xml`. Where the original was broken, it was
fixed on purpose and the change is listed in the [behaviour changes](docs/MIGRATION.md#behaviour-changes) ledger.

## Running it

You need **JDK 21** and **Docker**.

```bash
docker compose up -d mongo          # MongoDB 8 on localhost:27017
./mvnw spring-boot:run              # app on http://localhost:8080
```

Or run everything in containers:

```bash
docker compose --profile app up --build
```

The catalog and four demo customers are loaded on first start. Sign in with any of them, password `j2ee`:

| user      | notes                                         |
|-----------|-----------------------------------------------|
| `j2ee`    | English, favourite category Reptiles, My List on |
| `shopper` | same profile as `j2ee`                        |
| `j2ee-ja` | Japanese - the store switches language on sign-in |
| `j2ee-zh` | Chinese                                       |

### Without MongoDB

The app also runs on the original relational schema (H2 in memory), which needs nothing but the JDK:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=jdbc
```

### Configuration

| setting                       | default                                | what it does |
|-------------------------------|----------------------------------------|--------------|
| `SPRING_PROFILES_ACTIVE`      | `mongo`                                | `mongo` or `jdbc` - picks the persistence adapter |
| `MONGODB_URI`                 | `mongodb://localhost:27017/petstore`   | MongoDB connection string (Atlas works too) |
| `JDBC_URL` / `JDBC_USERNAME` / `JDBC_PASSWORD` | in-memory H2          | relational database for the `jdbc` profile |
| `PETSTORE_ALLOW_REPOPULATE`   | `true`                                 | whether the landing page may wipe and reload the demo data. Turn off anywhere real |

Everything else is in [`application.yml`](src/main/resources/application.yml).

## Tests

```bash
./mvnw test      # unit, repository contract, legacy parity and HTTP tests (needs Docker)
./mvnw verify    # the above plus browser end-to-end tests - about 40s for all 135 on a laptop
```

Docker is used by Testcontainers to start a throwaway MongoDB. The first `verify` downloads Chromium for
Playwright. Add `-De2e.headed=true` to watch the browser; screenshots of failed browser tests are written to
`target/e2e-artifacts/`.

What each layer is for:

| layer | where | what it proves |
|-------|-------|----------------|
| Unit | `catalog`, `cart`, `customer`, `populate`, `web.form` | the rules: paging arithmetic, pricing, validation, the fixed legacy bugs |
| Repository contract | `persistence/*Contract` | MongoDB and JDBC adapters behave identically - one suite, run against both |
| Legacy parity | `parity/LegacyParityTest` | every catalog query matches the **original SQL** run on the **original schema** |
| HTTP | `web/WebContractTest` | security rules, CSRF, cookies, status codes, redirects |
| End to end | `e2e/*IT` | real browser journeys against the booted app and a real MongoDB |

The parity test is the important one. `LegacyCatalogOracle` loads the Cloudscape DDL and INSERT statements from the
original `PopulateSQL.xml`, runs the DAO queries from the original `CatalogDAOSQL.xml` with the statement-building
and cursor-paging logic of `GenericCatalogDAO` ported line for line, and compares the answers with the new
repositories: every category, product and item, in all three locales, every page offset, and a search for every
word that appears in the catalog. During the migration it caught one real behaviour difference (paging past the
end) and surfaced a gap in the seed data. Both are covered in
[docs/MIGRATION.md](docs/MIGRATION.md#how-equivalence-was-checked).

## How the code is organised

```
com.petstore
├── catalog       categories, products, items; CatalogService (+ caching); the CatalogRepository port
├── cart          session-scoped ShoppingCart and pricing
├── customer      Customer aggregate (account, contact info, card, profile) and CustomerService
├── signon        credentials and the Spring Security user lookup
├── order         PurchaseOrder, OrderService, order ids, OrderPlacedEvent
├── populate      reads the legacy seed XML and loads it
├── persistence
│   ├── mongo     MongoDB adapters (profile "mongo")
│   └── jdbc      relational adapters on the legacy schema (profile "jdbc")
├── web           controllers, forms, the Screen layout definitions, error mapping
└── config        security, locale, caching, properties
```

Templates are under `src/main/resources/templates`. `layout.html` is the old `template.jsp`, and each file in
`screens/` is one of the old body JSPs. The `Screen` enum takes the place of `screendefinitions_*.xml`.

Where the legacy pieces went:

| legacy | now |
|--------|-----|
| WAF `MainServlet` + `mappings.xml` + `*HTMLAction` + `*EJBAction` + flow handlers | Spring MVC controllers in `web` |
| `TemplateServlet` + `screendefinitions_*.xml` + `template.jsp` | `Screen` enum + `layout.html` |
| `SignOnFilter` + `signon-config.xml` + `SignOnEJB` / `UserEJB` | Spring Security (`SecurityConfig`, `SignOnSuccessHandler`) |
| `ShoppingControllerEJB`, `ShoppingClientFacadeEJB` (stateful session beans) | gone - plain services plus a session-scoped cart |
| `ShoppingCartEJB` (stateful) | `ShoppingCart` (`@SessionScope`) + `CartService` |
| `CatalogEJB` / `CatalogHelper` / `GenericCatalogDAO` + `CatalogDAOSQL.xml` | `CatalogService` + `CatalogRepository` |
| Customer, Account, ContactInfo, Address, CreditCard, Profile CMP beans | one `Customer` aggregate, one document |
| `UniqueIdGeneratorEJB` + `CounterEJB` | `OrderIdGenerator` (atomic `$inc` in MongoDB) |
| `AsyncSenderEJB` → JMS queue → OPC | order stored as `PENDING`, then `OrderPlacedEvent` |
| `PopulateServlet` + 14 populator classes | `SeedDataReader` + `DataPopulator` |
| `waf:cache` fragment caching | Spring Cache with Caffeine, same 5-minute expiry |
| JSP per language (`ja/`, `zh/`) | one template set + `messages_*.properties` |

Questions like "where is X":

- backend validation for a form field: the annotations on `web/form/ContactForm`, `CustomerForm`, `NewUserForm`
- which URLs need sign-in: `config/SecurityConfig`
- environment-specific settings: `application.yml` (profiles `mongo` and `jdbc`, overridable by environment variables)
- MongoDB document shapes: `persistence/mongo/*Document*` and the repository classes next to them; indexes in `MongoIndexes`
- legacy SQL as it was: `persistence/jdbc/JdbcCatalogRepository` (ported) and `src/test/resources/legacy/` (verbatim)

## How the migration was approached

The plan was built for a codebase much larger than this one:

1. **Inventory before code.** Read the deployment descriptors first (`web.xml`, `ejb-jar.xml`, `mappings.xml`,
   `screendefinitions`, `signon-config.xml`). They list every entry point, screen, bean and resource the app has,
   which is far quicker than reading 283 classes. Then trace one request end to end through the WAF framework.
2. **Write down the behaviour, bugs included.** Before porting anything, record what the old code really does.
   That includes the bugs, because users may depend on them. Each one goes in the ledger as either kept or fixed,
   with a reason.
3. **Put a port where the risk is.** The database change is the riskiest part, so persistence sits behind
   interfaces from day one. The first adapter keeps the old schema and SQL: that is the "lift". The MongoDB adapter
   is the "shift". One shared contract suite and the legacy-SQL oracle keep both honest. On a large system this is
   what lets you move the runtime and the database in separate releases rather than one big-bang cut-over.
4. **Migrate by vertical slice.** Catalog browsing (read-only, lowest risk), then cart, then sign-on, then account,
   then checkout. Each slice ships with its tests.
5. **Keep the outside contract stable.** URLs, form field names on sign-in, cookie name, page sizes. On a real
   estate there are bookmarks, emails and partner links pointing at these.
6. **Check equivalence by running both.** The differential test asks the old queries and the new code the same
   questions and compares the answers. Hand-written expectations would only cover the cases someone thought of.

## Trade-offs

These are the decisions that shaped the code, and what each one cost.

**Server-rendered pages, not a SPA.** The legacy app is server-rendered JSP; Thymeleaf maps onto that almost one to
one. A React/Angular rewrite would have changed every screen at the same time as the runtime and the database,
leaving no way to tell a migration bug from a redesign bug. The cost is a dated UI. That is a separate project and
a lower-risk one once this is in place. The services have no web dependencies, so a JSON API can be added next to
the pages later.

**Modular monolith, not services.** Every EJB in the original used *local* interfaces; nothing was ever called
remotely. The layering was there for the EJB container, not for distribution. Splitting into services would add
network failure modes the original never had. The packages follow the old component boundaries (catalog, cart,
customer, signon, order), so a later split has obvious seams.

**Two persistence adapters.** Writing the relational adapter cost time, and in production you would retire it.
In return it made the runtime move testable on its own, before any data moved. It is also the reference that
the MongoDB adapter is checked against. For a migration with real data, that intermediate step is where most of
the risk reduction comes from.

**Document model.**
- *Customer = one document.* Six CMP tables that were always loaded together become one read.
- *Localized fields nested under `details.<locale>`* rather than a document per locale. Fewer documents, and a
  category/product/item read is one lookup. The cost: sorting by name needs a path per locale (`details.en_US.name`),
  so the product index is created once per locale. Fine for three locales, awkward for thirty. At that point
  separate per-locale documents would be the better shape.
- *Product name and category copied into each item.* Item pages, the cart and search all need them. Copying them
  turns the legacy four-way join into a single-collection query. The catalog is only written by the populator, so
  the copies can't drift. With a catalog admin tool, a product rename would have to update its items too.
- *Order lines embedded in the order.* They are never read on their own.
- *Prices as `Decimal128`.* The legacy code totalled orders in `float`.

**Search is a regex scan.** It matches the legacy "substring anywhere, any keyword" semantics exactly, and the parity
test proves it. An unanchored regex can't use an index, so it scans the items collection. That is fine for 28 items;
for a real catalog the next step is Atlas Search, or a `$text` index if word-level matching is acceptable. Both
change the matching rules, which is why that wasn't done as part of the migration.

**Parity first, fixes listed.** The rule was: keep observable behaviour unless it is a security problem, loses data,
or crashes. Some odd behaviour is deliberately kept:
- the item page shows unit cost as "Your Price", but the cart charges list price
- the search results show unit cost
- pages hold 2 rows
- adding an item that is already in the cart resets its quantity to 1

These are business decisions, not migration decisions, and they are listed so someone can make them.

**No distributed transaction on sign-up.** Credentials and customer are separate collections, and MongoDB
multi-document transactions need a replica set. Sign-up writes the user, then the customer, and deletes the user if
the customer write fails. Running Atlas or a replica set would let this become a transaction. It's a two-line
change, but it would make the local single-node setup harder.

**Order hand-off is an in-process event.** The legacy JMS send could fail silently after the cart was emptied, and
the order was simply lost. Now the order is saved first (status `PENDING`) and an event is published. Wiring OPC
would mean a consumer reading `PENDING` orders, either by polling or with a change stream. A true transactional
outbox wasn't needed, because the order document is the outbox.

**Session state stays in memory.** The cart is session-scoped, as the legacy stateful bean was. More than one
instance needs sticky sessions or Spring Session (MongoDB or Redis). Caching is in-process too, with the same
5-minute expiry the JSPs used, so after a repopulate other instances can show the old catalog for up to 5 minutes.

## Known gaps

- The legacy application itself is not deployed anywhere in this repo. The legacy behaviour baseline comes from
  executing its SQL and schema verbatim (the parity oracle) and from reading the code. Deploying the original EAR
  needs a J2EE 1.3 server (the RI, or JBoss 4 / GlassFish v2 with rewritten vendor descriptors).
- No order history page. The legacy storefront had none either (it lived in OPC), but `OrderRepository.findByUser`
  is there for it.
- Credit card numbers are still stored on the customer account, as before; orders store only the masked number.
  A real store would hand cards to a payment provider and keep a token.
