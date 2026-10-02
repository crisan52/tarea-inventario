package com.store.inventory.domain;

import com.store.inventory.api.ProductCategory;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Provides the reservation rules for every supported product category.
 */
public final class ProductCategoryRules {

    private static final long STANDARD_RESERVATION_MINUTES = 15;
    private static final long PRE_ORDER_RESERVATION_HOURS = 24;
    private static final long FLASH_SALE_RESERVATION_MINUTES = 5;
    private static final int FLASH_SALE_ORDER_LIMIT = 2;

    private static final Map<ProductCategory, ReservationRules> RULES = createRules();

    private ProductCategoryRules() {
    }

    private static Map<ProductCategory, ReservationRules> createRules() {
        Map<ProductCategory, ReservationRules> rules = new EnumMap<>(ProductCategory.class);
        rules.put(ProductCategory.STANDARD,
                new ReservationRules(Duration.ofMinutes(STANDARD_RESERVATION_MINUTES), OptionalInt.empty()));
        rules.put(ProductCategory.PRE_ORDER,
                new ReservationRules(Duration.ofHours(PRE_ORDER_RESERVATION_HOURS), OptionalInt.empty()));
        rules.put(ProductCategory.FLASH_SALE,
                new ReservationRules(Duration.ofMinutes(FLASH_SALE_RESERVATION_MINUTES),
                        OptionalInt.of(FLASH_SALE_ORDER_LIMIT)));
        return Map.copyOf(rules);
    }

    /**
     * Returns the rules configured for a product category.
     *
     * @param category product category
     * @return reservation rules for the category
     */
    public static ReservationRules getByCategory(ProductCategory category) {
        ReservationRules rules = RULES.get(Objects.requireNonNull(category, "category must not be null"));
        if (rules == null) {
            throw new IllegalArgumentException("Unsupported product category: " + category);
        }
        return rules;
    }
}
