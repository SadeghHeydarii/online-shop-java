package org.example.model.entity;

import java.util.Objects;
import org.example.model.vo.Money;
import org.example.model.vo.Quantity;

/**
 * Represents a product entity in the online shop catalog.
 *
 * <p>A product possesses a unique immutable identifier ({@code id}), commercial title,
 * detailed description, current price, warehouse stock level, parent category identifier,
 * storefront publication status flag, aggregated average review rating, and total review count.
 *
 * <p><b>Abstraction Function (AF):</b>
 * AF(id, name, description, price, availableStock, categoryId, active, averageRating, reviewCount) =
 * A catalog product with unique ID {@code id}, titled {@code name}, described by {@code description},
 * priced at {@code price}, with available physical inventory {@code availableStock}, belonging
 * to category {@code categoryId}, published in storefront if {@code active}, with calculated
 * customer rating {@code averageRating} across {@code reviewCount} reviews.
 *
 * <p><b>Representation Invariant (RI):</b>
 * <ul>
 *   <li>{@code id != null && !id.isBlank()}</li>
 *   <li>{@code name != null && !name.isBlank()}</li>
 *   <li>{@code description != null}</li>
 *   <li>{@code price != null && !price.isNegative()}</li>
 *   <li>{@code availableStock >= 0}</li>
 *   <li>{@code categoryId != null && !categoryId.isBlank()}</li>
 *   <li>{@code reviewCount >= 0}</li>
 *   <li>{@code averageRating >= 0.0 && averageRating <= 5.0}</li>
 *   <li>If {@code reviewCount == 0}, then {@code averageRating == 0.0}</li>
 * </ul>
 *
 * <p><b>Safety from Representation Exposure:</b>
 * All fields are private. Primitive types, immutable {@link String}, and immutable
 * Value Objects ({@link Money}, {@link Quantity}) are used throughout.
 */
public class Product {

    private final String id;
    private String name;
    private String description;
    private Money price;
    private int availableStock;
    private String categoryId;
    private boolean active;
    private double averageRating;
    private int reviewCount;

