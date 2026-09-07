package com.prospecta.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberUtilsTest {

    @Test
    @DisplayName("Should normalize local 9-digit Senegalese mobile number to E.164")
    void shouldNormalizeSenegaleseLocalNumber() {
        Optional<String> normalized = PhoneNumberUtils.normalizeToE164("771234567");
        assertThat(normalized).isPresent().contains("+221771234567");
    }

    @Test
    @DisplayName("Should normalize number with spaces and dots")
    void shouldNormalizeFormattedNumber() {
        Optional<String> normalized = PhoneNumberUtils.normalizeToE164("78 987.65.43");
        assertThat(normalized).isPresent().contains("+221789876543");
    }

    @Test
    @DisplayName("Should normalize already international Senegalese number")
    void shouldHandleInternationalFormat() {
        Optional<String> normalized = PhoneNumberUtils.normalizeToE164("+221 76 543 21 09");
        assertThat(normalized).isPresent().contains("+221765432109");
    }

    @Test
    @DisplayName("Should return empty for invalid or incomplete number")
    void shouldRejectInvalidNumber() {
        Optional<String> normalized = PhoneNumberUtils.normalizeToE164("12345");
        assertThat(normalized).isEmpty();
    }
}
