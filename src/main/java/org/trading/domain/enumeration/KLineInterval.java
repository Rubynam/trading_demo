package org.trading.domain.enumeration;

import lombok.Getter;

public enum KLineInterval {

    M5("5m"),
    M15("15m"),
    M1H("1h"),
    M4H("4h"),
    M1D("1D");
    @Getter
    private final String value;

    KLineInterval(String value) {
        this.value = value;
    }
}
