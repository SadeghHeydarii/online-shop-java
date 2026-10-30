package org.example.cli.menu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.cli.ConsoleIO;
import org.example.model.entity.Category;
import org.example.model.entity.Order;
import org.example.model.entity.Product;
import org.example.model.entity.User;
import org.example.model.enums.OrderStatus;
import org.example.model.enums.UserRole;
import org.example.model.vo.Address;
import org.example.model.vo.Money;
import org.example.model.vo.OrderItem;
import org.example.model.vo.Quantity;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminMenu Tests")
class AdminMenuTest {

    private Map<String, Product> productStore;
    private Map<String, Category> categoryStore;
    private Map<String, Order> orderStore;

    private ProductService productService;
    private OrderService orderService;

    private User admin;
    private Product existingProduct;
    private Category hardwareCategory;

    @BeforeEach
    void setUp() {
        productStore = new ConcurrentHashMap<>();
        categoryStore = new ConcurrentHashMap<>();
        orderStore = new ConcurrentHashMap<>();

        productService = new ProductService(productStore, categoryStore);
        orderService = new OrderService(orderStore);

        // Seed Category & Product
        hardwareCategory = new Category("CAT-HW", "Hardware", "Computer hardware gear");
        categoryStore.put(hardwareCategory.id(), hardwareCategory);

        Money initialPrice = new Money(new BigDecimal("5000000.00"), "IRR");
        productService.addProduct("Monitor", "27 inch 4K display", initialPrice, 10, "CAT-HW");
        existingProduct = productService.searchProducts("Monitor").getFirst();

        admin = new User("adm-1", "Saeed Manager", "saeed@aut.ac.ir", "09121234567", "hashPass", UserRole.ADMIN, LocalDateTime.now());
    }

    private ConsoleIO createConsoleIO(String input, ByteArrayOutputStream out) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        return new ConsoleIO(in, ps);
    }

    @Nested
    @DisplayName("Catalog Maintenance Tests")
    class CatalogMaintenanceTests {

        @Test
        @DisplayName("handleViewAllProducts displays all active products in table")
        void viewAllProductsSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

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
            // Name -> Desc -> Price -> Stock -> Category ID
            String script = "Keyboard\nMechanical RGB keyboard\n1500000\n25\nCAT-HW\n";
            ConsoleIO io = createConsoleIO(script, out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

            menu.handleAddProduct();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Product 'Keyboard' successfully created and added to catalog!"));

            List<Product> matches = productService.searchProducts("Keyboard");
            assertEquals(1, matches.size());
            assertEquals(25, matches.getFirst().getAvailableStock());
        }

        @Test
        @DisplayName("handleEditProduct updates specifications and price (US 34)")
        void editProductSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Product ID -> New Name -> New Desc -> New Price -> New CatId
            String script = existingProduct.getId() + "\nUltraWide Monitor\nCurved 34 inch\n8000000\nCAT-HW\n";
            ConsoleIO io = createConsoleIO(script, out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

            menu.handleEditProduct();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("updated successfully."));

            Product updated = productService.getProductById(existingProduct.getId());
            assertEquals("UltraWide Monitor", updated.getName());
            assertEquals("Curved 34 inch", updated.getDescription());
            assertEquals(new BigDecimal("8000000.00"), updated.getPrice().amount());
        }

        @Test
        @DisplayName("handleDeactivateProduct deactivates active item when confirmed (US 35)")
        void deactivateProductSuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Product ID -> y (confirm)
            String script = existingProduct.getId() + "\ny\n";
            ConsoleIO io = createConsoleIO(script, out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

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
            // Product ID -> 1 (Restock) -> 15 (Add qty)
            String script = existingProduct.getId() + "\n1\n15\n";
            ConsoleIO io = createConsoleIO(script, out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

            menu.handleManageStock();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Stock adjusted successfully!"));
            assertEquals(25, existingProduct.getAvailableStock()); // 10 + 15
        }

        @Test
        @DisplayName("handleManageStock deducts inventory quantity (US 36)")
        void deductInventorySuccess() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Product ID -> 2 (Deduct) -> 4 (Deduct qty)
            String script = existingProduct.getId() + "\n2\n4\n";
            ConsoleIO io = createConsoleIO(script, out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

            menu.handleManageStock();

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Stock adjusted successfully!"));
            assertEquals(6, existingProduct.getAvailableStock()); // 10 - 4
        }

        @Test
        @DisplayName("handleProcessOrders advances order status through lifecycle (US 29)")
        void processOrderTransitionsSuccess() {
            // Seed a PAID order
            Address addr = new Address("a1", "Customer", "09121112233", "Tehran", "Tehran", "St", "1234567890", "Home", true);
            OrderItem item = new OrderItem(existingProduct.getId(), existingProduct.getName(), existingProduct.getPrice(), new Quantity(1), existingProduct.getPrice());
            Order order = new Order("ord-admin-1", "usr-1", List.of(item), addr,
                    existingProduct.getPrice(), Money.ZERO, Money.ZERO, existingProduct.getPrice(), LocalDateTime.now());
            order.markAsPaid(LocalDateTime.now());
            orderStore.put(order.getOrderId(), order);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Order ID -> 1 (PROCESSING)
            String script = "ord-admin-1\n1\n";
            ConsoleIO io = createConsoleIO(script, out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

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
            ConsoleIO io = createConsoleIO("0\n", out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

            menu.showMenu(admin);

            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Logging out of administrative session. Goodbye!"));
        }

        @Test
        @DisplayName("showMenu handles EOF gracefully")
        void eofExitsGracefully() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ConsoleIO io = createConsoleIO("", out);
            AdminMenu menu = new AdminMenu(productService, orderService, io);

            menu.showMenu(admin);
            // Finished without throwing unhandled exception
        }
    }
}
