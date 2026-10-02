package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.domain.MutableClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InventoryReservationTest {

    private static final Instant NOW = Instant.parse("2026-10-02T05:00:00Z");

    private InventoryService inventoryWithStock(ProductCategory category, int stock) {
        InventoryService inventory = Inventory.create(Clock.fixed(NOW, ZoneOffset.UTC), (sku, available) -> { });
        inventory.registerProduct("SKU-1", category);
        inventory.addStock("SKU-1", stock);
        return inventory;
    }

    @Test
    void standardCategoryHasFifteenMinuteReservationPeriod() {
        InventoryService inventory = inventoryWithStock(ProductCategory.STANDARD, 10);

        Reservation reservation = inventory.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(new Reservation("ORDER-1", "SKU-1", 3, NOW.plus(Duration.ofMinutes(15))), reservation);
        assertEquals(7, inventory.available("SKU-1"));
    }

    @Test
    void preOrderCategoryHasTwentyFourHourReservationPeriod() {
        InventoryService inventory = inventoryWithStock(ProductCategory.PRE_ORDER, 10);

        Reservation reservation = inventory.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(new Reservation("ORDER-1", "SKU-1", 3, NOW.plus(Duration.ofHours(24))), reservation);
    }

    @Test
    void flashSaleCategoryHasFiveMinuteReservationPeriod() {
        InventoryService inventory = inventoryWithStock(ProductCategory.FLASH_SALE, 10);

        Reservation reservation = inventory.reserve("ORDER-1", "SKU-1", 2);

        assertEquals(new Reservation("ORDER-1", "SKU-1", 2, NOW.plus(Duration.ofMinutes(5))), reservation);
        assertEquals(8, inventory.available("SKU-1"));
    }

    @Test
    void flashSaleCategoryHasTwoUnitOrderLimit() {
        InventoryService inventory = inventoryWithStock(ProductCategory.FLASH_SALE, 10);

        assertThrows(OrderLimitExceededException.class, () -> inventory.reserve("ORDER-1", "SKU-1", 3));
    }

    @Test
    void doesNotAllowReservingZeroOrNegativeUnits() {
        InventoryService inventory = inventoryWithStock(ProductCategory.STANDARD, 10);

        assertThrows(IllegalArgumentException.class, () -> inventory.reserve("ORDER-1", "SKU-1", 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.reserve("ORDER-2", "SKU-1", -1));
    }

    @Test
    void reservingAnUnregisteredProductReportsNoAvailableStock() {
        InventoryService inventory = Inventory.create(Clock.fixed(NOW, ZoneOffset.UTC), (sku, available) -> { });

        assertThrows(InsufficientStockException.class, () -> inventory.reserve("ORDER-1", "UNKNOWN-SKU", 1));
    }

    @Test
    void expiredReservationReleasesItsUnits() {
        MutableClock clock = new MutableClock(NOW, ZoneOffset.UTC);
        InventoryService inventory = Inventory.create(clock, (sku, available) -> { });
        inventory.registerProduct("SKU-1", ProductCategory.STANDARD);
        inventory.addStock("SKU-1", 10);
        inventory.reserve("ORDER-1", "SKU-1", 3);

        clock.advance(Duration.ofMinutes(15));

        assertEquals(10, inventory.available("SKU-1"));
    }

    @Test
    void repeatedOrderReturnsTheExistingReservationWithoutReducingStockAgain() {
        InventoryService inventory = inventoryWithStock(ProductCategory.STANDARD, 10);

        Reservation firstReservation = inventory.reserve("ORDER-1", "SKU-1", 3);
        Reservation repeatedReservation = inventory.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(firstReservation, repeatedReservation);
        assertEquals(7, inventory.available("SKU-1"));
    }

    @Test
    void confirmingAnOrderWithoutAnActiveReservationFails() {
        InventoryService inventory = inventoryWithStock(ProductCategory.STANDARD, 10);

        assertThrows(IllegalStateException.class, () -> inventory.confirm("ORDER-1"));
    }

    @Test
    void confirmingAnOrderTwiceFailsAfterTheFirstConfirmation() {
        InventoryService inventory = inventoryWithStock(ProductCategory.STANDARD, 10);
        inventory.reserve("ORDER-1", "SKU-1", 3);
        inventory.confirm("ORDER-1");

        assertEquals(7, inventory.available("SKU-1"));
        assertThrows(IllegalStateException.class, () -> inventory.confirm("ORDER-1"));
    }

    @Test
    void confirmingAnExpiredReservationFailsAndLeavesUnitsAvailable() {
        MutableClock clock = new MutableClock(NOW, ZoneOffset.UTC);
        InventoryService inventory = Inventory.create(clock, (sku, available) -> { });
        inventory.registerProduct("SKU-1", ProductCategory.STANDARD);
        inventory.addStock("SKU-1", 10);
        inventory.reserve("ORDER-1", "SKU-1", 3);

        clock.advance(Duration.ofMinutes(15));

        assertThrows(IllegalStateException.class, () -> inventory.confirm("ORDER-1"));
        assertEquals(10, inventory.available("SKU-1"));
    }
}
