package com.store.inventory.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Defines how long a reservation lasts and its order limit.
 * An empty order limit means that the category has no limit.
 */
public record ReservationRules(Duration reservationDuration, OptionalInt orderLimit) {

    public ReservationRules {
        Objects.requireNonNull(reservationDuration, "reservationDuration must not be null");
        Objects.requireNonNull(orderLimit, "orderLimit must not be null");

        if (reservationDuration.isZero() || reservationDuration.isNegative()) {
            throw new IllegalArgumentException("reservationDuration must be positive");
        }
        if (orderLimit.isPresent() && orderLimit.getAsInt() <= 0) {
            throw new IllegalArgumentException("orderLimit must be positive when present");
        }
    }

    /**
     * Returns how long a reservation can stay active.
     *
     * @return reservation duration
     */
    public Duration getReservationDuration() {
        return reservationDuration;
    }

    /**
     * Returns the maximum units allowed in one order.
     * An empty value means that the category has no limit.
     *
     * @return optional order limit
     */
    public OptionalInt getOrderLimit() {
        return orderLimit;
    }
}
