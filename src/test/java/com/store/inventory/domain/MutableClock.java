package com.store.inventory.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Test clock that can move forward in time.
 */
public final class MutableClock extends Clock {

    private Instant currentTime;
    private final ZoneId zone;

    public MutableClock(Instant currentTime, ZoneId zone) {
        this.currentTime = currentTime;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(currentTime, zone);
    }

    @Override
    public Instant instant() {
        return currentTime;
    }

    public void advance(Duration duration) {
        currentTime = currentTime.plus(duration);
    }
}
