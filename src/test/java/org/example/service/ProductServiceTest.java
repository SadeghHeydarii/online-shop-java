package org.example.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.exception.CategoryNotFoundException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.vo.Category;
import org.example.model.entity.Product;
import org.example.model.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductService Tests")
class ProductServiceTest {

    private ProductService productService;
    private Category electronics;
    private Category books;

    @BeforeEach
    void setUp() {
        productService = new ProductService();
        electronics = new Category("CAT-ELEC", "Electronics", "Electronic devices");
        books = new Category("CAT-BOOK", "Books", "Printed books");
        productService.registerCategory(electronics);
        productService.registerCategory(books);
    }

    @Nested
    @DisplayName("addProduct Tests")
    class AddProductTests {

        @Test
        @DisplayName("Valid product addition should register product into catalog")
        void addProductSuccess() {
            Money price = new Money(BigDecimal.valueOf(150000), "IRR");
            productService.addProduct("Mechanical Keyboard", "RGB Gaming Keyboard", price, 20, "CAT-ELEC");

            List<Product> products = productService.getAllActiveProducts();
            assertEquals(1, products.size());

            Product added = products.getFirst();
            assertTrue(added.getId().startsWith("prod-"));
            assertEquals("Mechanical Keyboard", added.getName());
            assertEquals("RGB Gaming Keyboard", added.getDescription());
            assertEquals(price, added.getPrice());
            assertEquals(20, added.getAvailableStock());
            assertEquals("CAT-ELEC", added.getCategoryId());
            assertTrue(added.isActive());
        }

