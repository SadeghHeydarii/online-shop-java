# SOLID Refactoring Summary & UML Handoff Guide

This document summarizes the Phase 2 **SOLID refactoring** applied to `online-shop-java` and maps the final package structure, abstractions, and wiring rules so the UML diagrams (`Class`, `Object`, `Activity`, `Component`, `Deployment`, `Sequence`) and Design Report stay 100% consistent with the final Java 21 codebase.

---

## 1. Architectural Shift & Acyclic Package Graph

Previously, domain workflows were grouped into four monolithic facades (`AuthService`, `UserService`, `ProductService`, `OrderService`) wired directly to raw `ConcurrentHashMap` stores and concrete terminal I/O (`ConsoleIO`), while `Main`, CLI menus, and static utilities coupled directly to concrete classes and system clocks.

The refactored architecture separates **domain core**, **use-case boundaries (`service.api`)**, **outbound infrastructure ports (`service.port`)**, **cohesive application services (`service`)**, **interchangeable adapters (`infrastructure`)**, **decomposed CLI coordinators/handlers (`cli`)**, and the **sole Composition Root (`Main`)**:

```text
org.example.Main  (Composition Root)
  ├──► org.example.cli.*  ──► service.api.*, cli.io.*, cli.view.*, service.port.IdGenerator
  ├──► org.example.infrastructure.*  ──► service.port.*, CatalogAdminUseCase, util.PasswordUtil
  └──► org.example.service.*  ──► service.api.*, service.port.*
                                      │
                                      ▼
                             org.example.model.*  (zero outward dependencies)
                             org.example.exception.* (ShopException hierarchy; invalid input inherits IllegalArgumentException)
```

### Verified Package Dependency Invariants
- **Acyclic Packages:** Zero dependency cycles across `model`, `exception`, `util`, `service.api`, `service.port`, `service`, `infrastructure`, `cli`, and `Main`.
- **`org.example.model` Purity:** Zero imports from `service`, `infrastructure`, `cli`, or `Main`; domain entities (`User`, `Product`, `Cart`, `Order`) preserve `RI`, `AF`, and `checkRep()` invariants (`User.matchesContact()` encapsulates contact lookup without leaking `ContactInfo` duplication).
- **DIP Compliance in `service` & `cli`:** `org.example.service` has zero imports from `infrastructure`, `cli`, or `Main`. `org.example.cli` has zero imports from `infrastructure` or concrete `*Service` implementations, and only `Main` instantiates services, repositories, registries, `ApplicationDataSeeder`, or `ConsoleIO`.

---

## 2. SOLID Principles Applied

### Single Responsibility Principle (SRP)
Separating responsibilities by actor and reason to change (`week05-solid.pdf` §2.3):
- **Order Domain (`OrderService` removed):** Split into `OrderPricingService` (totals/discount/shipping), `OrderCheckoutService` (cart snapshot creation & personal order queries/cancellation), `OrderPaymentService` (gateway authorization, stock deduction, paid snapshot cleanup), `OrderFulfillmentService` (admin lifecycle transitions & store-wide filtering), and package-private `OrderLifecycleSupport`.
- **Catalog Domain (`ProductService` removed):** Split into `CatalogQueryService` (read-only storefront browsing, search, dynamic filter/sort, availability queries), `CatalogManagementService` (admin product CRUD, activation, stock mutations, categories), and package-private `CatalogLookupSupport`.
- **User & Identity Domain (`AuthService`, `UserService`, `util.DataSeeder` removed):** Split into `AuthenticationService` (registration, login, session tokens), `PasswordRecoveryService` (10-minute code issuance, validation, session revocation), `PasswordPolicySupport` (package-private shared password/validation helpers), `UserProfileService` (profile updates & owned address book CRUD), and `ApplicationDataSeeder` in `infrastructure.seed`.
- **CLI Layer (`cli`):** Extracted `ProductTablePrinter` (DRY tabular product display + Unicode `\u2605` fix), decomposed `CustomerMenu` into five handlers (`CartMenuHandler`, `CheckoutMenuHandler`, `OrderHistoryMenuHandler`, `ReviewMenuHandler`, `ProfileMenuHandler`), decomposed `AdminMenu` into two handlers (`AdminCatalogHandler`, `AdminOrderHandler`), and moved active session ownership out of `GuestMenu` into `MainMenuController`.

