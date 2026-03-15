package org.trading.domain.enumeration;

public enum FrequencyCondition {
    ONLY_ONCE,          // Trigger only once
    PER_DAY,            // Trigger once per day
    ALWAYS_PER_MINUTE   // Trigger continuously (max once per minute)
}
