# Source Connector Design for Kafka Integration

## Task 1 Review

### Implemented Components (Task 1)
Task 1 focused on creating Kafka producers to stream price data from Binance and Huobi exchanges:

**Key Requirements:**
- **Topics**: `inbound-data-binance` and `inbound-data-huobi` for provider-specific data streams
- **Apache Spark Integration**: Added to build.gradle for stream processing capabilities
- **Retry Mechanism**: Fault tolerance for Kafka broker failures
- **Partitioning Strategy**:
  - Primary partitioning by provider source (Binance, Huobi)
  - Sub-partitioning by symbol (ETHUSDT, BTCUSDT)

**Observations:**
- The current implementation uses direct Kafka producers
- No abstraction layer exists for different data sources
- Lacks a unified interface for connecting to multiple exchanges
- No standardized pattern for adding new data providers

## Source Connector Design Pattern

### 1. Pattern Overview

The **Source Connector Pattern** provides an abstraction layer between external price data sources (Binance, Huobi, future exchanges) and the Kafka streaming pipeline. It follows the **Adapter Pattern** and **Strategy Pattern** to enable:

- Uniform interface for all exchange connectors
- Easy addition of new exchanges without modifying core logic
- Separation of concerns between data retrieval and data publishing
- Standardized error handling and retry mechanisms

### 2. Connector Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Source Connector Layer                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           SourceConnector Interface                       │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  + connect(): Connection                           │  │  │
│  │  │  + fetchPriceData(): PriceData                     │  │  │
│  │  │  + getProviderSource(): ProviderSource             │  │  │
│  │  │  + healthCheck(): boolean                          │  │  │
│  │  │  + disconnect(): void                              │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                          ▲                                       │
│                          │                                       │
│         ┌────────────────┴────────────────┐                    │
│         │                                  │                    │
│  ┌──────▼──────┐                  ┌───────▼────────┐          │
│  │   Binance   │                  │     Huobi      │          │
│  │  Connector  │                  │   Connector    │          │
│  │             │                  │                │          │
│  │ - apiUrl    │                  │  - apiUrl      │          │
│  │ - apiKey    │                  │  - apiKey      │          │
│  │ - transform │                  │  - transform   │          │
│  └─────────────┘                  └────────────────┘          │
│                                                                   │
└───────────────────────────────┬───────────────────────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │  Kafka Producer Pool  │
                    │                       │
                    │  Topic Router:        │
                    │  - Binance → Topic 0  │
                    │  - Huobi → Topic 1    │
                    └───────────────────────┘
```

### 3. Interface Design

#### 3.1 SourceConnector Interface

```java
/**
 * Base interface for all exchange data source connectors.
 * Implements the Strategy Pattern to allow dynamic selection of data sources.
 */
public interface SourceConnector {

    /**
     * Establishes connection to the external data source.
     * @return Connection metadata
     * @throws ConnectorException if connection fails
     */
    ConnectorMetadata connect() throws ConnectorException;

    /**
     * Fetches current price data from the exchange.
     * @param symbols List of trading symbols to fetch
     * @return Raw price data from the source
     * @throws DataFetchException if retrieval fails
     */
    PriceDataResponse fetchPriceData(List<Symbol> symbols) throws DataFetchException;

    /**
     * Returns the provider source identifier.
     * Used for Kafka partitioning and routing.
     * @return ProviderSource enum (BINANCE, HUOBI)
     */
    ProviderSource getProviderSource();

    /**
     * Performs health check on the connection.
     * @return true if connection is healthy, false otherwise
     */
    boolean healthCheck();

    /**
     * Gracefully disconnects from the data source.
     */
    void disconnect();

    /**
     * Returns the retry configuration for this connector.
     * @return RetryPolicy with backoff and max attempts
     */
    RetryPolicy getRetryPolicy();
}
```

#### 3.2 Supporting Types

```java
/**
 * Provider source enumeration
 */
