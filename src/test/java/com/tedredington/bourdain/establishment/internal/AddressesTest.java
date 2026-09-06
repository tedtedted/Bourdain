package com.tedredington.bourdain.establishment.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressesTest {

    @Test
    void suiteSuffixesAreTheSameLocation() {
        assertThat(Addresses.sameLocation("2913 N CLARK ST ", "2913 N CLARK ST 1ST")).isTrue();
        assertThat(Addresses.sameLocation("2913 N CLARK ST", "2913 N CLARK ST SUITE 200")).isTrue();
    }

    @Test
    void differentHouseNumbersAreDifferentLocations() {
        assertThat(Addresses.sameLocation("2913 N CLARK ST", "2827 N BROADWAY 1")).isFalse();
    }

    @Test
    void sameHouseNumberOnDifferentStreetsIsADifferentLocation() {
        assertThat(Addresses.sameLocation("100 W MADISON ST", "100 E MADISON ST")).isFalse();
    }

    @Test
    void addressesWithoutHouseNumbersCompareAsText() {
        assertThat(Addresses.houseNumber("NAVY PIER")).isEqualTo("NAVY PIER");
        assertThat(Addresses.sameLocation("NAVY PIER", "navy pier")).isTrue();
    }
}
