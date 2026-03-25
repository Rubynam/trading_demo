package org.trading.domain.enumeration;

import lombok.Getter;

public enum Interval {

    M5("5m",null),
    M15("15m",null),
    M1H("1h",null),
    M4H("4h",null),
    M1D("1d","24hr");

    @Getter
    private final String interval;

    @Getter
    private final String tickerInternval;

    Interval(String interval, String tickerInternval) {
        this.interval = interval;
        this.tickerInternval = tickerInternval;
    }
}