public enum ProviderSource {
    BINANCE("inbound-data-binance"),
    HUOBI("inbound-data-huobi");

    private final String topicName;

    ProviderSource(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}

/**
 * Connector metadata returned on successful connection
 */
public class ConnectorMetadata {
    private ProviderSource source;
    private String connectionId;
    private LocalDateTime connectedAt;
    private Map<String, String> metadata;
}

/**
 * Retry policy for fault tolerance
 */
public class RetryPolicy {
    private int maxAttempts;          // Default: 3
    private long initialBackoffMs;    // Default: 1000ms
    private long maxBackoffMs;        // Default: 30000ms
    private double backoffMultiplier; // Default: 2.0
    private boolean exponentialBackoff;
}
```

### 4. Kafka Message Key Design

#### 4.1 Key Structure

The Kafka message key determines partition assignment and ensures ordering guarantees. The key design follows this hierarchy:

**Composite Key Pattern:**
```
{providerSource}:{symbol}:{timestamp}
```

**Examples:**
- `BINANCE:ETHUSDT:1703001234567`
- `HUOBI:BTCUSDT:1703001235890`

#### 4.2 Key Design Rationale

**Provider Source as Primary Key Component:**
- **Partition Affinity**: All messages from the same provider go to the same partition
- **Ordering Guarantee**: Events from a single source maintain temporal order
- **Load Distribution**: Balanced load across partitions (1 partition per provider)

**Symbol as Secondary Key Component:**
- **Sub-partitioning**: Within a provider partition, messages are grouped by symbol
- **Consumer Optimization**: Consumers can filter by symbol efficiently
- **Parallelism**: Enables symbol-level parallel processing in Spark

**Timestamp as Tertiary Component:**
- **Uniqueness**: Prevents key collision for simultaneous events
- **Time-based Ordering**: Preserves temporal sequence
- **Idempotency**: Allows detection of duplicate events

#### 4.3 Partitioning Strategy

```
┌────────────────────────────────────────────┐
│         Kafka Topic: raw-price-data        │
├────────────────────────────────────────────┤
│                                            │
│  Partition 0: BINANCE                      │
│  ├─ ETHUSDT events (sub-partitioned)       │
│  └─ BTCUSDT events (sub-partitioned)       │
│                                            │
│  Partition 1: HUOBI                        │
│  ├─ ETHUSDT events (sub-partitioned)       │
│  └─ BTCUSDT events (sub-partitioned)       │
│                                            │
└────────────────────────────────────────────┘
```

**Custom Partitioner Implementation:**
```java
/**
 * Custom partitioner that routes messages based on provider source.
 * Ensures all messages from a provider go to the same partition.
 */
public class ProviderSourcePartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes,
                         Cluster cluster) {

        String compositeKey = (String) key;
        String providerSource = compositeKey.split(":")[0];

        // Map provider to partition
        return switch (providerSource) {
            case "BINANCE" -> 0;
            case "HUOBI" -> 1;
            default -> throw new IllegalArgumentException("Unknown provider: " + providerSource);
        };
    }
}
```

### 5. Connector Factory Pattern

To manage multiple connectors, implement a **Factory Pattern**:

```java
/**
 * Factory for creating source connectors.
 * Implements the Factory Pattern for connector instantiation.
 */
public class SourceConnectorFactory {

    private final Map<ProviderSource, SourceConnector> connectorCache;
    private final ConnectorConfiguration config;

    /**
     * Creates or retrieves a cached connector for the given provider.
     * @param source Provider source
     * @return Initialized connector instance
     */
    public SourceConnector getConnector(ProviderSource source) {
        return connectorCache.computeIfAbsent(source, this::createConnector);
    }

    /**
     * Creates a new connector instance based on the provider source.
     */
    private SourceConnector createConnector(ProviderSource source) {
        return switch (source) {
            case BINANCE -> new BinanceConnector(config.getBinanceConfig());
            case HUOBI -> new HuobiConnector(config.getHuobiConfig());
        };
    }

