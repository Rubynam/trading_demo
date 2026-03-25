# Design Pattern Documentation

## Architecture Overview

This document extracts the key architectural layers, their modules, descriptions, and implementation rules from the trading demo application.

---

## Layer Architecture Summary

| Layer Name | Sub-Modules | Description | Rules for Implementation |
|------------|-------------|-------------|--------------------------|
| **Presentation Layer** (`org.trading.presentation`) | - Controllers<br>- Schedulers<br>- Batch Launchers<br>- Exception Handlers<br>- Request/Response DTOs | Handles all external interactions including HTTP endpoints, scheduled tasks, batch job execution, and global exception handling. Acts as the entry point for all user and system-triggered operations. | **Rules:**<br>1. Must not contain business logic<br>2. Only responsible for request/response transformation<br>3. Exception handling must be centralized (GlobalExceptionHandler)<br>4. Schedulers should trigger application services, not implement logic<br>5. All DTOs must be serializable for API responses<br>6. Use `@RestController`, `@Scheduled`, or batch launchers for entry points |
| **Application Layer** (`org.trading.application`) | - Commands (Command Pattern)<br>- Ports/Adapters (Transformer interfaces)<br>- Services (SourceConnector, PartitionStrategy)<br>- DTOs (Data Transfer Objects)<br>- Connectors (BinanceSourceConnector, HoubiSourceConnector) | Orchestrates use cases and application workflows. Defines ports (interfaces) for external adapters. Implements command pattern for executable operations and factory pattern for service creation. Contains application-specific transformers between domain and infrastructure. | **Rules:**<br>1. Define ports as interfaces without implementation details<br>2. Use Command pattern for all executable operations<br>3. Transformers must be bidirectional (I -> O, O -> I)<br>4. Factory classes must cache instances using ConcurrentHashMap<br>5. Services should orchestrate domain logic, not implement it<br>6. DTOs should be immutable where possible (`@Builder`, `@Getter`)<br>7. No direct dependency on infrastructure implementations |
| **Domain Layer** (`org.trading.domain`) | - Business Logic Interfaces (AggregationService, KLineAggregationService, TickerAggregationService)<br>- Logic Implementations (impl/)<br>- Aggregates (AggregationPrice, KLineData, TickerData)<br>- Domain Models (KLineParameters)<br>- Enumerations (AggregatedSource, Interval, TradeSide, etc.) | Contains pure business logic and domain models. Completely independent of frameworks and external systems. Defines core business rules and domain entities. Aggregates represent business concepts with their invariants. | **Rules:**<br>1. **NO framework dependencies** (no Spring annotations)<br>2. Interfaces define contracts, implementations contain logic<br>3. Aggregates must maintain their own invariants<br>4. Enumerations must represent domain concepts only<br>5. Domain models should be framework-agnostic<br>6. Business logic must be testable without infrastructure<br>7. No direct I/O operations (API calls, database, Kafka)<br>8. Use meaningful domain language (ubiquitous language) |
| **Infrastructure Layer** (`org.trading.insfrastructure`) | - Configuration (Spring, Kafka, Batch, Retry, Tomcat)<br>- Mappers (BinanceData, HuobiData, BinanceKLine, BinanceTicker, BinanceDepth)<br>- External System Adapters<br>- Persistence Implementations | Provides technical implementations for external systems. Contains all framework-specific configurations, database access, external API integrations, and messaging infrastructure. Implements adapters for domain ports. | **Rules:**<br>1. All Spring configurations must be in `config/` package<br>2. Mappers must match external API data structures exactly<br>3. Use `@Configuration` for bean definitions<br>4. RestTemplate must be configured with proper timeouts<br>5. Kafka producer must be idempotent with acknowledgment=all<br>6. Retry logic should use `@Retryable` with exponential backoff<br>7. Compression must be enabled (lz4) for Kafka<br>8. All external calls must have error handling |

---

## Design Patterns by Category

### Structural Patterns

