package org.trading.application.port;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.trading.application.dto.PriceEventMessage;
import org.trading.domain.aggregates.AggregationPrice;

import java.util.UUID;

@Service
public class KafkaEventTransformer implements Transformer<Pair<String,AggregationPrice>, PriceEventMessage> {

    @Override
    public PriceEventMessage transform(Pair<String,AggregationPrice> input) throws IllegalArgumentException {
        String source = input.getLeft();
        AggregationPrice data = input.getRight();

        return PriceEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .source(source)
                .symbol(data.getSymbol())
                .bidPrice(data.getBidPrice())
                .bidQty(data.getBidQty())
                .askPrice(data.getAskPrice())
                .askQty(data.getAskQty())
                .build();
    }

    @Override
    public Pair<String,AggregationPrice> reverseTransform(PriceEventMessage output) throws IllegalArgumentException {
        throw new UnsupportedOperationException("can not support this operation");
    }
}
