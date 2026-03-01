package org.trading.domain.enumeration;


public enum AlertStatus {
    ENABLED,      // Alert is active and monitoring
    DISABLED,     // Alert is paused/stopped
    TRIGGERED,    // Alert has been triggered
    EXPIRED       // Alert has reached max hit count
}