| Pattern Name | Location | Components | Purpose | Implementation Rules |
|--------------|----------|------------|---------|---------------------|
| **Adapter/Port Pattern** | `org.trading.application.port` | - `Transformer<I,O>` interface<br>- BinanceDataTransformer<br>- HuobiDataTransformer<br>- KafkaEventTransformer<br>- KLineBinanceTransformer<br>- TickerBinanceTransformer | Convert between different data representations (external APIs, domain models, Kafka messages) to isolate domain from external formats | 1. Use generic `Transformer<I,O>` interface<br>2. Implement bidirectional transformation<br>3. Keep transformers stateless<br>4. Place in application.port package<br>5. No business logic in transformers |
| **Strategy Pattern** | `org.trading.application.service` | - `PartitionStrategy` interface<br>- ProviderSourcePartitioner implementation | Encapsulate Kafka partitioning algorithm to allow runtime selection of partitioning strategy | 1. Define strategy interface with single method<br>2. Implementations must be stateless<br>3. Inject strategy via constructor<br>4. Allow multiple strategies to coexist |

### Creational Patterns

| Pattern Name | Location | Components | Purpose | Implementation Rules |
|--------------|----------|------------|---------|---------------------|
| **Factory Pattern** | `org.trading.application.command` | - SourceConnectorFactory<br>- ConcurrentHashMap cache<br>- ApplicationContext lookup | Create and cache source connector instances (BinanceSourceConnector, HoubiSourceConnector) with lazy initialization | 1. Use ConcurrentHashMap for thread-safe caching<br>2. Implement lazy initialization with `computeIfAbsent()`<br>3. Lookup beans from ApplicationContext<br>4. Factory must be a Spring component (`@Component`)<br>5. Return cached instances for same source type |

### Behavioral Patterns

| Pattern Name | Location | Components | Purpose | Implementation Rules |
|--------------|----------|------------|---------|---------------------|
| **Command Pattern** | `org.trading.application.command` | - `Command<I,O>` interface<br>- `execute(I input)` method | Encapsulate command execution logic as objects to enable parameterized execution and queuing | 1. Define generic `Command<I,O>` interface<br>2. Single method: `O execute(I input)`<br>3. Commands should be immutable<br>4. Use for all executable operations<br>5. Enable retry and logging via AOP |
| **Template Method Pattern** | `org.trading.application.service` | - SourceConnector interface<br>- Default methods: `createHeaders()`, `createProducerRecord()` | Define skeleton of algorithm in base with variations in subclasses for Kafka message creation | 1. Use Java default methods for template<br>2. Abstract methods for variation points<br>3. Common logic in default methods<br>4. Subclasses override only what changes<br>5. Headers creation is customizable per connector |

---

## Architectural Patterns

| Pattern Name | Scope | Description | Implementation Rules |
|--------------|-------|-------------|---------------------|
| **Hexagonal Architecture (Ports & Adapters)** | Entire Application | Application core (domain + application layers) is isolated from external concerns through ports (interfaces) and adapters (implementations). Domain layer has no knowledge of frameworks or infrastructure. | 1. Domain layer has zero external dependencies<br>2. Application layer defines ports as interfaces<br>3. Infrastructure implements adapters for ports<br>4. Dependencies point inward (infrastructure -> application -> domain)<br>5. Use dependency injection for all cross-layer communication<br>6. No direct imports from outer layers to inner layers |
| **Repository/Service Pattern** | Domain & Application Layers | Separate business logic (domain services) from data access (repositories). Services orchestrate operations and enforce business rules. | 1. Domain services contain business logic<br>2. Application services orchestrate use cases<br>3. Services must be stateless<br>4. Use constructor injection for dependencies<br>5. Annotate with `@Service` in application/infrastructure<br>6. No annotations in domain layer |
| **Event-Driven Architecture** | Cross-Layer (via Kafka) | Asynchronous communication between components via Kafka topics. Producers send events without blocking, consumers process independently. | 1. All events go through Kafka topics<br>2. Use custom partitioning strategy<br>3. Events must include metadata headers (symbol, source, eventId)<br>4. Idempotent producers to prevent duplicates<br>5. Compression (lz4) for efficiency<br>6. Retry logic with exponential backoff |

---

## Cross-Cutting Concerns Implementation Rules

