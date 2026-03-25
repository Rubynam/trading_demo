package org.trading.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public final class NumberUtils {
    private NumberUtils(){

    }

    public static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse value to BigDecimal: {}", value);
            return BigDecimal.ZERO;
        }
    }
}