### Open-Closed Principle (OCP)
Replacing closed `switch` / `if-else` dispatch chains with polymorphic registries (`week05-solid.pdf` §3):
- **Payment Methods:** `PaymentGateway` interface + `MapPaymentGatewayRegistry` resolve `OnlinePaymentGateway`, `WalletPaymentGateway`, and `CashOnDeliveryPaymentGateway` by `PaymentMethod`. Adding a new gateway requires zero changes to `OrderPaymentService`.
- **Pricing Rules:** `DiscountPolicy` (`CouponDiscountPolicy`) and `ShippingPolicy` (`FreeShippingPolicy`) plug into `OrderPricingService`.
- **Catalog Sorting:** `ProductSortStrategy` + `MapProductSortRegistry` register `DefaultNameSort`, `PriceAscendingSort`, `PriceDescendingSort`, `RatingSort`, `NewestSort`, and `PopularSort`. `CatalogQueryUseCase.getAvailableSortOptions()` exposes registered keys dynamically to `CatalogMenu`, eliminating the CLI sort `switch`.
- **Role Menus:** `AuthenticatedMenu` (`supportedRole()`, `showMenu(User)`) + `Map<UserRole, AuthenticatedMenu>` in `MainMenuController` replace hardcoded `if (role == CUSTOMER) ... else if (role == ADMIN)` checks.

### Liskov Substitution Principle (LSP)
Shared behavioral contracts verified via abstract/parameterized JUnit LSP contract suites (`week05-solid.pdf` §4):
- `UserRepository`, `OrderRepository`, `CartRepository` (`UserRepositoryContractTest`, `OrderRepositoryContractTest`, `CartRepositoryContractTest`)
- `PasswordHasher` (`PasswordHasherContractTest`)
- `PaymentGateway` (`PaymentGatewayContractTest`)
- `DiscountPolicy` & `ShippingPolicy` (`DiscountPolicyContractTest`, `ShippingPolicyContractTest`)
- `ProductSortStrategy` (`ProductSortStrategyContractTest`)
- `AuthenticatedMenu` (`AuthenticatedMenuContractTest` — enforces non-null `supportedRole()`, `NullPointerException` for null users, and non-throwing rejection for mismatched roles across `CustomerMenu` and `AdminMenu`).

### Interface Segregation Principle (ISP)
Narrow client-focused ports eliminating fat-interface coupling (`week05-solid.pdf` §5):
- **Outbound (`service.port`):** 11 focused repositories (`UserRepository`, `CredentialRepository`, `SessionRepository`, `PasswordResetRepository`, `ProductRepository`, `CategoryRepository`, `CartRepository`, `OrderRepository`, `CouponRepository`, `ReviewRepository`, `CheckoutSnapshotRepository`), security/ID/code ports (`PasswordHasher`, `IdGenerator`, `VerificationCodeGenerator`), and strategy/gateway ports (`PaymentResult`, `PaymentGateway`, `PaymentGatewayRegistry`, `DiscountPolicy`, `ShippingPolicy`, `ProductSortStrategy`, `ProductSortRegistry`).
- **Inbound & Inter-Service (`service.api`):** 17 focused interfaces separating caller needs (`AuthUseCase`, `PasswordRecoveryUseCase`, `UserProfileUseCase`, `AddressBookUseCase`, `CatalogQueryUseCase`, `CatalogAdminUseCase`, `CartUseCase`, `CustomerOrderUseCase`, `PaymentUseCase`, `OrderFulfillmentUseCase`, `ReviewUseCase`, plus narrow internal adapters `UserLookupPort`, `CartCheckoutPort`, `ProductAvailabilityPort`, `ProductInventoryPort`, `ProductLookupPort`, `PurchaseHistoryQuery`).
- **CLI I/O (`cli.io`):** Split `ConsoleIO` into `InputReader`, `OutputWriter`, and combined `TerminalIO`, allowing output-only components like `ProductTablePrinter` and `MainMenuController` to depend strictly on `OutputWriter`.

### Dependency Inversion Principle (DIP)
High-level policies define and depend on abstractions; low-level details plug inward (`week05-solid.pdf` §6):
- Every application service injects `Clock`, `IdGenerator`, repositories, and strategies rather than constructing `ConcurrentHashMap`, `UUID.randomUUID()`, `LocalDateTime.now()`, or static helpers directly.
- `Main.java` serves as the single Composition Root wiring concrete adapters into abstract service ports and CLI menus.
- Uniform domain exception hierarchy `ShopException` enables bounded CLI fault handling (`catch (ShopException | IllegalArgumentException)`) instead of catching broad `RuntimeException`.

---

## 3. Final Package Inventory

