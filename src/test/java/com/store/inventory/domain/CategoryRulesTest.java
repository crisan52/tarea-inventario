package com.store.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.store.inventory.api.ProductCategory;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CategoryRulesTest {

    @Test
    void standardHasFifteenMinuteReservationAndNoOrderLimit() {
        ReservationRules rules = ProductCategoryRules.getByCategory(ProductCategory.STANDARD);

        assertEquals(Duration.ofMinutes(15), rules.getReservationDuration());
        assertFalse(rules.getOrderLimit().isPresent());
    }

    @Test
    void preOrderHasTwentyFourHourReservationAndNoOrderLimit() {
        ReservationRules rules = ProductCategoryRules.getByCategory(ProductCategory.PRE_ORDER);

        assertEquals(Duration.ofHours(24), rules.getReservationDuration());
        assertFalse(rules.getOrderLimit().isPresent());
    }

    @Test
    void flashSaleHasFiveMinuteReservationAndTwoUnitOrderLimit() {
        ReservationRules rules = ProductCategoryRules.getByCategory(ProductCategory.FLASH_SALE);

        assertEquals(Duration.ofMinutes(5), rules.getReservationDuration());
        assertTrue(rules.getOrderLimit().isPresent());
        assertEquals(2, rules.getOrderLimit().getAsInt());
    }
}
