# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a cryptocurrency trading demo application supporting ETHUSDT and BTCUSDT pairs. It aggregates prices from Binance and Huobi exchanges, stores the best prices, and allows users to execute trades against their wallet balances.

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

The application starts on port 8080 with context path `/api`.

### Run Tests
```bash
./gradlew test
```

### Build
```bash
./gradlew build
```

## Architecture

### Layered Architecture Pattern

The codebase follows a clean architecture with distinct layers:

- **Presentation Layer** (`org.trading.presentation`): REST controllers, request/response DTOs, schedulers
- **Application Layer** (`org.trading.application`): Commands and queries (CQRS-style), data transformers/ports
- **Domain Layer** (`org.trading.domain`): Business logic services, aggregates, validations, enumerations
- **Infrastructure Layer** (`org.trading.insfrastructure`): JPA entities, repositories, external API mappers, configuration

### Key Architectural Patterns

**Command Pattern**: Commands in `application/command/` encapsulate operations:
- `AggregatedPriceCommand`: Fetches prices from external sources (Binance/Huobi)
- `AggregatedPriceStoreCommand`: Stores aggregated prices to database
- `TradeCommand`: Executes user trades

**Transformer/Adapter Pattern**: `application/port/` contains transformers for external data:
- `BinanceDataTransformer` and `HuobiDataTransformer` convert external API responses to domain models
- `BestAggregatedPriceTransformer` selects best prices from multiple sources

**Service Layer Pattern**: Domain services in `domain/logic/impl/` implement business rules:
- Transaction isolation and concurrent trade execution
- Wallet balance validations
- Price aggregation logic

### Critical Components

**Price Aggregation Scheduler** (`PriceAggregationScheduler.java:22`):
- Runs every 10 seconds (configurable via `scheduler.fixed-rate` in application.yaml)
- Fetches from both Binance and Huobi
- Merges and stores best prices

**Transaction Execution** (`TransactionExecutionService.java:24`):
- Uses `@Transactional(propagation = REQUIRES_NEW, isolation = SERIALIZABLE)` for trade execution
- Critical: Each trade creates a new transaction with SERIALIZABLE isolation to prevent concurrent modification issues
- Validates balances before executing BUY/SELL operations

**Custom Tomcat Configuration** (`CustomTomcatConfig.java`):
- Custom thread pool executor with dynamic thread naming
- Thread names formatted as `{threadId}/{cpuCore}` using native library
- Enables context-aware request tracking

### Native Integration

The application loads a custom native library (`libmetricscpu.dylib`) for CPU monitoring:
- Source: `src/main/c/metricscpu.c`
- JNI integration: `CoreInfo.java:15`
- Gradle task `compileNativeLibrary` compiles for macOS/Linux/Windows

## Database

- **Database**: H2 in-memory database
- **Schema location**: `src/main/resources/db/migration/schema.sql`
- **Initial data**: `src/main/resources/db/migration/import.sql`
- **JPA**: Hibernate with manual schema management (`ddl-auto: none`)

Users start with 50,000 USDT balance (configured in import.sql).

## API Endpoints

All endpoints are prefixed with `/api`:

- `POST /api/trade` - Execute a trade (BUY/SELL)
- `GET /api/price/best/{symbol}` - Get latest best aggregated price for a symbol
- `GET /api/account/balance/{username}` - Get user wallet balances
- `GET /api/transactions/history/{username}` - Get user transaction history

## Configuration

Key configurations in `application.yaml`:

- External API URLs: `source.binance.url` and `source.huobi.url`
- Scheduler interval: `scheduler.fixed-rate` (default 10000ms)
- Database connection: `spring.datasource.*`
- Context path: `server.servlet.context-path: /api`



## Supported Trading Pairs

Only two pairs are supported:
- ETHUSDT (Ethereum/USDT)
- BTCUSDT (Bitcoin/USDT)

Symbol validation is enforced in `SymbolValidationImpl`.