| Package | Role | Key Types |
| :--- | :--- | :--- |
| `org.example` | Composition Root | `Main` |
| `org.example.model.{entity,enums,vo}` | Core domain model | `User`, `Product`, `Cart`, `Order`; `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `UserRole`; `Money`, `Quantity`, `Address`, `ContactInfo`, `CartItem`, `Category`, `Coupon`, `OrderItem`, `PaymentTransaction`, `Review` |
| `org.example.exception` | Domain error hierarchy | `ShopException` base + 10 checked-hierarchy runtime domain exceptions; `InvalidInputException` (`IllegalArgumentException`) |
| `org.example.service.api` | Inbound use-case & internal service ports (17) | `AuthUseCase`, `PasswordRecoveryUseCase`, `UserProfileUseCase`, `AddressBookUseCase`, `CatalogQueryUseCase`, `CatalogAdminUseCase`, `CartUseCase`, `CustomerOrderUseCase`, `PaymentUseCase`, `OrderFulfillmentUseCase`, `ReviewUseCase`, `UserLookupPort`, `CartCheckoutPort`, `ProductAvailabilityPort`, `ProductInventoryPort`, `ProductLookupPort`, `PurchaseHistoryQuery` |
| `org.example.service.port` | Outbound ports, repositories & strategy contracts (19) | 11 `*Repository` interfaces, `IdGenerator`, `PasswordHasher`, `VerificationCodeGenerator`, `DiscountPolicy`, `ShippingPolicy`, `PaymentGateway`, `PaymentGatewayRegistry`, `PaymentResult`, `ProductSortStrategy`, `ProductSortRegistry` |
| `org.example.service` | Cohesive SRP services & package support (14) | `AuthenticationService`, `PasswordRecoveryService`, `UserProfileService`, `CatalogQueryService`, `CatalogManagementService`, `CartService`, `OrderPricingService`, `OrderCheckoutService`, `OrderPaymentService`, `OrderFulfillmentService`, `ReviewService`, plus package-private `OrderLifecycleSupport`, `CatalogLookupSupport`, `PasswordPolicySupport` |
| `org.example.infrastructure.*` | Adapters & sample seeding | `persistence` (11 `InMemory*Repository` + `RepositoryPreconditions`), `payment` (3 gateways + `AbstractSimulatedPaymentGateway` + `MapPaymentGatewayRegistry`), `pricing` (`CouponDiscountPolicy`, `FreeShippingPolicy`), `catalog` (6 sorts + `MapProductSortRegistry`), `security` (`Sha256PasswordHasher`, `UuidIdGenerator`, `RandomVerificationCodeGenerator`), `seed` (`ApplicationDataSeeder`) |
| `org.example.cli.*` | Segregated CLI views, handlers & controllers | `io` (`InputReader`, `OutputWriter`, `TerminalIO`), `view` (`ProductTablePrinter`), `menu` (`AuthenticatedSession`, `UnauthenticatedMenu`, `AuthenticatedMenu`, `CatalogMenu`, `GuestMenu`, `CustomerMenu`, `AdminMenu`), `menu.customer` (5 handlers), `menu.admin` (2 handlers), `ConsoleIO`, `MainMenuController` |
| `org.example.util` | Shared password hashing helper | `PasswordUtil` |

---

## 4. Guidance for UML Diagrams & Design Report

1. **Class Diagram:** Organize packages cleanly into Presentation (`cli.*`), Application Ports (`service.api`, `service.port`), Application Services (`service`), Infrastructure (`infrastructure.*`), Domain (`model.*`), Exceptions (`exception`), and Composition (`Main`). Show interface realizations (`implements`) across ports and adapters rather than direct dependencies on `InMemory*` or concrete `*Service` classes.
2. **Component Diagram:** Represent high-level decoupled components interacting through provided/required lollipop interfaces (`AuthUseCase`, `CatalogQueryUseCase`, `CustomerOrderUseCase`, `PaymentUseCase`, `OrderRepository`, `PaymentGatewayRegistry`, `ProductSortRegistry`).
3. **Sequence Diagrams:** For **Checkout & Payment**, trace `CheckoutMenuHandler` -> `CustomerOrderUseCase` (`OrderCheckoutService`) -> `OrderPricingService` (`DiscountPolicy` + `ShippingPolicy`) -> `OrderRepository`/`CheckoutSnapshotRepository`, followed by `PaymentUseCase` (`OrderPaymentService`) -> `PaymentGatewayRegistry` -> `PaymentGateway` -> `ProductInventoryPort` -> `CartCheckoutPort`.
4. **Object & Deployment Diagrams:** Capture runtime instances created inside `Main.main(...)` (in-memory repositories, `MapProductSortRegistry`, `MapPaymentGatewayRegistry`, `ApplicationDataSeeder`) running inside a single Java 21 JVM process.