    /**
     * Returns all active connectors.
     */
    public List<SourceConnector> getAllConnectors() {
        return new ArrayList<>(connectorCache.values());
    }

    /**
     * Disconnects all connectors and clears cache.
     */
    public void shutdown() {
        connectorCache.values().forEach(SourceConnector::disconnect);
        connectorCache.clear();
    }
}
```

### 6. Error Handling and Retry Mechanism

#### 6.1 Retry Strategy

**Exponential Backoff with Jitter:**
```
Attempt 1: 1 second delay
Attempt 2: 2 seconds delay (2^1)
Attempt 3: 4 seconds delay (2^2)
Max backoff: 30 seconds
Jitter: ±20% random variance to prevent thundering herd
```

#### 6.2 Fault Scenarios

| Scenario | Handling Strategy |
|----------|------------------|
| Kafka broker down | Retry with exponential backoff (max 3 attempts) |
| Exchange API timeout | Circuit breaker pattern: fail fast after 3 consecutive failures |
| Invalid data response | Log error, skip message, continue processing |
| Network partition | Switch to backup Kafka broker from cluster |
| Rate limiting (429) | Adaptive backoff based on Retry-After header |

#### 6.3 Circuit Breaker Pattern

```java
/**
 * Circuit breaker state machine for connector resilience
 */
public class ConnectorCircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private State state = State.CLOSED;
    private int failureCount = 0;
    private int failureThreshold = 3;
    private long openStateTimeout = 60000; // 60 seconds

    /**
     * Executes the operation with circuit breaker protection.
     */
    public <T> T execute(Supplier<T> operation) throws CircuitBreakerOpenException {
        if (state == State.OPEN) {
            if (shouldAttemptReset()) {
                state = State.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN");
            }
        }

        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        failureCount = 0;
        state = State.CLOSED;
    }

    private void onFailure() {
        failureCount++;
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
}
```

### 7. Data Transformation Pipeline

Each connector includes a transformation layer to normalize data from different APIs:

```
Exchange API Response (Raw)
         ↓
[Connector-Specific Transformer]
         ↓
Normalized PriceData Object
         ↓
[Validation Layer]
         ↓
Kafka Message (PriceEventDto)
         ↓
[Serialization + Key Generation]
         ↓
Kafka Topic (inbound-data-{provider})
```

**Transformer Interface:**
```java
/**
 * Transforms raw API responses to normalized price data.
 */
public interface PriceDataTransformer {

    /**
     * Transforms raw response to normalized format.
     * @param rawResponse Raw API response
     * @return Normalized price data
     */
    PriceData transform(String rawResponse) throws TransformationException;

    /**
     * Validates the transformed data.
     * @param priceData Data to validate
     * @return true if valid, false otherwise
     */
    boolean validate(PriceData priceData);
}
```

### 8. Monitoring and Observability

#### 8.1 Connector Metrics

Each connector should expose the following metrics:

| Metric Name | Type | Description |
|-------------|------|-------------|
| `connector.fetch.success` | Counter | Successful data fetches |
| `connector.fetch.failure` | Counter | Failed data fetches |
| `connector.fetch.duration` | Histogram | Time to fetch data (ms) |
| `connector.kafka.publish.success` | Counter | Successful Kafka publishes |
| `connector.kafka.publish.failure` | Counter | Failed Kafka publishes |
| `connector.retry.attempts` | Counter | Number of retry attempts |
| `connector.circuit.state` | Gauge | Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN) |

#### 8.2 Health Check Endpoint

```java
/**
 * Health check aggregator for all connectors.
 */
public class ConnectorHealthCheck {

    private final SourceConnectorFactory factory;

