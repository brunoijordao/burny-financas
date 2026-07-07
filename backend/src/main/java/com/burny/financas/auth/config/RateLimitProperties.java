package com.burny.financas.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private Limit login = new Limit(5, 60);
    private Limit general = new Limit(100, 60);

    @Getter
    @Setter
    public static class Limit {
        private int capacity;
        private long refillPeriodSeconds;

        public Limit() {
        }

        public Limit(int capacity, long refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}
