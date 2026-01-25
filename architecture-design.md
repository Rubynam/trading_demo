# Data Lake Architecture Design for Stock Exchange Historical Data

## Executive Summary

This document outlines the architecture for a scalable data lake system that ingests real-time cryptocurrency price data from Binance and Huobi exchanges, processes it through a streaming pipeline, and generates multi-timeframe candlestick (OHLC) data for historical analysis and charting.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA INGESTION LAYER                             │
├─────────────────────────────────────────────────────────────────────────┤
│  Price Aggregation    │                                                  │
│  Scheduler (10s)      │  External Sources                                │
│         │             │  ┌──────────┐    ┌──────────┐                   │
│         └─────────────┼─>│ Binance  │    │  Huobi   │                   │
│                       │  │   API    │    │   API    │                   │
│                       │  └──────────┘    └──────────┘                   │
└───────────────────────┼──────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STREAMING DATA PIPELINE                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Apache Kafka Cluster                          │   │
│  │                                                                   │   │
│  │  Topic: raw-price-data                                           │   │
│  │  ├─ Partition 0: Binance (all symbols)                          │   │
│  │  └─ Partition 1: Huobi (all symbols)                            │   │
│  │                                                                   │   │
│  │  Topic: aggregated-price-events                                  │   │
│  │  ├─ Best bid/ask prices with source metadata                     │   │
│  │  └─ Retention: 7 days (configurable)                             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
└───────────────────────┬───────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STREAM PROCESSING LAYER                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              Apache Spark Structured Streaming                   │   │
│  │                                                                   │   │
│  │  Job 1: Real-time 1-minute Candle Generator                      │   │
│  │  ├─ Watermarking: 30 seconds late data tolerance                 │   │
│  │  ├─ Window: 1 minute tumbling window                             │   │
│  │  └─ Output: Kafka topic "candles-1m" + ScyllaDB                  │   │
│  │                                                                   │   │
│  │  Job 2: Batch Aggregation (Hourly Schedule)                      │   │
│  │  ├─ Read from: ScyllaDB 1-minute candles                         │   │
│  │  ├─ Aggregate to: 5m, 15m, 30m, 1h, 4h, 1d, 1w timeframes       │   │
│  │  └─ Write to: ScyllaDB OHLC tables                               │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
└───────────────────────┬───────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      DATA PERSISTENCE LAYER                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     ScyllaDB Cluster                             │   │
│  │                  (Cassandra-compatible)                          │   │
│  │                                                                   │   │
│  │  Keyspace: crypto_market_data                                    │   │
│  │  Replication Strategy: NetworkTopologyStrategy                   │   │
│  │  Replication Factor: 3                                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

## Detailed Component Design

### 1. Data Ingestion Layer

**Current Implementation Enhancement:**
- Leverage existing `PriceAggregationScheduler` (10-second intervals)
- Add Kafka Producer to publish raw price data instead of direct DB writes
- Maintain existing `AggregatedPriceCommand` for fetching from Binance/Huobi

**Kafka Producer Configuration:**
```yaml
Producer Settings:
  - Topic: raw-price-data
  - Serialization: JSON (StringSerializer for key, JsonSerializer for value)
  - Partitioning: By provider source (Binance, Huobi)
  - Sub-partitioning: Symbol-based routing within each partition
  - Compression: lz4
  - Batch Size: 16KB
  - Linger Time: 100ms
```

**Message Schema (JSON):**
```json
{
  "symbol": "ETHUSDT",
  "source": "BINANCE",
  "bidPrice": 2045.50,
  "askPrice": 2045.75,
  "bidQty": 10.5,
  "askQty": 8.3,
  "timestamp": 1703001234567,
  "eventId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**JSON Validation (Java Layer):**

Validation is implemented in the Java application layer using Jakarta Bean Validation (JSR-380) before publishing to Kafka:

```java
// DTO for Kafka message
public class PriceEventDto {
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^(ETHUSDT|BTCUSDT)$", message = "Invalid symbol")
    private String symbol;

    @NotNull(message = "Source is required")
    @Pattern(regexp = "^(BINANCE|HUOBI)$", message = "Invalid source")
    private String source;

    @NotNull(message = "Bid price is required")
    @Positive(message = "Bid price must be positive")
    private BigDecimal bidPrice;

    @NotNull(message = "Ask price is required")
    @Positive(message = "Ask price must be positive")
    private BigDecimal askPrice;

    @NotNull(message = "Bid quantity is required")
    @PositiveOrZero(message = "Bid quantity must be non-negative")
    private BigDecimal bidQty;

    @NotNull(message = "Ask quantity is required")
    @PositiveOrZero(message = "Ask quantity must be non-negative")
    private BigDecimal askQty;

    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    private Long timestamp;

