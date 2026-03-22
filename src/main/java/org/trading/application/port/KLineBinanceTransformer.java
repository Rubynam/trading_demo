package org.trading.application.port;

import org.springframework.stereotype.Component;
import org.trading.domain.aggregates.KLineData;
import org.trading.insfrastructure.mapper.BinanceKLine;

@Component
public class KLineBinanceTransformer implements Transformer<BinanceKLine, KLineData> {
    @Override
    public KLineData transform(BinanceKLine input) throws IllegalArgumentException {
        return KLineData.builder()
                .time(input.openTime())//todo need to format
                .open(input.open())
                .build();
    }

    @Override
    public BinanceKLine reverseTransform(KLineData output) throws IllegalArgumentException {
        return null;
    }
}
