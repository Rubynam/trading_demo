package org.trading.insfrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.trading.application.dto.PriceEventMessage;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.producer.acks}")
    private String acks;

    @Value("${kafka.producer.retries}")
    private int retries;

    @Value("${kafka.producer.batch-size}")
    private int batchSize;

    @Value("${kafka.producer.linger-ms}")
    private int lingerMs;

    @Value("${kafka.producer.compression-type}")
    private String compressionType;

    @Value("${kafka.producer.max-in-flight-requests-per-connection}")
    private int maxInFlightRequestsPerConnection;

    @Value("${kafka.producer.enable-idempotence}")
    private boolean enableIdempotence;

    @Value("${kafka.producer.retry-backoff-ms}")
    private int retryBackoffMs;

    @Bean
    public ProducerFactory<String, PriceEventMessage> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Bootstrap servers
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializers
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Acknowledgment and reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);

        // Retry mechanism
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);

        // Request timeout - increased to 60 seconds for retry mechanism
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);

        // Delivery timeout - increased to allow for retries (2 minutes)
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        // Performance tuning
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);

        // Buffer memory - 32MB
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // JSON serializer settings
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, PriceEventMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}