    /**
     * Constructs a new {@code Product} entity with active storefront status,
     * zero rating, and zero review count.
     *
     * @param id           the unique identifier of the product (non-blank)
     * @param name         the commercial title/name of the product (non-blank)
     * @param description  the detailed description of the product (non-null)
     * @param price        the non-negative price of the product
     * @param initialStock the initial non-negative physical warehouse stock
     * @param categoryId   the identifier of the product's category (non-blank)
     * @throws IllegalArgumentException if any argument violates representation invariants
     */
    public Product(String id, String name, String description, Money price,
                   int initialStock, String categoryId) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (description == null) {
            throw new IllegalArgumentException("Product description cannot be null");
        }
        if (price == null || price.isNegative()) {
            throw new IllegalArgumentException("Product price must be non-null and non-negative");
        }
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock must be non-negative: " + initialStock);
        }
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Category ID cannot be null or blank");
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.availableStock = initialStock;
        this.categoryId = categoryId;
        this.active = true;
        this.averageRating = 0.0;
        this.reviewCount = 0;
        checkRep();
    }

    /**
     * Asserts the representation invariant of the Product entity.
     */
    private void checkRep() {
        assert id != null && !id.isBlank() : "Product id cannot be null or blank";
        assert name != null && !name.isBlank() : "Product name cannot be null or blank";
        assert description != null : "Product description cannot be null";
        assert price != null && !price.isNegative() : "Product price must be non-null and non-negative";
        assert availableStock >= 0 : "Available stock must be non-negative: " + availableStock;
        assert categoryId != null && !categoryId.isBlank() : "Category id cannot be null or blank";
        assert reviewCount >= 0 : "Review count must be non-negative: " + reviewCount;
        assert averageRating >= 0.0 && averageRating <= 5.0 : "Average rating must be between 0.0 and 5.0: " + averageRating;
        if (reviewCount == 0) {
            assert averageRating == 0.0 : "Average rating must be 0.0 when reviewCount is 0";
        }
    }

    /**
     * Returns the unique immutable identifier of this product.
     *
     * @return product ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the commercial title of this product.
     *
     * @return product name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the detailed description of this product.
     *
     * @return product description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the current monetary price of this product.
     *
     * @return price
     */
    public Money getPrice() {
        return price;
    }

    /**
     * Returns the current available inventory count in the warehouse.
     *
     * @return available stock
     */
    public int getAvailableStock() {
        return availableStock;
    }

    /**
     * Returns the category identifier to which this product belongs.
     *
     * @return category ID
     */
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * Checks whether this product is active for display and sale in the storefront.
     *
     * @return {@code true} if active; {@code false} otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the calculated average customer review rating (between 0.0 and 5.0).
     *
     * @return average rating score
     */
    public double getAverageRating() {
        return averageRating;
    }

    /**
     * Returns the total count of customer reviews submitted for this product.
     *
     * @return review count
     */
    public int getReviewCount() {
        return reviewCount;
    }

    /**
     * Checks whether the warehouse stock is sufficient to satisfy the requested quantity.
     *
     * @param required the requested quantity
     * @return {@code true} if available stock is greater than or equal to required quantity; {@code false} otherwise
     * @throws IllegalArgumentException if required is null
     */
    public boolean isAvailable(Quantity required) {
        if (required == null) {
            throw new IllegalArgumentException("Required quantity cannot be null");
        }
        return this.availableStock >= required.value();
    }

    /**
     * Updates editable commercial details of the product.
     *
     * @param name       the updated product name (non-blank)
     * @param desc       the updated product description (non-null)
     * @param price      the updated non-negative price
     * @param categoryId the updated category identifier (non-blank)
     * @throws IllegalArgumentException if any argument is invalid
     */
    public void updateDetails(String name, String desc, Money price, String categoryId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (desc == null) {
            throw new IllegalArgumentException("Product description cannot be null");
        }
        if (price == null || price.isNegative()) {
            throw new IllegalArgumentException("Product price must be non-null and non-negative");
        }
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Category ID cannot be null or blank");
        }

        this.name = name;
        this.description = desc;
        this.price = price;
        this.categoryId = categoryId;
        checkRep();
    }

    /**
     * Sets the active storefront publication status of this product.
     *
     * @param active {@code true} to activate; {@code false} to deactivate
     */
    public void setActive(boolean active) {
        this.active = active;
        checkRep();
    }

    /**
     * Deducts a specified quantity from the available warehouse stock.
     *
     * @param quantity the quantity to deduct
     * @throws IllegalArgumentException if quantity is null
     * @throws IllegalStateException    if quantity exceeds available stock
     */
    public void deductStock(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Deduction quantity cannot be null");
        }
        if (quantity.value() > this.availableStock) {
            throw new IllegalStateException("Insufficient stock to deduct " + quantity.value()
                    + "; current available: " + this.availableStock);
        }
        this.availableStock -= quantity.value();
        checkRep();
    }

    /**
     * Restocks the warehouse inventory by adding the specified quantity.
     *
     * @param quantity the quantity to add to available stock
     * @throws IllegalArgumentException if quantity is null
     */
    public void restock(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Restock quantity cannot be null");
        }
        this.availableStock += quantity.value();
        checkRep();
    }

    /**
     * Updates the running average rating and review count with a newly submitted rating score.
     *
     * @param newRatingScore the new rating score (must be between 1 and 5)
     * @throws IllegalArgumentException if newRatingScore is outside the range [1, 5]
     */
    public void updateRatingStats(int newRatingScore) {
        if (newRatingScore < 1 || newRatingScore > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5, got: " + newRatingScore);
        }
        double totalScore = (this.averageRating * this.reviewCount) + newRatingScore;
        this.reviewCount++;
        this.averageRating = totalScore / this.reviewCount;
        checkRep();
    }

    /**
     * Compares this product to another object for equality based on entity identity ({@code id}).
     *
     * @param o the object to compare with
     * @return {@code true} if both are Product entities with identical IDs; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    /**
     * Returns the hash code based on entity identity ({@code id}).
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product[id=" + id
                + ", name=" + name
                + ", price=" + price
                + ", stock=" + availableStock
                + ", category=" + categoryId
                + ", active=" + active
                + ", rating=" + averageRating
                + ", reviews=" + reviewCount
                + "]";
    }
}
