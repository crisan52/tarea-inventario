package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryProductTest {

    private InventoryService inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.create(Clock.systemUTC(), (sku, available) -> { });
        inventory.registerProduct("SKU-1", ProductCategory.STANDARD);
    }

    @Test
    void doesNotAllowAddingZeroOrNegativeUnits() {
        assertThrows(IllegalArgumentException.class, () -> inventory.addStock("SKU-1", 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.addStock("SKU-1", -1));
    }

    @Test
    void doesNotAllowAddingStockToAnUnregisteredProduct() {
        assertThrows(IllegalArgumentException.class, () -> inventory.addStock("UNKNOWN-SKU", 1));
    }

    @Test
    void registeredProductCanReceiveAndReportStock() {
        inventory.addStock("SKU-1", 10);

        assertEquals(10, inventory.available("SKU-1"));
    }
}
