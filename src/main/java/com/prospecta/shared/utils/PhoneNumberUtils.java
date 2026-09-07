package com.prospecta.shared.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public final class PhoneNumberUtils {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    public static final String DEFAULT_REGION = "SN"; // Senegal (+221)

    private PhoneNumberUtils() {
    }

    /**
     * Normalizes a phone number to standard E.164 format (+221771234567).
     * Defaults to Senegal (SN) region if no country code is present.
     */
    public static Optional<String> normalizeToE164(String rawNumber) {
        return normalizeToE164(rawNumber, DEFAULT_REGION);
    }

    public static Optional<String> normalizeToE164(String rawNumber, String defaultRegion) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return Optional.empty();
        }

        try {
            String region = (defaultRegion != null && !defaultRegion.isBlank()) ? defaultRegion.toUpperCase() : DEFAULT_REGION;
            PhoneNumber parsed = PHONE_UTIL.parse(rawNumber.trim(), region);
            if (PHONE_UTIL.isValidNumber(parsed)) {
                return Optional.of(PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
            } else {
                log.debug("Phone number [{}] is not valid for region [{}]", rawNumber, region);
                return Optional.empty();
            }
        } catch (NumberParseException e) {
            log.debug("Failed to parse phone number [{}]: {}", rawNumber, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validates whether a given raw number string represents a valid telephone number.
     */
    public static boolean isValidNumber(String rawNumber, String defaultRegion) {
        return normalizeToE164(rawNumber, defaultRegion).isPresent();
    }

    public static boolean isValidNumber(String rawNumber) {
        return normalizeToE164(rawNumber, DEFAULT_REGION).isPresent();
    }
}