    /**
     * Returns health status of all connectors.
     */
    public HealthStatus checkAll() {
        Map<ProviderSource, Boolean> statuses = new HashMap<>();

        for (SourceConnector connector : factory.getAllConnectors()) {
            boolean healthy = connector.healthCheck();
            statuses.put(connector.getProviderSource(), healthy);
        }

        return new HealthStatus(statuses);
    }
}
```

### 9. Configuration Management

**application.yaml Configuration Structure:**
```yaml
source:
  connectors:
    binance:
      enabled: true
      api-url: ${BINANCE_API_URL}
      api-key: ${BINANCE_API_KEY}
      timeout-ms: 5000
      retry:
        max-attempts: 3
        initial-backoff-ms: 1000
        max-backoff-ms: 30000
        backoff-multiplier: 2.0
    huobi:
      enabled: true
      api-url: ${HUOBI_API_URL}
      api-key: ${HUOBI_API_KEY}
      timeout-ms: 5000
      retry:
        max-attempts: 3
        initial-backoff-ms: 1000
        max-backoff-ms: 30000
        backoff-multiplier: 2.0

kafka:
  producer:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    acks: all  # Ensure message durability
    retries: 3
    compression-type: lz4
    batch-size: 16384
    linger-ms: 100
    partitioner:
      class: org.trading.infrastructure.kafka.ProviderSourcePartitioner
```

### 10. Integration with Existing System

#### 10.1 Scheduler Integration

Update `PriceAggregationScheduler` to use the connector layer:

```java
/**
 * Updated scheduler that uses SourceConnector abstraction.
 */
@Scheduled(fixedRateString = "${scheduler.fixed-rate}")
public void aggregatePrices() {
    List<SourceConnector> connectors = connectorFactory.getAllConnectors();

    for (SourceConnector connector : connectors) {
        try {
            // Fetch data through connector
            PriceDataResponse response = connector.fetchPriceData(supportedSymbols);

            // Transform to Kafka message
            PriceEventDto event = transformer.toKafkaEvent(response);

            // Generate composite key
            String key = generateKey(
                connector.getProviderSource(),
                event.getSymbol(),
                event.getTimestamp()
            );

            // Publish to Kafka
            kafkaProducer.send(connector.getProviderSource().getTopicName(), key, event);

        } catch (Exception e) {
            logger.error("Failed to process data from {}",
                connector.getProviderSource(), e);
            // Retry mechanism handles this
        }
    }
}

private String generateKey(ProviderSource source, String symbol, long timestamp) {
    return String.format("%s:%s:%d", source.name(), symbol, timestamp);
}
```

### 11. Benefits of This Design

1. **Extensibility**: Adding new exchanges requires only implementing `SourceConnector`
2. **Testability**: Each connector can be unit tested in isolation
3. **Fault Tolerance**: Built-in retry and circuit breaker patterns
4. **Performance**: Connection pooling and caching reduce overhead
5. **Maintainability**: Clear separation of concerns
6. **Observability**: Comprehensive metrics and health checks
7. **Consistency**: Uniform key design ensures predictable partitioning

### 12. Future Enhancements

- **Dynamic Connector Loading**: Hot-reload connectors without restarting
- **Rate Limiting**: Per-connector rate limiting to respect API quotas
- **Data Quality Checks**: Statistical anomaly detection on fetched data
- **Multi-Region Support**: Connect to regional API endpoints for lower latency
- **Connector Plugins**: Plugin architecture for community-contributed connectors

## Conclusion

This Source Connector design provides a robust, scalable abstraction layer for integrating multiple cryptocurrency exchanges with the Kafka streaming pipeline. The pattern ensures:

- **Consistent message key design** based on provider source
- **Flexible partitioning strategy** that scales with new providers
- **Built-in fault tolerance** with retry and circuit breaker patterns
- **Easy extensibility** for adding new exchanges
- **Clear separation of concerns** between data fetching and publishing

The design aligns with the architecture defined in `architecture-design.md` while providing a clean interface for Task 1 implementation.

## Kafka Template and Partitioner Usage

### 1. KafkaTemplate Configuration

The Kafka producer is configured through Spring Boot's `KafkaProducerConfig` class located at `src/main/java/org/trading/insfrastructure/configuration/KafkaProducerConfig.java:18`.

#### 1.1 Producer Factory Bean

The `ProducerFactory` bean creates Kafka producers with optimized settings:

```java
@Bean
public ProducerFactory<String, PriceEventMessage> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    // Bootstrap servers (supports multiple brokers for HA)
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // Serializers
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

    // Reliability settings
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // Wait for all replicas
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // Prevent duplicates

    // Retry mechanism
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

    // Timeout settings
    configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);  // 60 seconds
    configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);  // 2 minutes

    // Performance tuning
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 16KB batches
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 100);  // Wait 100ms to batch
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");  // Fast compression
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);  // 32MB buffer

    return new DefaultKafkaProducerFactory<>(configProps);
}
```

**Key Configuration Highlights:**

| Setting | Value | Purpose |
|---------|-------|---------|
| `acks` | `all` | Ensures message durability by waiting for all in-sync replicas |
| `enable-idempotence` | `true` | Prevents duplicate messages in case of retries |
| `retries` | `3` | Automatic retry on transient failures |
| `compression-type` | `lz4` | Fast compression for better throughput |
| `batch-size` | `16384` | Batches messages up to 16KB for efficiency |
| `linger-ms` | `100` | Waits 100ms to accumulate more messages in a batch |

#### 1.2 KafkaTemplate Bean

```java
@Bean
public KafkaTemplate<String, PriceEventMessage> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

