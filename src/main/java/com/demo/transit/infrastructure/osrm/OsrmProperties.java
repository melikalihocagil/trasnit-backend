package com.demo.transit.infrastructure.osrm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "osrm")
public record OsrmProperties(
        String baseUrl,
        int timeoutMs
) {
    public OsrmProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:5000";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
    }
}
