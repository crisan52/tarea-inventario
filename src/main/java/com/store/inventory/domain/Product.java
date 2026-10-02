package com.store.inventory.domain;

import com.store.inventory.api.ProductCategory;

/**
 * Holds the inventory state for one product.
 */
public final class Product {

    private final ProductCategory category;
    private int stock;

    /**
     * Creates a product with no stock.
     *
     * @param category category that defines the product rules
     */
    public Product(ProductCategory category) {
        this.category = category;
    }

    /**
     * Increases the physical stock of this product.
     *
     * @param quantity units to add
     */
    public void addStock(int quantity) {
        stock += quantity;
    }

    /**
     * Returns the category used to apply product rules.
     *
     * @return the product category
     */
    public ProductCategory getCategory() {
        return category;
    }

    /**
     * Returns the physical stock stored for this product.
     *
     * @return current stock units
     */
    public int getStock() {
        return stock;
    }
}