The `KafkaTemplate` provides a high-level abstraction for sending messages to Kafka topics with type safety and Spring integration.

### 2. Using KafkaTemplate in PriceEventProducerService

The `PriceEventProducerService` demonstrates how to use `KafkaTemplate` for publishing price events.

**Location:** `src/main/java/org/trading/application/service/PriceEventProducerService.java:30`

#### 2.1 Basic Message Publishing Pattern

```java
@Service
@RequiredArgsConstructor
public class PriceEventProducerService {

    private final KafkaTemplate<String, PriceEventMessage> kafkaTemplate;
    private final Validator validator;

    @Value("${kafka.topics.binance}")
    private String binanceTopic;

    @Value("${kafka.topics.huobi}")
    private String huobiTopic;

    public void publishPriceEvent(PriceEventMessage event) {
        // 1. Validate event
        validateEvent(event);

        // 2. Set metadata
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(System.currentTimeMillis());

        // 3. Determine topic based on source
        String topic = getTopicBySource(event.getSource());

        // 4. Create partition key (ensures same source → same partition)
        String partitionKey = event.getSource();

        // 5. Create ProducerRecord with headers and key
        ProducerRecord<String, PriceEventMessage> record =
            createProducerRecord(topic, partitionKey, event);

        // 6. Send asynchronously
        CompletableFuture<SendResult<String, PriceEventMessage>> future =
            kafkaTemplate.send(record);

        // 7. Handle result
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Published to partition {} at offset {}",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish: {}", ex.getMessage());
                throw new RuntimeException("Kafka publish failed", ex);
            }
        });
    }
}
```

#### 2.2 Creating ProducerRecord with Custom Headers

```java
private ProducerRecord<String, PriceEventMessage> createProducerRecord(
    String topic,
    String partitionKey,
    PriceEventMessage event
) {
    // Add metadata headers for filtering and routing
    List<Header> headers = createHeaders(event);

    return new ProducerRecord<>(
        topic,          // Topic name
        null,           // Partition (determined by partitioner)
        partitionKey,   // Key for partitioning
        event,          // Message payload
        headers         // Custom headers
    );
}

private List<Header> createHeaders(PriceEventMessage event) {
    List<Header> headers = new ArrayList<>();
    headers.add(new RecordHeader("symbol",
        event.getSymbol().getBytes(StandardCharsets.UTF_8)));
    headers.add(new RecordHeader("source",
        event.getSource().getBytes(StandardCharsets.UTF_8)));
    headers.add(new RecordHeader("eventId",
        event.getEventId().getBytes(StandardCharsets.UTF_8)));
    return headers;
}
```

