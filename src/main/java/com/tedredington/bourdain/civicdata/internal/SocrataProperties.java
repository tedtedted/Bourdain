package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("bourdain.socrata")
public record SocrataProperties(
        @NotBlank String baseUrl,
        String appToken,
        @Min(100) @Max(50_000) int pageSize,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(1) int maxAttempts,
        @NotNull Duration retryBackoff,
        @NotBlank String inspectionsDataset,
        @NotBlank String licensesDataset,
        @NotEmpty List<String> licenseDescriptions) {

    public SocrataProperties {
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(readTimeout, "readTimeout must not be null");
        Objects.requireNonNull(retryBackoff, "retryBackoff must not be null");
        if (retryBackoff.isNegative()) {
            throw new IllegalArgumentException("retryBackoff must not be negative");
        }
    }

    public boolean hasAppToken() {
        return appToken != null && !appToken.isBlank();
    }
}
