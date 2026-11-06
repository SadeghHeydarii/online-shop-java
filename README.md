# Online Shop Java

A console-based shopping application built with Java 21. The project implements customer accounts, a product catalog, shopping carts, checkout, simulated payments, order management, and product reviews.

Developed for the **Principles of Software Design** course at **Amirkabir University of Technology**, Fall 2026, this repository contains the Java implementation stage of the course project. It separates domain models, application services, and terminal menus, with manual dependency wiring in `Main`.

All application data is held in memory. Each run starts with sample accounts and catalog data; changes are lost when the program exits. No database or external service configuration is required.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Demo accounts and sample data](#demo-accounts-and-sample-data)
- [Example shopping workflow](#example-shopping-workflow)
- [Order and payment behavior](#order-and-payment-behavior)
- [Architecture and project structure](#architecture-and-project-structure)
- [Testing](#testing)
- [Documentation](#documentation)
- [Scope and limitations](#scope-and-limitations)

## Features

### Guests

- Browse active products and categories.
- Search product names and descriptions.
- Filter by category and price range, then sort by price, rating, creation date, or popularity.
- View product details, image URLs, ratings, and customer reviews.
- Register with an email address, a phone number, or both, and log in using either identifier.
- Recover a password through a simulated verification-code flow.

### Customers

- Add products to a cart, change quantities, remove items, and view the subtotal.
- Manage profile details and add, edit, remove, or select a default delivery address.
- Create orders with a delivery address and an optional discount coupon.
- Simulate successful or failed payments and retry pending or failed orders.
- View their own order history, inspect order details, and cancel eligible orders.
- Submit ratings and reviews for products in a `PAID`, `PROCESSING`, `SHIPPED`, or `DELIVERED` order.

### Administrators

- View all products, including inactive products.
- Create products, edit their details, and deactivate catalog entries.
- Adjust inventory and manage product image URLs.
- Inspect orders, advance fulfillment status, and cancel eligible orders.
- List all store orders or filter them by status.

The customer menu also provides the same search, filtering, category browsing, and product-detail tools available to guests. Administrator menus require the `ADMIN` role.

## Requirements

| Tool | Version or requirement |
| --- | --- |
| Java Development Kit | JDK 21 |
| Apache Maven | 3.9 or later |
| Terminal | UTF-8 support recommended |
| Test framework | JUnit Jupiter 5.10.2, resolved by Maven |

The application uses the Java standard library at runtime. Maven downloads the build plugins and test dependencies on the first build. Any IDE with Java 21 and Maven support can be used.

## Getting started

Clone the repository and open its root directory:

```bash
git clone https://github.com/SadeghHeydarii/online-shop-java.git
cd online-shop-java
```

Check that both Java and Maven use JDK 21:

```bash
java -version
mvn -version
```

Compile and start the application:

```bash
mvn compile exec:java
```

The guest menu opens in the terminal. Enter the number beside an action and follow the prompts. Option `0` returns to the previous menu, logs out, or exits, as indicated by the current menu.

To build the project and run its tests:

```bash
mvn clean package
```

The resulting JAR can be started by specifying the main class:

```bash
java -ea -cp target/online-shop-1.0.0.jar org.example.Main
```

The `-ea` option enables the domain model's Java assertions. If Maven reports that release 21 is unsupported, check the JDK shown by `mvn -version` and update `JAVA_HOME` to point to JDK 21.

## Demo accounts and sample data

The following accounts are created automatically on every startup:

| Role | Email | Phone | Password |
| --- | --- | --- | --- |
| Administrator | `admin@shop.com` | `09120000000` | `Admin@123` |
| Customer | `customer@shop.com` | `09121112233` | `Pass@123` |

Use either the email address or phone number to sign in. These are demonstration credentials for the local application.

The customer account includes **Home** and **Work** delivery addresses. The sample catalog contains six products across four categories:

| Category ID | Category |
| --- | --- |
| `CAT-DIGITAL` | Digital & Electronics |
| `CAT-BOOKS` | Books & Literature |
| `CAT-FASHION` | Clothing & Apparel |
| `CAT-HOME` | Home & Office |

Product IDs are generated at startup. Copy an ID from the catalog displayed in the current run when adding an item to a cart or editing a product.

Two sample coupons are also available. All amounts below are in Iranian rials (`IRR`).

| Coupon | Discount | Maximum discount | Minimum order subtotal |
| --- | --- | --- | --- |
| `WELCOME20` | 20% | 500,000 IRR | 1,000,000 IRR |
| `AUT_SPECIAL` | 15% | 300,000 IRR | 500,000 IRR |

Both coupons receive an expiry date three months after startup.

## Example shopping workflow

1. Choose **6. Login** from the guest menu and sign in with the demo customer account.
2. Choose **1. Browse Catalog & Add to Cart**, copy a product ID, and enter a quantity.
3. Use **2. View & Manage Shopping Cart** to inspect or change the cart.
4. Choose **3. Proceed to Checkout**, select a delivery address, and optionally enter a coupon code.
5. Review the order summary. Pay immediately or leave the order pending and return to it through **4. My Order History & Tracking**.
6. For payment, select a method and choose whether the simulated authorization succeeds. A failed payment can be retried.
7. Log out and sign in as the administrator to inspect orders through option **8** and advance fulfillment through option **6**.

Customer option **6** manages the profile and address book. Customer option **7** opens the full catalog search and filtering menu.

## Order and payment behavior

Checkout records the selected items, delivery address, discount, and totals in a new `PENDING` order. Stock is checked during checkout and checked again before a successful payment deducts inventory.

| Current status | Allowed next status | Trigger |
| --- | --- | --- |
| `PENDING` | `PAID`, `PAYMENT_FAILED`, `CANCELLED` | Payment attempt or cancellation |
| `PAYMENT_FAILED` | `PAID`, `PAYMENT_FAILED`, `CANCELLED` | Payment retry or cancellation |
| `PAID` | `PROCESSING`, `CANCELLED` | Administrator starts fulfillment or the order is cancelled |
| `PROCESSING` | `SHIPPED` | Administrator dispatches the order |
| `SHIPPED` | `DELIVERED` | Administrator confirms delivery |
| `DELIVERED`, `CANCELLED` | None | Final states |

- Failed payments leave inventory unchanged and keep the cart available for another attempt.
- For orders created through checkout in the same service instance, successful payment removes only the original cart entries that have not subsequently changed. Later additions and edited entries remain in the cart.
- Cancelling a `PAID` order restores its stock. Orders cannot be cancelled once processing has begun.
- The payable amount is the items subtotal minus the coupon discount plus shipping. Shipping is currently zero.
- Money uses `BigDecimal` with two decimal places. The demo catalog and CLI price filters use `IRR`.
- The **newest** sort uses product creation time. **Popularity** counts units sold through successful payments; cancellation does not reduce that cumulative count.

## Architecture and project structure

The code is organized around domain behavior, use cases, and terminal interaction:

| Component | Location | Responsibility |
| --- | --- | --- |
| Application entry point | `src/main/java/org/example/Main.java` | Creates shared stores, wires services, seeds data, and starts the menu controller |
| Domain model | `src/main/java/org/example/model/` | Entities, immutable value objects, enums, and model invariants |
| Application services | `src/main/java/org/example/service/` | Authentication, profiles, catalog, cart, order, and review operations |
| Command-line interface | `src/main/java/org/example/cli/` | Input/output handling, role-based navigation, and guest, customer, and administrator menus |
| Application exceptions | `src/main/java/org/example/exception/` | Errors for invalid input, unavailable products, access checks, and invalid order operations |
| Utilities | `src/main/java/org/example/util/` | Sample-data initialization and password hashing |
| Tests | `src/test/java/org/example/` | Unit, service, menu, end-to-end, and regression tests |
| Documentation | `docs/` | Domain specification, diagrams, and document build files |
| Build configuration | `pom.xml` | Java version, test dependencies, and Maven plugins |

The main entities are `User`, `Product`, `Cart`, and `Order`. Java records represent values such as `Money`, `Quantity`, `Address`, `ContactInfo`, cart and order items, categories, coupons, reviews, and payment transactions.

Services receive their dependencies through constructors. `Main` supplies shared in-memory maps, allowing the CLI and tests to use the same service APIs without a database or dependency-injection framework. `CatalogMenu` shares catalog operations between guest and customer menus.

## Testing

Run the complete JUnit suite:

```bash
mvn test
```

Run the submission regression tests separately:

```bash
mvn -Dtest=SubmissionRegressionTest test
```

The suite covers model validation, monetary calculations, authentication, catalog operations, cart management, order transitions, reviews, and menu interaction. `EndToEndFlowTest` exercises shopping flows across services. `SubmissionRegressionTest` checks cases such as customer order ownership, password recovery, inactive-product validation, cart preservation after payment, address editing, and administrator order filtering.

Maven Surefire enables assertions during tests and writes reports to `target/surefire-reports/`.

## Documentation

The detailed design documents are maintained in Persian:

- [Documentation guide](docs/README.md)
- [Domain model specification — Markdown](docs/domain-model/domain-model-specification.md)
- [Domain model specification — PDF](docs/domain-model/domain-model-specification.pdf)
- [Domain model specification — LaTeX source](docs/domain-model/domain-model-specification.tex)
- [Mermaid diagram sources](docs/domain-model/assets/diagrams/)

The specification includes domain concepts, relationships, service descriptions, order states, and user-story mappings. The root-level [domain-model-specification.md](domain-model-specification.md) links to the documentation folder.

Document build scripts are under `docs/domain-model/build/`. They are separate from the Java build; Python and XeLaTeX are needed only when regenerating the typeset documentation. See the documentation guide for instructions.

## Scope and limitations

- This is a terminal application for the course project. It has no web interface or REST API.
- Accounts, sessions, carts, orders, reviews, and inventory changes exist only for the current process. There is no persistent database.
- All payment methods are simulations. No bank gateway, wallet balance, or cash-collection service is connected.
- Password recovery prints a six-digit code in the console instead of sending email or SMS. The code expires after ten minutes and is single-use; a successful reset invalidates the user's existing sessions.
- Password hashing currently uses SHA-256 as implemented in `PasswordUtil`; production authentication would require a dedicated password-hashing scheme.
- Product images are stored and displayed as HTTP/HTTPS URLs. The application does not upload or render image files.
- `STOREKEEPER` exists in the role enum, but there is no separate storekeeper menu and that role does not receive administrator access.

## Course information

- **Course:** Principles of Software Design
- **University:** Amirkabir University of Technology
- **Term:** Fall 2026
- **Instructor:** Dr. Hanieh Fallah
- **Head teaching assistant:** Amir Mohammad Zaker