**Header Usage Benefits:**
- **Filtering**: Consumers can filter messages without deserializing the payload
- **Routing**: Downstream processors can route based on headers
- **Tracing**: Event IDs enable distributed tracing
- **Sub-partitioning**: Symbol header allows logical sub-partitioning within partitions

#### 2.3 Topic Selection Strategy

```java
private String getTopicBySource(String source) {
    return switch (source.toUpperCase()) {
        case "BINANCE" -> binanceTopic;  // "inbound-data-binance"
        case "HUOBI" -> huobiTopic;      // "inbound-data-huobi"
        default -> throw new IllegalArgumentException("Unknown source: " + source);
    };
}
```

This approach:
- Maintains separate topics per exchange for isolation
- Allows different consumer groups per exchange
- Enables independent scaling of Binance vs Huobi processing

### 3. Custom Partitioner Implementation

The `ProviderSourcePartitioner` implements a custom partitioning strategy to ensure all messages from the same provider go to the same partition.

**Location:** `src/main/java/org/trading/application/service/ProviderSourcePartitioner.java:13`

#### 3.1 Partitioner Interface

```java
@Slf4j
public class ProviderSourcePartitioner implements PartitionStrategy {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes,
                         Cluster cluster) {

        String compositeKey = (String) key;
        String providerSource = compositeKey.split(":")[0];

        log.info("Partitioning message with key: {}", compositeKey);

        // Map provider to partition
        return switch (providerSource.toUpperCase()) {
            case "BINANCE" -> 0;
            case "HUOBI" -> 1;
            default -> throw new IllegalArgumentException("Unknown provider: " + providerSource);
        };
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // Configuration initialization if needed
    }

    @Override
    public void close() {
        // Cleanup resources if needed
    }
}
```

#### 3.2 Partition Assignment Strategy

```
Message Flow with Partitioner:
┌──────────────────────────────────────────────────────────────┐
│ PriceEventProducerService                                    │
│   publishPriceEvent(event)                                   │
│     ↓                                                         │
│   partitionKey = event.getSource()  // "BINANCE" or "HUOBI" │
│     ↓                                                         │
│   ProducerRecord(topic, partitionKey, event)                 │
└────────────────────────┬─────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────────┐
│ ProviderSourcePartitioner.partition()                      │
│   Input: key = "BINANCE"                                   │
│   Output: partition = 0                                    │
│                                                             │
│   Input: key = "HUOBI"                                     │
│   Output: partition = 1                                    │
└────────────────────────┬───────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────────┐
│ Kafka Topic: inbound-data-binance                          │
│                                                             │
│   Partition 0: [BINANCE messages]                          │
│     - ETHUSDT events ordered by timestamp                  │
│     - BTCUSDT events ordered by timestamp                  │
│                                                             │
│   Partition 1: [HUOBI messages]                            │
│     - ETHUSDT events ordered by timestamp                  │
│     - BTCUSDT events ordered by timestamp                  │
└────────────────────────────────────────────────────────────┘
```

#### 3.3 Enabling Custom Partitioner in Configuration

**In application.yaml:**
```yaml
kafka:
  producer:
    # Custom partitioner class
    partitioner:
      class: org.trading.application.service.ProviderSourcePartitioner
```

**Or programmatically in KafkaProducerConfig:**
```java
@Bean
public ProducerFactory<String, PriceEventMessage> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    // ... other configs ...

    // Set custom partitioner
    configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,
        ProviderSourcePartitioner.class);

    return new DefaultKafkaProducerFactory<>(configProps);
}
```

### 4. Partitioning Guarantees and Trade-offs

#### 4.1 Ordering Guarantees

**Within a Partition:**
- ✅ Messages are **strictly ordered** by offset
- ✅ All BINANCE messages maintain temporal order in partition 0
- ✅ All HUOBI messages maintain temporal order in partition 1

