package com.shopmicro.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports DOWN when app.fail=true (env var APP_FAIL=true).
 * Used to simulate a broken release.
 */
@Component
public class FailSwitchHealthIndicator implements HealthIndicator {

    @Value("${app.fail:false}")
    private boolean fail;

    @Override
    public Health health() {
        if (fail) {
            return Health.down().withDetail("reason", "failure switch is ON").build();
        }
        return Health.up().build();
    }
}
