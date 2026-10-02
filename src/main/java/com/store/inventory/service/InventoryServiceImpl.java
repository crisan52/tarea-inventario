package com.store.inventory.service;

import com.store.inventory.api.*;
import com.store.inventory.domain.ProductCategoryRules;
import com.store.inventory.domain.Product;
import com.store.inventory.domain.ReservationRules;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps products, stock, and active reservations in memory.
 */
public final class InventoryServiceImpl implements InventoryService {

    private static final int LOW_STOCK_LIMIT = 5;

    private final Clock clock;
    private final StockAlertListener stockAlertListener;
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Reservation> activeReservations = new HashMap<>();
    private final Set<String> lowStockProducts = new HashSet<>();

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
        if (!products.containsKey(sku)) {
            products.put(sku, new Product(category));
        }
    }

    /**
     * Adds positive stock to a registered product.
     */
    @Override
    public synchronized void addStock(String sku, int quantity) {
        validatePositiveQuantity(quantity);

        Product product = products.get(sku);
        if (product == null) {
            throw new IllegalArgumentException("product must be registered");
        }
        product.addStock(quantity);
        checkLowStockProducts(sku, product);
    }

    /**
     * A repeated active order identifier returns its existing reservation.
     */
    @Override
    public synchronized Reservation reserve(String orderId, String sku, int quantity) {
        validatePositiveQuantity(quantity);
        removeExpiredReservations();

        Reservation existingReservation = activeReservations.get(orderId);
        if (existingReservation != null) {
            return existingReservation;
        }

        Product product = products.get(sku);
        if (product == null) {
            throw new InsufficientStockException(sku, quantity, 0);
        }

        ReservationRules rules = ProductCategoryRules.getByCategory(product.getCategory());
        if (rules.getOrderLimit().isPresent() && quantity > rules.getOrderLimit().getAsInt()) {
            throw new OrderLimitExceededException(sku, quantity, rules.getOrderLimit().getAsInt());
        }

        int availableUnits = calculateAvailableUnits(sku, product);
        if (availableUnits < quantity) {
            throw new InsufficientStockException(sku, quantity, availableUnits);
        }

        Reservation reservation = new Reservation(orderId, sku, quantity,
                clock.instant().plus(rules.getReservationDuration()));
        activeReservations.put(orderId, reservation);
        checkLowStockProducts(sku, product);
        return reservation;
    }

    @Override
    public synchronized void confirm(String orderId) {
        removeExpiredReservations();

        Reservation reservation = activeReservations.remove(orderId);
        if (reservation == null) {
            throw new IllegalStateException("order does not have an active reservation: " + orderId);
        }

        Product product = products.get(reservation.sku());
        product.removeStock(reservation.quantity());
    }

    /**
     * Returns the stock that is currently available for the SKU.
     * Unknown products have no available stock.
     */
    @Override
    public synchronized int available(String sku) {
        removeExpiredReservations();

        Product product = products.get(sku);
        return product == null ? 0 : calculateAvailableUnits(sku, product);
    }

    /**
     * Calculates units that can still be reserved for a product.
     * It subtracts units in active reservations from physical stock.
     */
    private int calculateAvailableUnits(String sku, Product product) {
        int reservedUnits = 0;
        for (Reservation reservation : activeReservations.values()) {
            if (Objects.equals(sku, reservation.sku())) {
                reservedUnits += reservation.quantity();
            }
        }
        return product.getStock() - reservedUnits;
    }

    /**
     * Sends one alert when available stock reaches the low-stock limit.
     * The product is rearmed when its available stock recovers above the limit.
     */
    private void checkLowStockProducts(String sku, Product product) {
        int availableUnits = calculateAvailableUnits(sku, product);
        if (availableUnits > LOW_STOCK_LIMIT) {
            lowStockProducts.remove(sku);
            return;
        }
        if (lowStockProducts.add(sku)) {
            stockAlertListener.onLowStock(sku, availableUnits);
        }
    }

    /**
     * Removes reservations that have reached or passed their expiration time.
     */
    private void removeExpiredReservations() {
        Instant currentTime = clock.instant();
        boolean expiredReservationRemoved = activeReservations.values()
                .removeIf(reservation -> !reservation.expiresAt().isAfter(currentTime));
        if (expiredReservationRemoved) {
            resetLowStockAlerts();
        }
    }

    /**
     * Resets alerts for products whose available stock recovered above the limit.
     */
    private void resetLowStockAlerts() {
        for (Map.Entry<String, Product> productEntry : products.entrySet()) {
            int availableUnits = calculateAvailableUnits(productEntry.getKey(), productEntry.getValue());
            if (availableUnits > LOW_STOCK_LIMIT) {
                lowStockProducts.remove(productEntry.getKey());
            }
        }
    }

    /**
     * Ensures that an operation receives at least one unit.
     *
     * @param quantity requested units
     * @throws IllegalArgumentException if the quantity is zero or negative
     */
    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