**Across Partitions:**
- ❌ No ordering guarantee between BINANCE and HUOBI events
- This is acceptable because price events from different sources are independent

#### 4.2 Scalability Considerations

**Current Design (2 partitions):**
```
Partition 0: BINANCE  → Can have 1 consumer
Partition 1: HUOBI    → Can have 1 consumer
Maximum parallelism: 2 concurrent consumers
```

**Future Expansion (sub-partition by symbol):**
```
Partition 0: BINANCE + ETHUSDT
Partition 1: BINANCE + BTCUSDT
Partition 2: HUOBI + ETHUSDT
Partition 3: HUOBI + BTCUSDT
Maximum parallelism: 4 concurrent consumers
```

To implement symbol-based sub-partitioning:
```java
@Override
public int partition(String topic, Object key, byte[] keyBytes,
                     Object value, byte[] valueBytes,
                     Cluster cluster) {

    String[] keyParts = ((String) key).split(":");
    String provider = keyParts[0];
    String symbol = keyParts[1];  // "ETHUSDT" or "BTCUSDT"

    // Hash-based partitioning for symbol
    int symbolHash = Math.abs(symbol.hashCode());
    int numPartitions = cluster.partitionCountForTopic(topic);

    // First partition by provider, then by symbol
    int providerOffset = provider.equals("BINANCE") ? 0 : 2;
    int symbolPartition = symbolHash % 2;  // 0 or 1

    return providerOffset + symbolPartition;
}
```

### 5. Best Practices for KafkaTemplate Usage

#### 5.1 Error Handling Patterns

**Synchronous Send (blocking):**
```java
try {
    SendResult<String, PriceEventMessage> result =
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
    log.info("Sent to partition {} at offset {}",
        result.getRecordMetadata().partition(),
        result.getRecordMetadata().offset());
} catch (InterruptedException | ExecutionException | TimeoutException e) {
    log.error("Failed to send message", e);
    throw new KafkaPublishException("Send failed", e);
}
```

**Asynchronous Send with Callback (non-blocking):**
```java
kafkaTemplate.send(record).whenComplete((result, ex) -> {
    if (ex == null) {
        log.debug("Success: partition={}, offset={}",
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    } else {
        log.error("Failed: {}", ex.getMessage());
        // Handle failure (e.g., dead letter queue, retry)
    }
});
```

#### 5.2 Retry Mechanism with Spring Retry

```java
@Retryable(
    value = {KafkaException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2.0)
)
public void publishPriceEvent(PriceEventMessage event) {
    // Publishing logic
}
```

**Retry Behavior:**
```
Attempt 1: Immediate
Attempt 2: 1 second delay
Attempt 3: 2 seconds delay
After 3 failures: throw exception
```

#### 5.3 Transaction Support (Optional)

For exactly-once semantics:
```java
@Bean
public KafkaTransactionManager<String, PriceEventMessage> kafkaTransactionManager() {
    return new KafkaTransactionManager<>(producerFactory());
}

@Transactional("kafkaTransactionManager")
public void publishMultipleEvents(List<PriceEventMessage> events) {
    events.forEach(this::publishPriceEvent);
    // All succeed or all fail atomically
}
```

### 6. Monitoring and Metrics

#### 6.1 Key Metrics to Track

```java
@Component
public class KafkaProducerMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    public void recordPublishSuccess(String topic, String source) {
        meterRegistry.counter("kafka.publish.success",
            "topic", topic,
            "source", source
        ).increment();
    }

    public void recordPublishFailure(String topic, String source) {
        meterRegistry.counter("kafka.publish.failure",
            "topic", topic,
            "source", source
        ).increment();
    }

    public void recordPublishLatency(String topic, long latencyMs) {
        meterRegistry.timer("kafka.publish.latency",
            "topic", topic
        ).record(latencyMs, TimeUnit.MILLISECONDS);
    }
}
```

