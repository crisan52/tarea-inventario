package com.store.inventory;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.StockAlertListener;
import com.store.inventory.service.InventoryServiceImpl;
import java.time.Clock;

/**
 * Entry point used by our automated tests. Keep this signature exactly as it is,
 * and build your implementation here.
 */
public final class Inventory {

    private Inventory() {
    }

    /**
     * Creates an inventory service that keeps its data in memory.
     *
     * @param clock source of the current time for reservation rules
     * @param alertListener receiver of low stock alerts
     * @return a new inventory service
     */
    public static InventoryService create(Clock clock, StockAlertListener alertListener) {
        return new InventoryServiceImpl(clock, alertListener);
    }
}
