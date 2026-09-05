package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("bourdain.sync")
public record SyncProperties(
        @NotBlank String cron,
        @NotBlank String zone,
        boolean onStartup,
        @NotNull @PositiveOrZero Duration watermarkOverlap) {
}