| Concern | Implementation | Rules |
|---------|---------------|-------|
| **Dependency Injection** | Spring Framework | 1. Use constructor injection (via `@RequiredArgsConstructor`)<br>2. Prefer immutable dependencies<br>3. No field injection<br>4. All beans in infrastructure/application layers<br>5. Domain layer uses pure constructor injection without annotations |
| **Logging** | SLF4J + Lombok | 1. Use `@Slf4j` annotation<br>2. Log at entry/exit of critical operations<br>3. Include contextual information (symbol, source)<br>4. Error logs must include exception details<br>5. Root level: INFO, Application: INFO |
| **Exception Handling** | Spring AOP + GlobalExceptionHandler | 1. Centralize exception handling in presentation layer<br>2. Use `@ControllerAdvice` for global handler<br>3. Return structured ErrorResponse objects<br>4. Log exceptions before handling<br>5. Don't catch generic Exception unless necessary |
| **Retry Logic** | Spring Retry | 1. Use `@Retryable` annotation on Kafka operations<br>2. Configure exponential backoff (1s initial)<br>3. Maximum 3 retries<br>4. Log each retry attempt<br>5. Use `@Recover` for fallback logic |
| **Configuration** | Externalized (application.yaml) | 1. No hardcoded URLs or values<br>2. All infrastructure config in application.yaml<br>3. Use `@Value` or `@ConfigurationProperties`<br>4. Environment-specific profiles (dev, prod)<br>5. Sensitive data via environment variables |
| **AOP (Aspect-Oriented Programming)** | Spring AOP | 1. Enable with `@EnableAspectJAutoProxy`<br>2. Use for cross-cutting concerns only<br>3. Proxy-based AOP for Spring beans<br>4. Combine with @Retryable for resilience |

---

## Data Flow Architecture

```
External APIs (Binance/Huobi)
         ↓
RestTemplate (Infrastructure)
         ↓
Mappers (BinanceData, HuobiData)
         ↓
Transformers (Application.Port)
         ↓
Domain Aggregates (AggregationPrice, KLineData, TickerData)
         ↓
Domain Services (AggregationService implementations)
         ↓
Application Services (SourceConnector)
         ↓
Kafka Producers (with PartitionStrategy)
         ↓
Kafka Topic (ingestion-source)
```

**Rules:**
1. Data flows in one direction (top to bottom)
2. Each layer transforms data to its representation
3. No layer skipping (must go through all layers)
4. Transformers are the only bridge between representations
5. Domain layer never depends on external data formats

---

## Kafka Integration Rules

| Configuration | Value | Rule |
|---------------|-------|------|
| **Topic** | `ingestion-source` | Single topic for all ingestion data |
| **Partitions** | Source-based (Binance=0, Huobi=1) | Use custom PartitionStrategy implementation |
| **Idempotent Producer** | Enabled | Prevents duplicate messages in case of retries |
| **Acknowledgment** | `all` | All replicas must acknowledge before success |
| **Compression** | `lz4` | Compress all messages for efficiency |
| **Retries** | 3 with 1s backoff | Exponential backoff for transient failures |
| **Headers** | symbol, source, eventId | Every message must include metadata headers |

---

## Architectural Strengths

1. **Clean Separation**: Clear layer boundaries prevent tight coupling
2. **Extensibility**: New transformers, strategies, and connectors can be added without modifying existing code
3. **Testability**: Interface-based design enables easy mocking for unit tests
4. **Resilience**: Retry logic and exception handling across critical operations
5. **Scalability**: Kafka-based event streaming for distributed processing
6. **Framework Independence**: Domain layer is completely framework-agnostic

---

## Recent Architectural Evolution

Based on git commit history:
- **Spring Batch Migration**: Moving from fixed-rate scheduling to Spring Batch for dynamic, configurable job management
- **Multi-Data Type Support**: Added K-line, Ticker, and Depth data aggregation
- **Code Restructuring**: Improved layer separation and pattern adherence
- **Interval Enumeration**: Unified interval handling across different data types

---

## Compliance Checklist for New Code

When adding new features, ensure:

- [ ] Domain logic is in domain layer (no framework dependencies)
- [ ] Ports defined as interfaces in application.port
- [ ] Adapters implemented in infrastructure layer
- [ ] Transformers are bidirectional and stateless
- [ ] Commands implement `Command<I,O>` interface
- [ ] Strategies implement single-method interface
- [ ] Factory pattern used for object creation with caching
- [ ] Kafka messages include required headers (symbol, source, eventId)
- [ ] Retry logic configured for external operations
- [ ] Configuration externalized to application.yaml
- [ ] Exceptions handled in GlobalExceptionHandler
- [ ] Logging with SLF4J (`@Slf4j`)
- [ ] Constructor injection used (`@RequiredArgsConstructor`)
- [ ] No layer skipping in data flow
- [ ] All tests are framework-independent for domain layer

---

**Document Version**: 1.0
**Last Updated**: 2026-03-23
**Author**: Extracted from codebase analysis