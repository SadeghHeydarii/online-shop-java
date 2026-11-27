package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.example.cli.ConsoleIO;
import org.example.cli.menu.admin.AdminCatalogHandler;
import org.example.cli.menu.admin.AdminOrderHandler;
import org.example.cli.view.ProductTablePrinter;
import org.example.infrastructure.catalog.DefaultNameSort;
import org.example.infrastructure.catalog.MapProductSortRegistry;
import org.example.infrastructure.persistence.InMemoryCategoryRepository;
import org.example.infrastructure.persistence.InMemoryCheckoutSnapshotRepository;
import org.example.infrastructure.persistence.InMemoryOrderRepository;
import org.example.infrastructure.persistence.InMemoryProductRepository;
import org.example.infrastructure.security.UuidIdGenerator;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Category;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.CatalogManagementService;
import org.example.service.CatalogQueryService;
import org.example.service.OrderFulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminMenu Tests")
class AdminMenuTest {

    private InMemoryOrderRepository orderRepository;
    private CatalogQueryService catalogQueryService;
    private CatalogManagementService catalogManagementService;
    private OrderFulfillmentService orderFulfillmentService;

    private User admin;
    private Product existingProduct;
    private Category hardwareCategory;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        var idGenerator = new UuidIdGenerator();
        var productRepository = new InMemoryProductRepository();
        var categoryRepository = new InMemoryCategoryRepository();
        orderRepository = new InMemoryOrderRepository();
        var snapshotRepository = new InMemoryCheckoutSnapshotRepository();

        catalogQueryService = new CatalogQueryService(
                productRepository, categoryRepository, new MapProductSortRegistry(new DefaultNameSort()));
        catalogManagementService = new CatalogManagementService(
                productRepository, categoryRepository, idGenerator, clock);
        orderFulfillmentService = new OrderFulfillmentService(
                orderRepository, snapshotRepository, catalogManagementService);

        hardwareCategory = new Category("CAT-HW", "Hardware", "Computer hardware gear");
        categoryRepository.save(hardwareCategory);

        Money initialPrice = new Money(new BigDecimal("5000000.00"), "IRR");
        catalogManagementService.addProduct("Monitor", "27 inch 4K display", initialPrice, 10, "CAT-HW");
        existingProduct = catalogQueryService.searchProducts("Monitor").getFirst();

        admin = new User("adm-1", "Saeed Manager", "saeed@aut.ac.ir", "09121234567", "hashPass", UserRole.ADMIN, LocalDateTime.now());
    }

    private ConsoleIO createConsoleIO(String input, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, ps);
    }

    private AdminMenu createAdminMenu(ConsoleIO io) {
        AdminCatalogHandler catalogHandler = new AdminCatalogHandler(
                catalogManagementService, catalogQueryService, new ProductTablePrinter(io), io, io);
        AdminOrderHandler orderHandler = new AdminOrderHandler(orderFulfillmentService, io, io);
        return new AdminMenu(catalogHandler, orderHandler, io, io);
    }

    @Nested
    @DisplayName("Catalog Maintenance Tests")
    class CatalogMaintenanceTests {

        @Test
        @DisplayName("handleViewAllProducts displays all active products in table")
        void viewAllProductsSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO("", out));

            menu.handleViewAllProducts();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Catalog Products List"));
            assertTrue(output.contains("Monitor"));
            assertTrue(output.contains("5000000.00 IRR"));
        }

        @Test
        @DisplayName("handleAddProduct creates new product in catalog (US 33)")
        void addProductSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String script = "Keyboard\nMechanical RGB keyboard\n1500000\n25\nCAT-HW\n";
            AdminMenu menu = createAdminMenu(createConsoleIO(script, out));

            menu.handleAddProduct();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Product 'Keyboard' successfully created and added to catalog!"));

            List<Product> matches = catalogQueryService.searchProducts("Keyboard");
            assertEquals(1, matches.size());
            assertEquals(25, matches.getFirst().getAvailableStock());
        }

        @Test
        @DisplayName("handleEditProduct updates specifications and price (US 34)")
        void editProductSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String script = existingProduct.getId() + "\nUltraWide Monitor\nCurved 34 inch\n8000000\nCAT-HW\n";
            AdminMenu menu = createAdminMenu(createConsoleIO(script, out));

            menu.handleEditProduct();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("updated successfully."));

            Product updated = catalogQueryService.getProductById(existingProduct.getId());
            assertEquals("UltraWide Monitor", updated.getName());
            assertEquals("Curved 34 inch", updated.getDescription());
            assertEquals(new BigDecimal("8000000.00"), updated.getPrice().amount());
        }

        @Test
        @DisplayName("handleDeactivateProduct deactivates active item when confirmed (US 35)")
        void deactivateProductSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO(existingProduct.getId() + "\ny\n", out));

            menu.handleDeactivateProduct();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("has been deactivated."));
            assertFalse(existingProduct.isActive());
        }
    }

    @Nested
    @DisplayName("Inventory & Order Fulfillment Tests")
    class InventoryAndFulfillmentTests {

        @Test
        @DisplayName("handleManageStock restocks item inventory (US 36)")
        void restockInventorySuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO(existingProduct.getId() + "\n1\n15\n", out));

            menu.handleManageStock();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Stock adjusted successfully!"));
            assertEquals(25, existingProduct.getAvailableStock());
        }

        @Test
        @DisplayName("handleManageStock deducts inventory quantity (US 36)")
        void deductInventorySuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO(existingProduct.getId() + "\n2\n4\n", out));

            menu.handleManageStock();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Stock adjusted successfully!"));
            assertEquals(6, existingProduct.getAvailableStock());
        }

        @Test
        @DisplayName("handleProcessOrders advances order status through lifecycle (US 29)")
        void processOrderTransitionsSuccess() {
            Address addr = new Address("a1", "Customer", "09121112233", "Tehran", "Tehran", "St", "1234567890", "Home", true);
            OrderItem item = new OrderItem(existingProduct.getId(), existingProduct.getName(), existingProduct.getPrice(), new Quantity(1), existingProduct.getPrice());
            Order order = new Order("ord-admin-1", "usr-1", List.of(item), addr,
                    existingProduct.getPrice(), Money.ZERO, Money.ZERO, existingProduct.getPrice(), LocalDateTime.now());
            order.markAsPaid(LocalDateTime.now());
            orderRepository.save(order);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO("ord-admin-1\n1\n", out));

            menu.handleProcessOrders();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("is now in PROCESSING state."));
            assertEquals(OrderStatus.PROCESSING, order.getStatus());
        }
    }

    @Nested
    @DisplayName("Menu Navigation Tests")
    class MenuNavigationTests {

        @Test
        @DisplayName("showMenu option 0 exits loop and logs out")
        void exitLogsOut() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO("0\n", out));

            menu.showMenu(admin);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Logging out of administrative session. Goodbye!"));
        }

        @Test
        @DisplayName("showMenu handles EOF gracefully")
        void eofExitsGracefully() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdminMenu menu = createAdminMenu(createConsoleIO("", out));

            menu.showMenu(admin);
        }
    }
}
