package com.store.inventory.service;

import com.store.inventory.api.*;
import com.store.inventory.domain.Product;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keeps product and stock data in memory.
 */
public final class InventoryServiceImpl implements InventoryService {

    private final Clock clock;
    private final StockAlertListener stockAlertListener;
    private final Map<String, Product> products = new HashMap<>();

    /**
     * Creates the service with the resources needed for inventory rules.
     *
     * @param clock source of the current time
     * @param stockAlertListener receiver of low stock alerts
     */
    public InventoryServiceImpl(Clock clock, StockAlertListener stockAlertListener) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.stockAlertListener = Objects.requireNonNull(stockAlertListener, "StockAlertListener must not be null");
    }

    /**
     * Stores a product when its SKU has not been registered yet.
     * A repeated registration keeps the original product and stock.
     */
    @Override
    public synchronized void registerProduct(String sku, ProductCategory category) {
        // Let only not registered products
        if (!products.containsKey(sku)) {
            products.put(sku, new Product(category));
        }
    }

    /**
     * Adds positive stock to a registered product.
     */
    @Override
    public synchronized void addStock(String sku, int quantity) {
        Product product = products.get(sku);
        if (quantity <= 0 || product == null) {
            throw new IllegalArgumentException("quantity must be positive and product must be registered");
        }
        product.addStock(quantity);
    }

    /**
     * Reservation support is not part of the current implementation step.
     */
    @Override
    public Reservation reserve(String orderId, String sku, int quantity) {

        Product product = products.get(sku);
        if (quantity <= 0 || product == null) {
            throw new IllegalArgumentException("quantity must be positive and product must be registered");
        }

        if (product.getStock() < quantity) {
            throw new InsufficientStockException(sku, quantity, product.getStock());
        }
        //return new Reservation(orderId, sku, quantity, new Instant());
        throw new UnsupportedOperationException("Reservation is not implemented yet");
    }

    /**
     * Reservation confirmation is not part of the current implementation step.
     */
    @Override
    public void confirm(String orderId) {
        throw new UnsupportedOperationException("Reservation confirmation is not implemented yet");
    }

    /**
     * Returns the stock that is currently available for the SKU.
     * Unknown products have no available stock.
     */
    @Override
    public synchronized int available(String sku) {
        Product product = products.get(sku);
        return product == null ? 0 : product.getStock();
    }
}
