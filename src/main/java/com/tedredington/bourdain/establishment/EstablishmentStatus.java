package com.tedredington.bourdain.establishment;

/**
 * Derived after each sync. {@code CLOSED} means the latest inspection said
 * "Out of Business"; {@code RELOCATED} upgrades that when the same name holds
 * an active license at a different address.
 */
public enum EstablishmentStatus {
    OPEN,
    CLOSED,
    RELOCATED
}
