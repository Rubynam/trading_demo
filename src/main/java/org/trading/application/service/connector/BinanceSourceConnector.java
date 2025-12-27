package org.trading.application.service.connector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.trading.application.dto.PriceEventMessage;
import org.trading.application.port.KafkaEventTransformer;
import org.trading.application.service.PartitionStrategy;
import org.trading.application.service.SourceConnector;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.domain.enumeration.AggregatedSource;
import org.trading.util.KafkaKeyUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceSourceConnector implements SourceConnector {

    private final KafkaTemplate<String, PriceEventMessage> kafkaTemplate;
    private final PartitionStrategy partitionStrategy;
    private final KafkaEventTransformer kafkaEventTransformer;
    private static final AggregatedSource BINANCE = AggregatedSource.Binance;

    @Value("${kafka.topics-source}")
    private String topicName;

    @Override
    public void push(List<AggregationPrice> data) {
        for(AggregationPrice item: data){
            Pair<String, AggregationPrice> pair = new ImmutablePair<>(BINANCE.name(), item);
            var event = kafkaEventTransformer.transform(pair);
            int partition = partitionStrategy.partition(BINANCE);
            var producerRecord = this.createProducerRecord(topicName,
                    KafkaKeyUtil.keygen(pair.getLeft(),item.getSymbol()),
                    partition,
                    event);

            this.sendWithRetry(producerRecord);
        }
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    void sendWithRetry(ProducerRecord<String, PriceEventMessage> producerRecord){
        CompletableFuture<SendResult<String, PriceEventMessage>> future= kafkaTemplate.send(producerRecord);
        future.whenComplete((
                result, ex) -> {
            if (ex == null) {
                log.debug("Source data {} success",AggregatedSource.Binance.name());
            } else {
                log.error("Failed to publish price event to Kafka - Topic:",ex);
                // Exception will be caught by @Retryable
                throw new RuntimeException("Kafka publish failed", ex);
            }
        });
    }

}
