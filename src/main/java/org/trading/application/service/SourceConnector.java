package org.trading.application.service;


import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.trading.application.dto.PriceEventMessage;
import org.trading.domain.aggregates.AggregationPrice;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public interface SourceConnector {

    void push(List<AggregationPrice> data);


    default List<Header> createHeaders(PriceEventMessage event){
        List<Header> headers = new ArrayList<>();
        headers.add(new RecordHeader("symbol",
                event.getSymbol().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("source",
                event.getSource().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("eventId",
                event.getEventId().getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

    default ProducerRecord<String, PriceEventMessage> createProducerRecord(
            String topic,
            String partitionKey,
            int partition,
            PriceEventMessage event
    ) {
        // Add metadata headers for filtering and routing
        List<Header> headers = createHeaders(event);

        return new ProducerRecord<>(
                topic,          // Topic name
                partition,           // Partition (determined by partitioner)
                partitionKey,   // Key for partitioning
                event,          // Message payload
                headers         // Custom headers
        );
    }
}