        @Test
        @DisplayName("Adding product with non-existent category throws CategoryNotFoundException")
        void addProductNonExistentCategoryThrows() {
            Money price = new Money(BigDecimal.valueOf(50000), "IRR");
            assertThrows(CategoryNotFoundException.class, () ->
                    productService.addProduct("Mystery Item", "Unknown", price, 10, "CAT-UNKNOWN"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null product name throws InvalidInputException")
        void blankNameThrows(String name) {
            Money price = new Money(BigDecimal.valueOf(50000), "IRR");
            assertThrows(InvalidInputException.class, () ->
                    productService.addProduct(name, "Valid Description", price, 10, "CAT-ELEC"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null description throws InvalidInputException")
        void blankDescThrows(String desc) {
            Money price = new Money(BigDecimal.valueOf(50000), "IRR");
            assertThrows(InvalidInputException.class, () ->
                    productService.addProduct("Valid Name", desc, price, 10, "CAT-ELEC"));
        }

        @Test
        @DisplayName("Null price throws InvalidInputException")
        void nullPriceThrows() {
            assertThrows(InvalidInputException.class, () ->
                    productService.addProduct("Valid Name", "Desc", null, 10, "CAT-ELEC"));
        }

        @Test
        @DisplayName("Negative stock throws InvalidInputException")
        void negativeStockThrows() {
            Money price = new Money(BigDecimal.valueOf(50000), "IRR");
            assertThrows(InvalidInputException.class, () ->
                    productService.addProduct("Valid Name", "Desc", price, -5, "CAT-ELEC"));
        }
    }

    @Nested
    @DisplayName("getProductById Tests")
    class GetProductByIdTests {

        @Test
        @DisplayName("Existing product can be fetched by ID")
        void getProductByIdSuccess() {
            Money price = new Money(BigDecimal.valueOf(100000), "IRR");
            productService.addProduct("Laptop Stand", "Aluminum Stand", price, 15, "CAT-ELEC");

            Product created = productService.getAllActiveProducts().getFirst();
            Product fetched = productService.getProductById(created.getId());

            assertEquals(created.getId(), fetched.getId());
            assertEquals("Laptop Stand", fetched.getName());
        }

        @Test
        @DisplayName("Non-existent product ID throws ProductNotFoundException")
        void nonExistentIdThrows() {
            assertThrows(ProductNotFoundException.class, () -> productService.getProductById("non-existent-id"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank or null ID throws InvalidInputException")
        void invalidIdThrows(String id) {
            assertThrows(InvalidInputException.class, () -> productService.getProductById(id));
        }
    }

    @Nested
    @DisplayName("searchProducts Tests")
    class SearchProductsTests {

        @BeforeEach
        void seedCatalog() {
            productService.addProduct("Clean Code", "A handbook of agile software craftsmanship",
                    new Money(BigDecimal.valueOf(80000), "IRR"), 10, "CAT-BOOK");
            productService.addProduct("Clean Architecture", "A craftsman guide to software structure",
                    new Money(BigDecimal.valueOf(95000), "IRR"), 8, "CAT-BOOK");
            productService.addProduct("USB-C Hub", "Multiport USB-C adapter",
                    new Money(BigDecimal.valueOf(120000), "IRR"), 25, "CAT-ELEC");
        }

        @Test
        @DisplayName("Search matching name case-insensitively")
        void searchByName() {
            List<Product> results = productService.searchProducts("clean");
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Search matching description case-insensitively")
        void searchByDescription() {
            List<Product> results = productService.searchProducts("craftsmanship");
            assertEquals(1, results.size());
            assertEquals("Clean Code", results.getFirst().getName());
        }

        @Test
        @DisplayName("Blank query returns all active products")
        void blankQueryReturnsAll() {
            List<Product> results = productService.searchProducts("  ");
            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("Null query throws InvalidInputException")
        void nullQueryThrows() {
            assertThrows(InvalidInputException.class, () -> productService.searchProducts(null));
        }

        @Test
        @DisplayName("Deactivated product is omitted from search results")
        void deactivatedExcludedFromSearch() {
            Product book = productService.searchProducts("Clean Code").getFirst();
            productService.deactivateProduct(book.getId());

            List<Product> results = productService.searchProducts("clean");
            assertEquals(1, results.size());
            assertEquals("Clean Architecture", results.getFirst().getName());
        }
    }

    @Nested
    @DisplayName("filterAndSort Tests")
    class FilterAndSortTests {

        private Product phone;
        private Product monitor;
        private Product javaBook;

        @BeforeEach
        void seedProducts() {
            productService.addProduct("Smartphone", "Flagship phone",
                    new Money(BigDecimal.valueOf(500000), "IRR"), 5, "CAT-ELEC");
            productService.addProduct("4K Monitor", "IPS Ultra HD Display",
                    new Money(BigDecimal.valueOf(800000), "IRR"), 10, "CAT-ELEC");
            productService.addProduct("Effective Java", "Best practices for Java platform",
                    new Money(BigDecimal.valueOf(120000), "IRR"), 30, "CAT-BOOK");

            phone = productService.searchProducts("Smartphone").getFirst();
            monitor = productService.searchProducts("4K Monitor").getFirst();
            javaBook = productService.searchProducts("Effective Java").getFirst();

            phone.updateRatingStats(5);
            phone.updateRatingStats(4); // avg = 4.5
            monitor.updateRatingStats(3); // avg = 3.0
            javaBook.updateRatingStats(5); // avg = 5.0
        }

        @Test
        @DisplayName("Filter by category alone")
        void filterByCategory() {
            List<Product> results = productService.filterAndSort("CAT-BOOK", null, null, null);
            assertEquals(1, results.size());
            assertEquals("Effective Java", results.getFirst().getName());
        }

        @Test
        @DisplayName("Category ALL or blank ignores category filtering")
        void filterAllCategory() {
            List<Product> results = productService.filterAndSort("ALL", null, null, null);
            assertEquals(3, results.size());

            List<Product> resultsBlank = productService.filterAndSort("   ", null, null, null);
            assertEquals(3, resultsBlank.size());
        }

        @Test
        @DisplayName("Filter by price range")
        void filterByPriceRange() {
            Money min = new Money(BigDecimal.valueOf(200000), "IRR");
            Money max = new Money(BigDecimal.valueOf(600000), "IRR");
            List<Product> results = productService.filterAndSort(null, min, max, null);

            assertEquals(1, results.size());
            assertEquals("Smartphone", results.getFirst().getName());
        }

        @Test
        @DisplayName("minPrice exceeding maxPrice throws InvalidInputException")
        void minPriceExceedsMaxPriceThrows() {
            Money min = new Money(BigDecimal.valueOf(700000), "IRR");
            Money max = new Money(BigDecimal.valueOf(500000), "IRR");
            assertThrows(InvalidInputException.class, () ->
                    productService.filterAndSort(null, min, max, null));
        }

        @Test
        @DisplayName("Sort by price ascending")
        void sortByPriceAsc() {
            List<Product> results = productService.filterAndSort(null, null, null, "price_asc");
            assertEquals(3, results.size());
            assertEquals("Effective Java", results.get(0).getName()); // 120,000
            assertEquals("Smartphone", results.get(1).getName());    // 500,000
            assertEquals("4K Monitor", results.get(2).getName());    // 800,000
        }

        @Test
        @DisplayName("Sort by price descending")
        void sortByPriceDesc() {
            List<Product> results = productService.filterAndSort(null, null, null, "price_desc");
            assertEquals(3, results.size());
            assertEquals("4K Monitor", results.get(0).getName());    // 800,000
            assertEquals("Smartphone", results.get(1).getName());    // 500,000
            assertEquals("Effective Java", results.get(2).getName()); // 120,000
        }

        @Test
        @DisplayName("Sort by rating descending (highest rated first)")
        void sortByRating() {
            List<Product> results = productService.filterAndSort(null, null, null, "rating");
            assertEquals(3, results.size());
            assertEquals("Effective Java", results.get(0).getName()); // avg 5.0
            assertEquals("Smartphone", results.get(1).getName());    // avg 4.5
            assertEquals("4K Monitor", results.get(2).getName());    // avg 3.0
        }

        @Test
        @DisplayName("Invalid sort option throws InvalidInputException")
        void invalidSortOptionThrows() {
            assertThrows(InvalidInputException.class, () ->
                    productService.filterAndSort(null, null, null, "random_sort"));
        }
    }

    @Nested
    @DisplayName("updateProduct Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Update details modifies product fields correctly")
        void updateDetailsSuccess() {
            productService.addProduct("Wireless Mouse", "Standard mouse",
                    new Money(BigDecimal.valueOf(40000), "IRR"), 10, "CAT-ELEC");
            Product original = productService.getAllActiveProducts().getFirst();

            Money newPrice = new Money(BigDecimal.valueOf(55000), "IRR");
            productService.updateProduct(original.getId(), "Ergonomic Mouse", "Vertical mouse", newPrice, "CAT-ELEC");

            Product updated = productService.getProductById(original.getId());
            assertEquals("Ergonomic Mouse", updated.getName());
            assertEquals("Vertical mouse", updated.getDescription());
            assertEquals(newPrice, updated.getPrice());
        }

        @Test
        @DisplayName("Updating with invalid category throws CategoryNotFoundException")
        void updateInvalidCategoryThrows() {
            productService.addProduct("Mouse", "Basic",
                    new Money(BigDecimal.valueOf(30000), "IRR"), 5, "CAT-ELEC");
            Product original = productService.getAllActiveProducts().getFirst();

            Money price = new Money(BigDecimal.valueOf(30000), "IRR");
            assertThrows(CategoryNotFoundException.class, () ->
                    productService.updateProduct(original.getId(), "Mouse", "Basic", price, "NON_EXISTENT_CAT"));
        }
    }

    @Nested
    @DisplayName("deactivateProduct Tests")
    class DeactivateProductTests {

        @Test
        @DisplayName("Deactivating a product marks active as false and removes from active views")
        void deactivateProductSuccess() {
            productService.addProduct("Mouse Pad", "Cloth pad",
                    new Money(BigDecimal.valueOf(25000), "IRR"), 50, "CAT-ELEC");
            Product product = productService.getAllActiveProducts().getFirst();

            productService.deactivateProduct(product.getId());

            assertFalse(product.isActive());
            assertTrue(productService.getAllActiveProducts().isEmpty());
            assertEquals(product.getId(), productService.getProductById(product.getId()).getId());
        }
    }

    @Nested
    @DisplayName("adjustStock Tests")
    class AdjustStockTests {

        private Product item;

        @BeforeEach
        void createItem() {
            productService.addProduct("Power Bank", "10000mAh",
                    new Money(BigDecimal.valueOf(90000), "IRR"), 20, "CAT-ELEC");
            item = productService.getAllActiveProducts().getFirst();
        }

        @Test
        @DisplayName("Positive delta restocks inventory")
        void restockPositiveDelta() {
            productService.adjustStock(item.getId(), 15);
            assertEquals(35, item.getAvailableStock());
        }

        @Test
        @DisplayName("Negative delta deducts inventory")
        void deductNegativeDelta() {
            productService.adjustStock(item.getId(), -8);
            assertEquals(12, item.getAvailableStock());
        }

        @Test
        @DisplayName("Zero delta does not change inventory")
        void zeroDeltaNoOp() {
            productService.adjustStock(item.getId(), 0);
            assertEquals(20, item.getAvailableStock());
        }

        @Test
        @DisplayName("Deducting more stock than available throws IllegalStateException")
        void deductExceedingStockThrows() {
            assertThrows(IllegalStateException.class, () ->
                    productService.adjustStock(item.getId(), -25));
            assertEquals(20, item.getAvailableStock());
        }
    }
}
