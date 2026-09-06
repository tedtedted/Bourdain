package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("bourdain.sync")
public record SyncProperties(
        @NotBlank String cron,
        @NotBlank String zone,
        boolean onStartup,
        @NotNull Duration watermarkOverlap) {

    public SyncProperties {
        Objects.requireNonNull(watermarkOverlap, "watermarkOverlap must not be null");
        if (watermarkOverlap.isNegative()) {
            throw new IllegalArgumentException("watermarkOverlap must not be negative");
        }
    }
}
