package org.example.service;

import org.example.exception.CategoryNotFoundException;
import org.example.exception.InvalidInputException;
import org.example.exception.ProductNotFoundException;
import org.example.model.entity.Product;
import org.example.model.vo.Category;
import org.example.service.port.CategoryRepository;
import org.example.service.port.ProductRepository;

/**
 * Package-private helper centralizing shared product lookup, active-category validation,
 * and storefront visibility rules across catalog query and management services.
 */
final class CatalogLookupSupport {

    private CatalogLookupSupport() {
    }

    static Product requireProduct(ProductRepository productRepository, String productId) {
        if (productId == null || productId.isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank");
        }
        String id = productId.trim();
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID '" + productId + "' was not found"));
    }

    static Category requireActiveCategory(CategoryRepository categoryRepository, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new InvalidInputException("Category ID cannot be null or blank");
        }
        String id = categoryId.trim();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Active category not found: " + categoryId));
        if (!category.active()) {
            throw new CategoryNotFoundException("Active category not found: " + categoryId);
        }
        return category;
    }

    static boolean isVisible(Product product, CategoryRepository categoryRepository) {
        if (!product.isActive()) {
            return false;
        }
        return categoryRepository.findById(product.getCategoryId())
                .map(Category::active)
                .orElse(false);
    }
}
