package com.tedredington.bourdain.civicdata.internal;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("bourdain.sync")
public record SyncProperties(@NotBlank String cron, @NotBlank String zone, boolean onStartup) {
}
