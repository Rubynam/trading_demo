package org.trading.util;

import org.trading.domain.enumeration.AggregatedSource;

public final class KafkaKeyUtil {
    private KafkaKeyUtil(){

    }

    public static String keygen(String provideSource, String symbol){
        return String.format("%s:%s", provideSource, symbol);
    }

    public static AggregatedSource extractKey(String compositeKey){
        return AggregatedSource.valueOf(compositeKey.split(":")[0]);
    }
}
