package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.domain.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryStockAlertTest {

    private static final Instant NOW = Instant.parse("2026-10-02T05:00:00Z");

    private MutableClock clock;
    private List<StockAlert> alerts;
    private InventoryService inventory;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW, ZoneOffset.UTC);
        alerts = new ArrayList<>();
        inventory = Inventory.create(clock,
                (sku, availableUnits) -> alerts.add(new StockAlert(sku, availableUnits)));
        inventory.registerProduct("SKU-1", ProductCategory.STANDARD);
        inventory.addStock("SKU-1", 10);
    }

    @Test
    void sendsOneAlertWhenAvailableStockReachesFiveUnits() {
        inventory.reserve("ORDER-1", "SKU-1", 5);
        inventory.reserve("ORDER-2", "SKU-1", 1);

        assertEquals(List.of(new StockAlert("SKU-1", 5)), alerts);
    }

    @Test
    void sendsANewAlertAfterStockRecoversAboveTheLimit() {
        inventory.reserve("ORDER-1", "SKU-1", 5);

        inventory.addStock("SKU-1", 1);
        inventory.reserve("ORDER-2", "SKU-1", 1);

        assertEquals(List.of(new StockAlert("SKU-1", 5), new StockAlert("SKU-1", 5)), alerts);
    }

    @Test
    void expirationRearmsTheAlertWhenItRestoresAvailableStock() {
        inventory.reserve("ORDER-1", "SKU-1", 6);

        clock.advance(Duration.ofMinutes(15));
        inventory.available("SKU-1");
        inventory.reserve("ORDER-2", "SKU-1", 5);

        assertEquals(List.of(new StockAlert("SKU-1", 4), new StockAlert("SKU-1", 5)), alerts);
    }

    private record StockAlert(String sku, int availableUnits) {
    }

}
