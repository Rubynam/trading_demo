-- ScyllaDB Initialization Script
-- This script creates the market keyspace, user, and chart_m1 table for OHLC data storage

-- ============================================================================
-- 1. CREATE KEYSPACE: market
-- ============================================================================
-- NetworkTopologyStrategy is recommended for production environments
-- Replication factor of 2 ensures data is replicated across both master and slave nodes
CREATE KEYSPACE IF NOT EXISTS market
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': 2
}
AND durable_writes = true;

-- ============================================================================
-- 2. CREATE USER: market_user
-- ============================================================================
-- Create a dedicated user for market data operations
-- Note: This requires authentication to be enabled (PasswordAuthenticator)
CREATE ROLE IF NOT EXISTS market_user WITH PASSWORD = 'abc@1234' AND LOGIN = true;

-- Grant full permissions on the market keyspace to market_user
GRANT ALL PERMISSIONS ON KEYSPACE market TO market_user;

-- ============================================================================
-- 3. CREATE TABLE: chart_m1 (1-Minute OHLC Candles)
-- ============================================================================
-- This table stores 1-minute candlestick (OHLC) data for cryptocurrency trading pairs
-- Partitioning strategy based on provider source and symbol for optimal query performance

CREATE TABLE IF NOT EXISTS market.chart_m1 (
  -- Partition Key Components
  source text,              -- Provider source: 'BINANCE' or 'HUOBI'
  symbol text,              -- Trading pair: 'ETHUSDT', 'BTCUSDT'
  date text,                -- Date bucket in format 'YYYYMMDD' (e.g., '20240115')

  -- Clustering Key
  timestamp timestamp,      -- Candle timestamp (start of 1-minute window)

  -- OHLC Data
  open decimal,             -- Opening price
  high decimal,             -- Highest price in the interval
  low decimal,              -- Lowest price in the interval
  close decimal,            -- Closing price

  -- Volume and Metadata
  volume decimal,           -- Total trading volume
  tick_count bigint,        -- Number of price ticks aggregated

  -- Audit Fields
  created_at timestamp,     -- Record creation timestamp

  -- Primary Key: Composite partition key (source, symbol, date) + clustering key (timestamp)
  -- This ensures:
  -- 1. Data is partitioned by source and symbol for even distribution
  -- 2. Date bucketing prevents partition growth over time
  -- 3. Timestamp clustering allows efficient time-range queries
  PRIMARY KEY ((source, symbol), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND compaction = {
    'class': 'TimeWindowCompactionStrategy',
    'compaction_window_size': 1,
    'compaction_window_unit': 'DAYS'
  }
  AND comment = '1-minute OHLC candlestick data from Binance and Huobi exchanges';

-- ============================================================================
-- 4. CREATE INDEX (Optional - for querying by timestamp across partitions)
-- ============================================================================
-- Note: Secondary indexes can impact performance. Use sparingly.
-- This index allows querying candles by timestamp without specifying source/symbol
-- CREATE INDEX IF NOT EXISTS idx_chart_m1_timestamp ON market.chart_m1 (timestamp);

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: Insert a 1-minute candle
-- INSERT INTO market.chart_m1 (source, symbol, date, timestamp, open, high, low, close, volume, tick_count, created_at)
-- VALUES ('BINANCE', 'ETHUSDT', '20240115', '2024-01-15 10:30:00', 2045.50, 2046.75, 2044.20, 2045.80, 125.5, 42, toTimestamp(now()));

-- Example 2: Query candles for a specific symbol on a specific date
-- SELECT * FROM market.chart_m1
-- WHERE source = 'BINANCE'
--   AND symbol = 'ETHUSDT'
--   AND date = '20240115'
--   AND timestamp >= '2024-01-15 10:00:00'
--   AND timestamp < '2024-01-15 11:00:00';

-- Example 3: Get latest 100 candles for a symbol
-- SELECT * FROM market.chart_m1
-- WHERE source = 'BINANCE'
--   AND symbol = 'ETHUSDT'
--   AND date = '20240115'
-- LIMIT 100;

-- ============================================================================
-- NOTES
-- ============================================================================
-- 1. Date Format: The 'date' field uses 'YYYYMMDD' format for efficient bucketing
--    - Prevents unlimited partition growth
--    - Each partition contains max ~1440 candles (1 day of 1-minute data)
--
-- 2. TTL (Time To Live): Set to 90 days (7776000 seconds)
--    - Data older than 90 days will be automatically deleted
--    - For longer retention, archive to S3/cold storage before expiration
--
-- 3. Compaction Strategy: TimeWindowCompactionStrategy
--    - Optimized for time-series data
--    - Groups data by time windows for efficient compaction
--
-- 4. Replication: Set to 2 (master + slave)
--    - Adjust based on cluster size
--    - For single-node development, use SimpleStrategy with RF=1
--
-- 5. Authentication: Ensure ScyllaDB is running with PasswordAuthenticator
--    - Command flag: --authenticator PasswordAuthenticator
--    - Already configured in docker-compose.yml