    @NotBlank(message = "Event ID is required")
    private String eventId;
}

// Kafka Producer Service with validation
@Service
public class PriceEventProducer {

    private final KafkaTemplate<String, PriceEventDto> kafkaTemplate;
    private final Validator validator;

    public void publishPriceEvent(PriceEventDto event) {
        // Validate before sending
        Set<ConstraintViolation<PriceEventDto>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new ValidationException(
                violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "))
            );
        }

        // Partition key: source (ensures all data from same provider goes to same partition)
        String partitionKey = event.getSource();

        // Send to Kafka
        kafkaTemplate.send("raw-price-data", partitionKey, event);
    }
}
```

### 2. Apache Kafka Architecture

**Cluster Configuration:**
- 3+ broker nodes for fault tolerance
- Zookeeper ensemble (3 nodes) or KRaft mode (Kafka 3.0+)

**Topic Design:**

| Topic Name | Partitions | Replication Factor | Retention | Purpose |
|------------|------------|-------------------|-----------|---------|
| raw-price-data | 2 | 3 | 7 days | Raw price ticks from exchanges |
| aggregated-price-events | 2 | 3 | 7 days | Best bid/ask prices |
| candles-1m | 2 | 3 | 90 days | 1-minute candlesticks |
| candles-aggregated | 2 | 3 | 365 days | Higher timeframe candles |

**Consumer Groups:**
- `spark-streaming-1m-candles`: Real-time 1-minute candle generation
- `spark-batch-aggregator`: Batch processing for higher timeframes
- `analytics-consumers`: For real-time dashboards/alerts

### 3. Apache Spark Processing

#### 3.1 Real-time Streaming Job (1-minute Candles)

**Spark Structured Streaming Configuration:**
```python
# Pseudocode for architecture reference
spark_stream = (
  spark
    .readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "kafka-cluster:9092")
    .option("subscribe", "aggregated-price-events")
    .option("startingOffsets", "latest")
    .load()
)

# Windowing for 1-minute candles
candles_1m = (
  spark_stream
    .withWatermark("timestamp", "30 seconds")  # Handle late arrivals
    .groupBy(
      window("timestamp", "1 minute"),
      "symbol"
    )
    .agg(
      first("bidPrice").alias("open"),
      max("bidPrice").alias("high"),
      min("askPrice").alias("low"),
      last("askPrice").alias("close"),
      sum("bidQty").alias("volume"),
      count("*").alias("tick_count")
    )
)

# Dual sink: Kafka + ScyllaDB
candles_1m.writeStream
  .foreachBatch(write_to_kafka_and_scylla)
  .outputMode("append")
  .trigger(processingTime="10 seconds")
  .start()
```

**Processing Characteristics:**
- Micro-batch interval: 10 seconds
- Watermark: 30 seconds (allows late data)
- Stateful processing with checkpointing to HDFS/S3
- Exactly-once semantics with idempotent writes

#### 3.2 Batch Aggregation Job (Higher Timeframes)

**Schedule:** Hourly (via Apache Airflow or cron)

**Process:**
1. Read 1-minute candles from ScyllaDB for the last hour
2. Aggregate using Spark DataFrame operations
3. Generate 5m, 15m, 30m, 1h, 4h, 1d, 1w candles
4. Write back to ScyllaDB

**Spark SQL Aggregation Logic:**
```sql
-- Example for 5-minute candles from 1-minute data
SELECT
  symbol,
  window(timestamp, '5 minutes') as time_window,
  FIRST(open) as open,
  MAX(high) as high,
  MIN(low) as low,
  LAST(close) as close,
  SUM(volume) as volume,
  SUM(tick_count) as tick_count