#### 6.2 Producer Metrics from Kafka

Built-in Kafka producer metrics:
- `record-send-rate`: Messages sent per second
- `record-error-rate`: Failed sends per second
- `record-retry-rate`: Retry attempts per second
- `request-latency-avg`: Average request latency
- `batch-size-avg`: Average batch size

### 7. Configuration Reference

**Complete application.yaml for Kafka:**
```yaml
kafka:
  bootstrap-servers: localhost:9092,localhost:9094

  producer:
    # Serialization
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

    # Reliability
    acks: all                          # Wait for all replicas
    enable-idempotence: true           # Prevent duplicates

    # Retry mechanism
    retries: 3                         # Retry up to 3 times
    retry-backoff-ms: 1000             # Wait 1 second between retries
    max-in-flight-requests-per-connection: 5

    # Performance
    batch-size: 16384                  # 16KB batch size
    linger-ms: 100                     # Wait 100ms for batching
    compression-type: lz4              # Fast compression

    # Custom partitioner
    partitioner:
      class: org.trading.application.service.ProviderSourcePartitioner

  topics:
    binance: inbound-data-binance
    huobi: inbound-data-huobi
```

### 8. Testing Kafka Integration

#### 8.1 Unit Testing with Embedded Kafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 2, topics = {"inbound-data-binance", "inbound-data-huobi"})
class PriceEventProducerServiceTest {

    @Autowired
    private PriceEventProducerService producerService;

    @Autowired
    private KafkaTemplate<String, PriceEventMessage> kafkaTemplate;

    @Test
    void shouldPublishToBinancePartition() {
        PriceEventMessage event = PriceEventMessage.builder()
            .source("BINANCE")
            .symbol("ETHUSDT")
            .price(new BigDecimal("2000.00"))
            .build();

        producerService.publishPriceEvent(event);

        // Verify partition assignment
        // Assert on consumer records
    }
}
```

#### 8.2 Integration Testing

```java
@Test
void shouldMaintainOrderingWithinPartition() {
    List<PriceEventMessage> events = List.of(
        createEvent("BINANCE", "ETHUSDT", "2000.00", 1000L),
        createEvent("BINANCE", "ETHUSDT", "2001.00", 1001L),
        createEvent("BINANCE", "ETHUSDT", "2002.00", 1002L)
    );

    events.forEach(producerService::publishPriceEvent);

    // Consume from partition 0 and verify order
    List<ConsumerRecord<String, PriceEventMessage>> records =
        consumeFromPartition("inbound-data-binance", 0);

    assertThat(records).extracting(r -> r.value().getTimestamp())
        .containsExactly(1000L, 1001L, 1002L);
}
```

### 9. Common Pitfalls and Solutions

| Pitfall | Issue | Solution |
|---------|-------|----------|
| **Lost Messages** | Producer crashes before ack | Use `acks=all` and `enable.idempotence=true` |
| **Duplicate Messages** | Network retry creates duplicates | Enable idempotence in producer config |
| **Unbalanced Partitions** | All messages go to one partition | Ensure partition key has good distribution |
| **Slow Consumers** | Consumer can't keep up | Increase partitions and consumer instances |
| **Out-of-Order Messages** | Cross-partition ordering | Accept it or use single partition (reduces throughput) |

### 10. Summary

The Kafka integration in this trading demo provides:

✅ **Reliable Message Delivery**
- `acks=all` ensures durability
- Idempotent producer prevents duplicates
- Automatic retries handle transient failures

✅ **Efficient Partitioning**
- Custom partitioner routes by provider source
- Maintains ordering within provider streams
- Enables parallel processing per provider

✅ **High Performance**
- Batching and compression reduce network overhead
- Asynchronous sends prevent blocking
- Connection pooling reduces latency

✅ **Observability**
- Custom headers enable filtering and tracing
- Built-in metrics track producer health
- Logging provides debugging insights

