# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a cryptocurrency trading demo application supporting multiple cryptocurrency pairs (BTCUSDT, ETHUSDT, SOLUSDT, ADAUSDT). It aggregates K-line data and market prices from Binance and Huobi exchanges, streams data through Kafka, and provides market data endpoints.

### Supported Symbols
- BTCUSDT (Bitcoin/USDT)
- ETHUSDT (Ethereum/USDT)
- SOLUSDT (Solana/USDT)
- ADAUSDT (Cardano/USDT)

## Build and Run Commands

### Initial Setup (macOS only)
Before first run, compile the native CPU metrics library:
```bash
./gradlew compileNativeLibrary
```

This compiles `src/main/c/metricscpu.c` into a native library used for thread monitoring.

### Run Application
```bash
./gradlew bootRun
```

The application starts on port 11080 with context path `/api`.

### Run Tests
```bash
./gradlew test
```

### Build
```bash
./gradlew build
```

## Configuration Summary

### Server
- **Port**: 11080
- **Context Path**: /api

### Kafka
- **Bootstrap Servers**: localhost:9092, localhost:9094
- **Topics**:
  - `ingestion-source` (source data ingestion)
- **Producer Settings**:
  - Idempotent producer enabled
  - Acknowledgment: all
  - Compression: lz4
  - Retries: 3 with 1s backoff

### Data Sources

#### Binance API
- **Book Ticker**: https://api.binance.com/api/v3/ticker/bookTicker
- **K-Line Data**: https://api.binance.com/api/v3/klines
- **Ticker**: https://api.binance.com/api/v3/ticker/
- **Depth**: https://api.binance.com/api/v3/depth

#### Huobi API
- **Tickers**: https://api.huobi.pro/market/tickers

### Scheduler
- **Fixed Rate**: 5000000ms (83.3 minutes) for price aggregation - deprecated feature
- **Spring Batch**: Planned migration to Spring Batch for dynamic, configurable scheduling and job management
  - Enables runtime job configuration without application restart
  - Supports complex job orchestration and workflow management
  - Provides built-in monitoring, logging, and restart capabilities
  - Allows for parallel processing and partitioning of data aggregation tasks

### Logging
- Root level: INFO
- Application package: INFO