FROM candles_1m_table
WHERE timestamp >= current_timestamp - INTERVAL 1 HOUR
GROUP BY symbol, window(timestamp, '5 minutes')
```

**Batch Job Configuration:**
- Executor Memory: 4GB
- Executor Cores: 2
- Dynamic Allocation: Enabled
- Shuffle Partitions: Matches ScyllaDB partition count

### 4. ScyllaDB Data Model

#### 4.1 Keyspace Configuration

```cql
CREATE KEYSPACE IF NOT EXISTS crypto_market_data
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': 3
}
AND durable_writes = true;
```

#### 4.2 Table Schemas

**Raw Price Events Table:**
```cql
CREATE TABLE crypto_market_data.raw_price_events (
  source text,              -- Provider source: 'BINANCE' or 'HUOBI'
  symbol text,              -- Sub-partition by symbol
  timestamp timestamp,
  event_id uuid,
  bid_price decimal,
  ask_price decimal,
  bid_qty decimal,
  ask_qty decimal,
  PRIMARY KEY ((source, symbol), timestamp, event_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, event_id DESC)
  AND compaction = {
    'class': 'TimeWindowCompactionStrategy',
    'compaction_window_size': 1,
    'compaction_window_unit': 'DAYS'
  }
  AND default_time_to_live = 604800;  -- 7 days retention
```

**1-Minute Candles Table:**
```cql
CREATE TABLE crypto_market_data.candles_1m (
  source text,              -- Provider source: 'BINANCE' or 'HUOBI'
  symbol text,              -- Sub-partition by symbol
  omsServerId text,         -- Server ID for OMS routing: 'BINANCE_20231215'
  timestamp timestamp,
  open decimal,
  high decimal,
  low decimal,
  close decimal,
  volume decimal,
  tick_count bigint,
  created_at timestamp,
  PRIMARY KEY ((source, symbol, omsServerId), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND compaction = {
    'class': 'TimeWindowCompactionStrategy',
    'compaction_window_size': 1,
    'compaction_window_unit': 'DAYS'
  }
  AND default_time_to_live = 7776000;  -- 90 days retention
```

**Aggregated Candles Table (Multi-Timeframe):**
```cql
CREATE TABLE crypto_market_data.candles_aggregated (
  source text,              -- Provider source: 'BINANCE' or 'HUOBI'
  symbol text,              -- Sub-partition by symbol
  timeframe text,           -- '5m', '15m', '30m', '1h', '4h', '1d', '1w'
  omsServerId text,         -- Server ID for OMS routing: 'BINANCE_5m_202312'
  timestamp timestamp,
  open decimal,
  high decimal,
  low decimal,
  close decimal,
  volume decimal,
  tick_count bigint,
  created_at timestamp,
  PRIMARY KEY ((source, symbol, timeframe, omsServerId), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND compaction = {
    'class': 'TimeWindowCompactionStrategy',
    'compaction_window_size': 7,
    'compaction_window_unit': 'DAYS'
  }
  AND default_time_to_live = 31536000;  -- 1 year retention
```

**Best Aggregated Price Table (Current):**
```cql
CREATE TABLE crypto_market_data.best_aggregated_price (
  symbol text,
  timestamp timestamp,
  source text,
  bid_price decimal,
  ask_price decimal,
  PRIMARY KEY ((symbol), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 2592000;  -- 30 days retention
```

#### 4.3 Materialized Views for Query Optimization

```cql
-- View for latest candle per source/symbol/timeframe
CREATE MATERIALIZED VIEW crypto_market_data.latest_candles AS
  SELECT source, symbol, timeframe, timestamp, open, high, low, close, volume
  FROM crypto_market_data.candles_aggregated
  WHERE source IS NOT NULL
    AND symbol IS NOT NULL
    AND timeframe IS NOT NULL
    AND omsServerId IS NOT NULL
    AND timestamp IS NOT NULL
  PRIMARY KEY ((source, symbol, timeframe), timestamp)
  WITH CLUSTERING ORDER BY (timestamp DESC);
```

#### 4.4 Partitioning Strategy

**Provider Source-Based Partitioning:**

The partition strategy is based on **provider source** (Binance, Huobi) rather than symbol, which provides better scalability as the number of supported trading pairs grows.

**Rationale:**
- **Scalability:** As new symbols are added, partitions don't multiply exponentially
- **Data Locality:** All data from a single provider is co-located
- **Query Efficiency:** Most queries filter by source first, then symbol
- **Load Distribution:** Distributes load evenly across provider sources

**Primary Partition Key Structure:**
```
PRIMARY KEY ((source, symbol, omsServerId), timestamp)
```

**1-Minute Candles:**
- `omsServerId` format: `{SOURCE}_{DATE}` (e.g., "BINANCE_20231215")
- Prevents large partitions (max ~1440 candles per symbol per day)
- Enables OMS (Order Management System) routing by provider and date

**Aggregated Candles:**
- `omsServerId` format: `{SOURCE}_{TIMEFRAME}_{MONTH}` (e.g., "BINANCE_5m_202312")
- Monthly bucketing for higher timeframes (generates less data)
- Balances partition size and query efficiency

**Partition Size Control:**
- 1-minute candles: ~1440 rows per symbol per day per source
- 5-minute candles: ~288 rows per symbol per day per source
- Daily candles: ~30 rows per symbol per month per source
- Keeps partition sizes well under ScyllaDB's recommended 100MB limit

### 5. Data Pipeline Flow

#### Flow 1: Real-time 1-Minute Candle Generation

```
Price Aggregation Scheduler (10s)
  ↓
Kafka Producer → raw-price-data topic
  ↓
Aggregation Logic → aggregated-price-events topic
  ↓
Spark Structured Streaming (micro-batch: 10s)
  ├─ Window: 1 minute tumbling
  ├─ Compute OHLC
  └─ Watermark: 30s late data
  ↓
Dual Write:
  ├─ Kafka → candles-1m topic (for consumers)
  └─ ScyllaDB → candles_1m table (persistence)
```

#### Flow 2: Batch Aggregation (Hourly)

```
Airflow Scheduler (hourly)
  ↓
Spark Batch Job
  ├─ Read: candles_1m from ScyllaDB (last hour)
  ├─ Aggregate: 5m, 15m, 30m, 1h, 4h, 1d, 1w
  └─ Map/Reduce by timeframe
  ↓
ScyllaDB Write → candles_aggregated table
  ├─ Batch size: 1000 rows
  ├─ Parallelism: 10 partitions
  └─ Write consistency: QUORUM
```

### 6. Scalability and Performance

#### 6.1 Kafka Scalability

- **Throughput:** 10,000 messages/second per partition
- **Storage:** Tiered storage (hot data on SSD, cold on S3)
- **Horizontal Scaling:** Add brokers and increase partitions

#### 6.2 Spark Optimization

**Resource Allocation:**
```yaml
Streaming Job:
  Executors: 3-5
  Executor Memory: 4GB
  Executor Cores: 2
  Driver Memory: 2GB

Batch Job:
  Dynamic Allocation: 5-20 executors
  Executor Memory: 8GB
  Executor Cores: 4
```

**Checkpointing:**
- Location: HDFS/S3 for fault tolerance
- Interval: 10 seconds
- Cleanup: Automatic old checkpoint deletion

#### 6.3 ScyllaDB Performance

**Tuning Parameters:**
- Compaction Strategy: TimeWindowCompactionStrategy (time-series optimized)
- Bloom Filter: Enabled for timestamp queries
- Caching: Row cache disabled, key cache enabled
- Read/Write Consistency: QUORUM for accuracy, LOCAL_QUORUM for speed

**Expected Performance:**
- Write throughput: 100,000+ writes/second per node
- Read latency: <10ms at p99
- Partition size: <100MB (via bucketing strategy)

### 7. Data Retention and Archival

| Data Type | Retention in ScyllaDB | Archive Location | Archive Format |
|-----------|----------------------|------------------|----------------|
| Raw price events | 7 days | S3 Glacier | Parquet (compressed) |
| 1-minute candles | 90 days | S3 Standard | Parquet (partitioned by date) |
| 5m, 15m, 30m candles | 180 days | S3 Standard | Parquet |
| 1h, 4h candles | 1 year | S3 Standard | Parquet |
| 1d, 1w candles | Permanent | ScyllaDB + S3 | Parquet |

**Archival Process (Daily):**
```
Spark Batch Job → Read expired data from ScyllaDB
  ↓
Convert to Parquet → Partition by (symbol, year, month, day)
  ↓
Write to S3 → s3://crypto-data-lake/candles/{timeframe}/{symbol}/{year}/{month}/{day}/
  ↓
Delete from ScyllaDB → TTL-based automatic expiration
```

### 8. Monitoring and Observability

**Metrics Collection:**
- Kafka: JMX metrics (lag, throughput, partition count)
- Spark: Spark UI, custom metrics (processing time, watermark delay)
- ScyllaDB: Prometheus exporter (read/write latency, compaction stats)

### 9. Disaster Recovery
 - DO not implement right now

### 10. Security Considerations
**Audit Logging:**
- Track all data access and modifications
- Retention: 1 year in WORM storage

## Implementation Phases

### Phase 1: Foundation (Weeks 1-2)
- Deploy Kafka cluster (3 brokers)
- Deploy ScyllaDB cluster (3 nodes)
- Create keyspace and tables
- Modify `PriceAggregationScheduler` to publish to Kafka

### Phase 2: Streaming Pipeline (Weeks 3-4)
- Implement Spark Structured Streaming job for 1m candles
- Set up checkpointing and monitoring
- Test with production data
- Validate data quality

### Phase 3: Batch Aggregation (Weeks 5-6)
- Develop Spark batch jobs for higher timeframes
- Set up Apache Airflow for scheduling
- Implement map/reduce logic
- Performance tuning

### Phase 4: Production Hardening (Weeks 7-8)
- Implement archival to S3
- Set up monitoring and alerting
- Disaster recovery testing
- Documentation and runbooks

## Conclusion

This architecture provides:
- **Scalability:** Kafka and ScyllaDB handle millions of events per second
- **Fault Tolerance:** 3x replication, automatic failover
- **Performance:** <1 second end-to-end latency for 1m candles
- **Flexibility:** Easy to add new exchanges, symbols, or timeframes
- **Cost Efficiency:** TTL-based data expiration, S3 archival for cold data

The design leverages industry-standard tools (Kafka, Spark, ScyllaDB) with proven scalability for financial time-series data, while integrating seamlessly with the existing trading application.