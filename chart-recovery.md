# Chart Recovery Design

## Executive Summary

This document outlines the strategy for chart recovery and batch aggregation jobs to generate multi-timeframe candlestick data (2m, 5m, 15m, 1h, 1d, 1w) from the base 1-minute candles stored in ScyllaDB. The design leverages **Apache Airflow** for workflow orchestration, scheduling, and dependency management, ensuring data integrity, efficient recovery from failures, and the ability to backfill historical data.

## Table of Contents
1. [Chart Recovery Overview](#chart-recovery-overview)
2. [Apache Airflow Workflow Architecture](#apache-airflow-workflow-architecture)
3. [Batch Aggregation Job Design](#batch-aggregation-job-design)
4. [Airflow DAG Implementations](#airflow-dag-implementations)
5. [Recovery Scenarios](#recovery-scenarios)
6. [Data Validation and Quality Checks](#data-validation-and-quality-checks)
7. [Monitoring and Alerting](#monitoring-and-alerting)

---

## Chart Recovery Overview

### Recovery Objectives

**Recovery Point Objective (RPO):** 1 minute
**Recovery Time Objective (RTO):** 15 minutes

### Recovery Data Sources (Priority Order)

```
1. ScyllaDB (Primary)
   └─ crypto_market_data.candles_1m (90 days retention)

2. S3 Archive (Secondary)
   └─ s3://crypto-data-lake/candles/1m/{source}/{symbol}/{year}/{month}/{day}/

3. Kafka Topic Replay (Tertiary - for recent data)
   └─ Topic: aggregated-price-events (7 days retention)

4. Provider API Re-fetch (Last Resort - for critical gaps)
   └─ Binance & Huobi historical data APIs
```

### Recovery Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    RECOVERY ORCHESTRATOR                        │
│                    (Apache Airflow DAG)                         │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Step 1: Data Gap Detection                             │  │
│  │  ├─ Query ScyllaDB for missing time ranges              │  │
│  │  ├─ Check for zero-volume anomalies                     │  │
│  │  └─ Identify corrupted candles                          │  │
│  └─────────────────────────────────────────────────────────┘  │
│                          ↓                                      │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Step 2: Source Selection                               │  │
│  │  ├─ Gap < 7 days → Kafka replay                         │  │
│  │  ├─ Gap 7-90 days → ScyllaDB re-aggregate               │  │
│  │  ├─ Gap > 90 days → S3 Parquet files                    │  │
│  │  └─ Critical gaps → Provider API re-fetch               │  │
│  └─────────────────────────────────────────────────────────┘  │
│                          ↓                                      │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Step 3: Data Reconstruction                            │  │
│  │  ├─ Spark batch job reads source data                   │  │
│  │  ├─ Regenerate 1m candles (if needed)                   │  │
│  │  └─ Aggregate to all timeframes (2m, 5m, 1h, 2h, 1d, 1w)│  │
│  └─────────────────────────────────────────────────────────┘  │
│                          ↓                                      │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Step 4: Validation & Write                             │  │
│  │  ├─ Validate OHLC integrity (O≤H, L≤C, H≥L)            │  │
│  │  ├─ Check volume consistency                            │  │
│  │  └─ Write to ScyllaDB with idempotent operations        │  │
│  └─────────────────────────────────────────────────────────┘  │
│                          ↓                                      │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Step 5: Reconciliation                                 │  │
│  │  ├─ Compare recovered data with checksums               │  │
│  │  ├─ Log recovery metrics                                │  │
│  │  └─ Notify operations team                              │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## Apache Airflow Workflow Architecture

Apache Airflow serves as the central orchestrator for all batch aggregation jobs and recovery workflows. It manages task scheduling, dependency resolution, retry logic, and monitoring.

### Airflow Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                     AIRFLOW WEB SERVER & SCHEDULER                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────────┐    ┌─────────────────────┐                │
│  │   DAG Definitions    │    │   Task Scheduler    │                │
│  │  (Python Scripts)    │───>│   (Cron-based)      │                │
│  └─────────────────────┘    └─────────────────────┘                │
│                                      │                                │
│                                      ▼                                │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      TASK EXECUTION                           │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │  │
│  │  │  DAG 1   │  │  DAG 2   │  │  DAG 3   │  │Recovery  │    │  │
│  │  │  2m, 5m  │  │15m, 1h   │  │  1d, 1w  │  │   DAG    │    │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                      │                                │
└──────────────────────────────────────┼────────────────────────────────┘
                                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        SPARK CLUSTER                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ Spark Job 1  │  │ Spark Job 2  │  │ Spark Job 3  │              │
│  │ (2m, 5m)     │  │ (15m, 1h)    │  │ (1d, 1w)     │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└──────────────────────────────────────────────────────────────────────┘
                                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           SCYLLADB                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### DAG Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                    AGGREGATION WORKFLOW                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Real-time Streaming (1m) ──┐                                   │
│                              │                                   │
│                              ▼                                   │
│                         [2m Candles]                             │
│                              │                                   │
│                              ▼                                   │
│                         [5m Candles]                             │
│                              │                                   │
│                              ▼                                   │
│                        [15m Candles]                             │
│                              │                                   │
│                              ▼                                   │
│                         [1h Candles]                             │
│                              │                                   │
│                              ▼                                   │
│                         [1d Candles]                             │
│                              │                                   │
│                              ▼                                   │
│                         [1w Candles]                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Key Airflow Features Used

1. **Task Dependencies**: Ensure 15m runs after 5m, 1h after 15m, etc.
2. **SLAs**: Alert if jobs don't complete within expected timeframes
3. **Retry Logic**: Automatic retry with exponential backoff
4. **Catchup**: Backfill historical data for missed runs
5. **Sensors**: Wait for upstream data availability
6. **BranchOperator**: Conditional execution (e.g., weekly jobs only on Mondays)
7. **XCom**: Pass metadata between tasks (e.g., processed record counts)

---

## Batch Aggregation Job Design

### Timeframe Hierarchy

```
Raw Price Events (10-second ticks)
    ↓
1-Minute Candles (Base Layer - Real-time Spark Streaming)
    ↓
┌───────────────────────────────────────────────────────┐
│         BATCH AGGREGATION JOBS (Spark)                │
├───────────────────────────────────────────────────────┤
│                                                        │
│  2-Minute Candles (2m) ← Aggregate from 1m           │
│      ↓                                                 │
│  5-Minute Candles (5m) ← Aggregate from 1m           │
│      ↓                                                 │
│  15-Minute Candles (15m) ← Aggregate from 5m         │
│      ↓                                                 │
│  1-Hour Candles (1h) ← Aggregate from 15m            │
│      ↓                                                 │
│  1-Day Candles (1d) ← Aggregate from 1h              │
│      ↓                                                 │
│  1-Week Candles (1w) ← Aggregate from 1d             │
│                                                        │
└───────────────────────────────────────────────────────┘
```

### Job Schedule

| Timeframe | Schedule | Input Source | Processing Window | Backfill Capability |
|-----------|----------|--------------|-------------------|---------------------|
| 2m | Every 2 minutes | candles_1m | Last 2 minutes | Yes (from 1m) |
| 5m | Every 5 minutes | candles_1m | Last 5 minutes | Yes (from 1m) |
| 15m | Every 15 minutes | candles_5m | Last 15 minutes | Yes (from 5m or 1m) |
| 1h | Every hour (at :00) | candles_15m | Last 1 hour | Yes (from 15m, 5m, or 1m) |
| 1d | Daily at 00:00 UTC | candles_1h | Last 24 hours | Yes (from 1h) |
| 1w | Weekly on Monday 00:00 UTC | candles_1d | Last 7 days | Yes (from 1d) |

### Spark Batch Job Implementation

#### Job 1: Short-Interval Aggregation (2m, 5m)

**Schedule:** Continuous micro-batch (every 1 minute)
**Input:** `crypto_market_data.candles_1m`
**Output:** `crypto_market_data.candles_aggregated`

**PySpark Pseudocode:**
```python
# Configuration
timeframes = {
    '2m': {'window': '2 minutes', 'source': 'candles_1m'},
    '5m': {'window': '5 minutes', 'source': 'candles_1m'}
}

def aggregate_short_intervals(source_table, timeframes):
    for tf_name, config in timeframes.items():
        # Read last N minutes from ScyllaDB
        lookback_minutes = int(config['window'].split()[0]) + 1

        df = (spark.read
            .format("org.apache.spark.sql.cassandra")
            .options(table=config['source'], keyspace="crypto_market_data")
            .load()
            .where(f"timestamp >= now() - INTERVAL {lookback_minutes} MINUTES")
        )

        # Aggregate by timeframe
        aggregated = (df
            .groupBy(
                window("timestamp", config['window']),
                "source",
                "symbol"
            )
            .agg(
                first("open").alias("open"),
                max("high").alias("high"),
                min("low").alias("low"),
                last("close").alias("close"),
                sum("volume").alias("volume"),
                sum("tick_count").alias("tick_count"),
                current_timestamp().alias("created_at")
            )
            .withColumn("timeframe", lit(tf_name))
            .withColumn("omsServerId",
                concat(
                    col("source"),
                    lit("_"),
                    lit(tf_name),
                    lit("_"),
                    date_format(col("window.start"), "yyyyMM")
                )
            )
        )

        # Write to ScyllaDB (idempotent)
        (aggregated
            .write
            .format("org.apache.spark.sql.cassandra")
            .options(table="candles_aggregated", keyspace="crypto_market_data")
            .mode("append")
            .option("spark.cassandra.output.consistency.level", "QUORUM")
            .save()
        )
```

#### Job 2: Hourly Aggregation (1h, 2h)

**Schedule:** Hourly at :05 (5 minutes after the hour)
**Input:** `crypto_market_data.candles_5m` for 1h, `candles_1h` for 2h
**Output:** `crypto_market_data.candles_aggregated`

**PySpark Pseudocode:**
```python
def aggregate_hourly():
    # 1-Hour Candles from 5-minute candles
    df_5m = read_candles("5m", lookback_hours=2)

    candles_1h = (df_5m
        .groupBy(
            window("timestamp", "1 hour"),
            "source",
            "symbol"
        )
        .agg(
            first("open").alias("open"),
            max("high").alias("high"),
            min("low").alias("low"),
            last("close").alias("close"),
            sum("volume").alias("volume"),
            sum("tick_count").alias("tick_count")
        )
        .withColumn("timeframe", lit("1h"))
        .withColumn("omsServerId", generate_oms_id("1h"))
    )

    write_to_scylla(candles_1h, "candles_aggregated")

    # 2-Hour Candles from 1-hour candles (if 1h data exists)
    df_1h = read_candles("1h", lookback_hours=3)

    candles_2h = (df_1h
        .groupBy(
            window("timestamp", "2 hours"),
            "source",
            "symbol"
        )
        .agg(
            first("open").alias("open"),
            max("high").alias("high"),
            min("low").alias("low"),
            last("close").alias("close"),
            sum("volume").alias("volume"),
            sum("tick_count").alias("tick_count")
        )
        .withColumn("timeframe", lit("2h"))
        .withColumn("omsServerId", generate_oms_id("2h"))
    )

    write_to_scylla(candles_2h, "candles_aggregated")
```

#### Job 3: Daily and Weekly Aggregation (1d, 1w)

**Schedule:**
- Daily: 00:05 UTC
- Weekly: Monday 00:10 UTC

**Input:** `crypto_market_data.candles_1h` for 1d, `candles_1d` for 1w
**Output:** `crypto_market_data.candles_aggregated`

**PySpark Pseudocode:**
```python
def aggregate_daily():
    # Daily candles from 1-hour candles
    df_1h = read_candles("1h", lookback_days=2)

    candles_1d = (df_1h
        .groupBy(
            window("timestamp", "1 day"),
            "source",
            "symbol"
        )
        .agg(
            first("open").alias("open"),
            max("high").alias("high"),
            min("low").alias("low"),
            last("close").alias("close"),
            sum("volume").alias("volume"),
            sum("tick_count").alias("tick_count")
        )
        .withColumn("timeframe", lit("1d"))
        .withColumn("omsServerId", generate_oms_id("1d"))
    )

    write_to_scylla(candles_1d, "candles_aggregated")

def aggregate_weekly():
    # Weekly candles from daily candles
    df_1d = read_candles("1d", lookback_weeks=2)

    candles_1w = (df_1d
        .groupBy(
            window("timestamp", "1 week"),
            "source",
            "symbol"
        )
        .agg(
            first("open").alias("open"),
            max("high").alias("high"),
            min("low").alias("low"),
            last("close").alias("close"),
            sum("volume").alias("volume"),
            sum("tick_count").alias("tick_count")
        )
        .withColumn("timeframe", lit("1w"))
        .withColumn("omsServerId", generate_oms_id("1w"))
    )

    write_to_scylla(candles_1w, "candles_aggregated")
```

### Apache Airflow DAG Structure

```python
# Airflow DAG for Batch Aggregation Jobs

from airflow import DAG
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'data-platform',
    'depends_on_past': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
}

# DAG 1: Short Intervals (2m, 5m)
with DAG('aggregate_short_intervals',
         default_args=default_args,
         schedule_interval='*/1 * * * *',  # Every minute
         catchup=False) as dag_short:

    aggregate_2m_5m = SparkSubmitOperator(
        task_id='aggregate_2m_5m',
        application='/opt/spark/jobs/aggregate_short_intervals.py',
        conf={
            'spark.executor.memory': '4g',
            'spark.executor.cores': '2',
        }
    )

# DAG 2: Hourly Intervals (1h, 2h)
with DAG('aggregate_hourly',
         default_args=default_args,
         schedule_interval='5 * * * *',  # Every hour at :05
         catchup=True) as dag_hourly:

    aggregate_1h_2h = SparkSubmitOperator(
        task_id='aggregate_1h_2h',
        application='/opt/spark/jobs/aggregate_hourly.py',
        conf={
            'spark.executor.memory': '8g',
            'spark.executor.cores': '4',
        }
    )

# DAG 3: Daily and Weekly Intervals (1d, 1w)
with DAG('aggregate_daily_weekly',
         default_args=default_args,
         schedule_interval='5 0 * * *',  # Daily at 00:05 UTC
         catchup=True) as dag_daily:

    aggregate_1d = SparkSubmitOperator(
        task_id='aggregate_daily',
        application='/opt/spark/jobs/aggregate_daily.py',
        conf={
            'spark.executor.memory': '8g',
            'spark.executor.cores': '4',
        }
    )

    # Weekly aggregation runs only on Mondays
    aggregate_1w = SparkSubmitOperator(
        task_id='aggregate_weekly',
        application='/opt/spark/jobs/aggregate_weekly.py',
        conf={
            'spark.executor.memory': '8g',
            'spark.executor.cores': '4',
        },
        trigger_rule='none_failed',
        # Only run on Mondays
        execution_timeout=timedelta(hours=1)
    )

    aggregate_1d >> aggregate_1w
```

---

## Airflow DAG Implementations

This section provides comprehensive Airflow DAG implementations for managing all chart aggregation timeframes with proper dependencies, error handling, and recovery mechanisms.

### Complete DAG Configuration

```python
# File: dags/chart_aggregation_dags.py

from airflow import DAG
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator
from airflow.operators.python import PythonOperator, BranchPythonOperator
from airflow.sensors.external_task import ExternalTaskSensor
from airflow.utils.task_group import TaskGroup
from datetime import datetime, timedelta
from airflow.models import Variable

# Common configuration
default_args = {
    'owner': 'data-platform',
    'depends_on_past': False,
    'email': ['data-team@company.com'],
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
    'retry_exponential_backoff': True,
    'max_retry_delay': timedelta(minutes=30),
}

# Spark job configurations by timeframe
SPARK_CONFIGS = {
    '2m_5m': {
        'executor_memory': '4g',
        'executor_cores': 2,
        'num_executors': 5,
        'driver_memory': '2g',
    },
    '15m': {
        'executor_memory': '4g',
        'executor_cores': 2,
        'num_executors': 5,
        'driver_memory': '2g',
    },
    '1h': {
        'executor_memory': '8g',
        'executor_cores': 4,
        'num_executors': 10,
        'driver_memory': '4g',
    },
    '1d_1w': {
        'executor_memory': '8g',
        'executor_cores': 4,
        'num_executors': 5,
        'driver_memory': '4g',
    },
}


### DAG 1: Short Intervals (2m, 5m) - Every Minute

# Schedule: Every minute
# Dependencies: Depends on 1m candles from real-time streaming
# SLA: 2 minutes

with DAG(
    'aggregate_short_intervals',
    default_args=default_args,
    description='Aggregate 2-minute and 5-minute candles from 1-minute candles',
    schedule_interval='*/1 * * * *',
    start_date=datetime(2024, 1, 1),
    catchup=False,
    max_active_runs=1,
    tags=['candles', 'aggregation', 'short-interval'],
    sla_miss_callback=lambda *args: print("SLA MISSED: Short interval aggregation")
) as dag_short:

    check_1m_data_availability = PythonOperator(
        task_id='check_1m_data_availability',
        python_callable=lambda: check_recent_candles('1m', minutes_back=2),
        execution_timeout=timedelta(seconds=30),
    )

    aggregate_2m_5m = SparkSubmitOperator(
        task_id='aggregate_2m_5m',
        application='/opt/spark/jobs/aggregate_short_intervals.py',
        name='aggregate-2m-5m-{{ ds }}-{{ ts_nodash }}',
        conf=SPARK_CONFIGS['2m_5m'],
        application_args=[
            '--execution-date', '{{ ds }}',
            '--execution-ts', '{{ ts }}',
        ],
        verbose=True,
        execution_timeout=timedelta(minutes=2),
    )

    validate_2m_5m = PythonOperator(
        task_id='validate_2m_5m',
        python_callable=lambda: validate_candles(['2m', '5m']),
        execution_timeout=timedelta(seconds=30),
    )

    check_1m_data_availability >> aggregate_2m_5m >> validate_2m_5m


### DAG 2: 15-Minute Aggregation - Every 15 Minutes

# Schedule: Every 15 minutes
# Dependencies: Depends on 5m candles
# SLA: 5 minutes

with DAG(
    'aggregate_15m',
    default_args=default_args,
    description='Aggregate 15-minute candles from 5-minute candles',
    schedule_interval='*/15 * * * *',
    start_date=datetime(2024, 1, 1),
    catchup=True,
    max_active_runs=1,
    tags=['candles', 'aggregation', '15-minute'],
) as dag_15m:

    wait_for_5m_candles = ExternalTaskSensor(
        task_id='wait_for_5m_candles',
        external_dag_id='aggregate_short_intervals',
        external_task_id='validate_2m_5m',
        timeout=300,  # 5 minutes
        poke_interval=10,
        mode='poke',
    )

    aggregate_15m = SparkSubmitOperator(
        task_id='aggregate_15m',
        application='/opt/spark/jobs/aggregate_15m.py',
        name='aggregate-15m-{{ ds }}-{{ ts_nodash }}',
        conf=SPARK_CONFIGS['15m'],
        application_args=[
            '--execution-date', '{{ ds }}',
            '--lookback-minutes', '15',
        ],
        verbose=True,
        execution_timeout=timedelta(minutes=5),
    )

    validate_15m = PythonOperator(
        task_id='validate_15m',
        python_callable=lambda: validate_candles(['15m']),
        execution_timeout=timedelta(seconds=30),
    )

    wait_for_5m_candles >> aggregate_15m >> validate_15m


### DAG 3: Hourly Aggregation - Every Hour

# Schedule: Every hour at :05
# Dependencies: Depends on 15m candles
# SLA: 10 minutes

with DAG(
    'aggregate_hourly',
    default_args=default_args,
    description='Aggregate 1-hour candles from 15-minute candles',
    schedule_interval='5 * * * *',
    start_date=datetime(2024, 1, 1),
    catchup=True,
    max_active_runs=1,
    tags=['candles', 'aggregation', 'hourly'],
) as dag_hourly:

    wait_for_15m_candles = ExternalTaskSensor(
        task_id='wait_for_15m_candles',
        external_dag_id='aggregate_15m',
        external_task_id='validate_15m',
        timeout=600,  # 10 minutes
        poke_interval=30,
        mode='poke',
        execution_delta=timedelta(minutes=5),  # Account for schedule difference
    )

    aggregate_1h = SparkSubmitOperator(
        task_id='aggregate_1h',
        application='/opt/spark/jobs/aggregate_hourly.py',
        name='aggregate-1h-{{ ds }}-{{ ts_nodash }}',
        conf=SPARK_CONFIGS['1h'],
        application_args=[
            '--execution-date', '{{ ds }}',
            '--lookback-hours', '1',
        ],
        verbose=True,
        execution_timeout=timedelta(minutes=10),
    )

    validate_1h = PythonOperator(
        task_id='validate_1h',
        python_callable=lambda: validate_candles(['1h']),
        execution_timeout=timedelta(minutes=1),
    )

    wait_for_15m_candles >> aggregate_1h >> validate_1h


### DAG 4: Daily and Weekly Aggregation

# Schedule: Daily at 00:05 UTC
# Dependencies: Depends on 1h candles
# SLA: 30 minutes for daily, 60 minutes for weekly

with DAG(
    'aggregate_daily_weekly',
    default_args=default_args,
    description='Aggregate daily and weekly candles',
    schedule_interval='5 0 * * *',
    start_date=datetime(2024, 1, 1),
    catchup=True,
    max_active_runs=1,
    tags=['candles', 'aggregation', 'daily', 'weekly'],
) as dag_daily_weekly:

    wait_for_1h_candles = ExternalTaskSensor(
        task_id='wait_for_1h_candles',
        external_dag_id='aggregate_hourly',
        external_task_id='validate_1h',
        timeout=1800,  # 30 minutes
        poke_interval=60,
        mode='poke',
    )

    aggregate_1d = SparkSubmitOperator(
        task_id='aggregate_daily',
        application='/opt/spark/jobs/aggregate_daily.py',
        name='aggregate-1d-{{ ds }}-{{ ts_nodash }}',
        conf=SPARK_CONFIGS['1d_1w'],
        application_args=[
            '--execution-date', '{{ ds }}',
            '--lookback-days', '1',
        ],
        verbose=True,
        execution_timeout=timedelta(minutes=30),
    )

    validate_1d = PythonOperator(
        task_id='validate_daily',
        python_callable=lambda: validate_candles(['1d']),
        execution_timeout=timedelta(minutes=2),
    )

    # Branch to check if it's Monday for weekly aggregation
    def is_monday_check():
        return 'aggregate_weekly' if datetime.now().weekday() == 0 else 'skip_weekly'

    check_if_monday = BranchPythonOperator(
        task_id='check_if_monday',
        python_callable=is_monday_check,
    )

    aggregate_1w = SparkSubmitOperator(
        task_id='aggregate_weekly',
        application='/opt/spark/jobs/aggregate_weekly.py',
        name='aggregate-1w-{{ ds }}-{{ ts_nodash }}',
        conf=SPARK_CONFIGS['1d_1w'],
        application_args=[
            '--execution-date', '{{ ds }}',
            '--lookback-weeks', '1',
        ],
        verbose=True,
        execution_timeout=timedelta(hours=1),
    )

    validate_1w = PythonOperator(
        task_id='validate_weekly',
        python_callable=lambda: validate_candles(['1w']),
        execution_timeout=timedelta(minutes=5),
        trigger_rule='none_failed_min_one_success',
    )

    skip_weekly = PythonOperator(
        task_id='skip_weekly',
        python_callable=lambda: print("Not Monday, skipping weekly aggregation"),
    )

    # Task dependencies
    wait_for_1h_candles >> aggregate_1d >> validate_1d >> check_if_monday
    check_if_monday >> [aggregate_1w, skip_weekly]
    aggregate_1w >> validate_1w


### DAG 5: Chart Recovery DAG

# Trigger: Manual or by alert
# Purpose: Recover missing or corrupted candle data

with DAG(
    'chart_recovery',
    default_args=default_args,
    description='Recover missing or corrupted candle data',
    schedule_interval=None,  # Manual trigger only
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=['candles', 'recovery'],
) as dag_recovery:

    detect_gaps = PythonOperator(
        task_id='detect_gaps',
        python_callable=lambda **context: detect_data_gaps(context['dag_run'].conf),
        provide_context=True,
    )

    determine_recovery_source = BranchPythonOperator(
        task_id='determine_recovery_source',
        python_callable=lambda **context: select_recovery_source(context['dag_run'].conf),
        provide_context=True,
    )

    with TaskGroup('recovery_from_kafka') as kafka_recovery:
        replay_kafka = SparkSubmitOperator(
            task_id='replay_kafka',
            application='/opt/spark/jobs/recovery_kafka_replay.py',
            conf=SPARK_CONFIGS['15m'],
        )

    with TaskGroup('recovery_from_scylladb') as scylladb_recovery:
        reaggregate_from_db = SparkSubmitOperator(
            task_id='reaggregate_from_db',
            application='/opt/spark/jobs/recovery_reaggregate.py',
            conf=SPARK_CONFIGS['1h'],
        )

    with TaskGroup('recovery_from_s3') as s3_recovery:
        load_from_s3 = SparkSubmitOperator(
            task_id='load_from_s3',
            application='/opt/spark/jobs/recovery_s3_load.py',
            conf=SPARK_CONFIGS['1d_1w'],
        )

    validate_recovery = PythonOperator(
        task_id='validate_recovery',
        python_callable=lambda: validate_recovery_results(),
        trigger_rule='none_failed_min_one_success',
    )

    notify_completion = PythonOperator(
        task_id='notify_completion',
        python_callable=lambda: send_recovery_notification(),
    )

    # Recovery workflow
    detect_gaps >> determine_recovery_source
    determine_recovery_source >> [kafka_recovery, scylladb_recovery, s3_recovery]
    [kafka_recovery, scylladb_recovery, s3_recovery] >> validate_recovery >> notify_completion


# Helper functions (referenced in DAGs above)
def check_recent_candles(timeframe, minutes_back):
    """Check if recent candles exist in ScyllaDB"""
    pass  # Implementation would query ScyllaDB

def validate_candles(timeframes):
    """Validate OHLC integrity for specified timeframes"""
    pass  # Implementation would run validation queries

def detect_data_gaps(config):
    """Detect gaps in candle data"""
    pass  # Implementation would query for gaps

def select_recovery_source(config):
    """Determine which recovery method to use"""
    gap_age_days = config.get('gap_age_days', 0)
    if gap_age_days < 7:
        return 'recovery_from_kafka.replay_kafka'
    elif gap_age_days < 90:
        return 'recovery_from_scylladb.reaggregate_from_db'
    else:
        return 'recovery_from_s3.load_from_s3'

def validate_recovery_results():
    """Validate recovered data"""
    pass  # Implementation would validate recovered candles

def send_recovery_notification():
    """Send notification about recovery completion"""
    pass  # Implementation would send email/Slack notification
```

### Airflow Variables and Connections

**Required Airflow Variables:**
```python
# Set via Airflow UI or CLI: airflow variables set <key> <value>
{
    "spark_master": "spark://spark-master:7077",
    "scylladb_contact_points": "scylla-node-1,scylla-node-2,scylla-node-3",
    "kafka_brokers": "kafka-1:9092,kafka-2:9092,kafka-3:9092",
    "s3_bucket": "crypto-data-lake",
    "alert_email": "data-team@company.com",
    "slack_webhook": "https://hooks.slack.com/services/xxx"
}
```

**Required Airflow Connections:**
```bash
# Spark connection
airflow connections add spark_default \
    --conn-type spark \
    --conn-host spark://spark-master:7077

# AWS S3 connection
airflow connections add aws_default \
    --conn-type aws \
    --conn-extra '{"region_name": "us-east-1"}'
```

### DAG Monitoring and SLAs

**SLA Configuration:**
```python
sla_miss_callback_config = {
    'aggregate_short_intervals': timedelta(minutes=2),
    'aggregate_15m': timedelta(minutes=5),
    'aggregate_hourly': timedelta(minutes=10),
    'aggregate_daily_weekly': timedelta(minutes=30),
}
```

**Custom Metrics Tracking:**
```python
from airflow.metrics import Stats

def track_aggregation_metrics(timeframe, record_count, execution_time):
    Stats.gauge(f'candles.{timeframe}.record_count', record_count)
    Stats.timing(f'candles.{timeframe}.execution_time', execution_time)
```

### Backfill Strategy

**Manual Backfill Command:**
```bash
# Backfill 15m candles for a specific date range
airflow dags backfill \
    --start-date 2024-01-01 \
    --end-date 2024-01-31 \
    --reset-dagruns \
    aggregate_15m

# Backfill all timeframes sequentially
for dag in aggregate_short_intervals aggregate_15m aggregate_hourly aggregate_daily_weekly; do
    airflow dags backfill \
        --start-date 2024-01-01 \
        --end-date 2024-01-31 \
        $dag
done
```

---

## Recovery Scenarios

### Scenario 1: Missing 1-Minute Candles (Recent Data < 7 days)

**Trigger:** Gap detection in candles_1m table
**Recovery Source:** Kafka topic replay

**Process:**
1. **Gap Detection Query:**
```sql
-- Detect missing 1-minute candles
SELECT source, symbol,
       lag(timestamp) OVER (PARTITION BY source, symbol ORDER BY timestamp) as prev_ts,
       timestamp,
       timestamp - lag(timestamp) OVER (PARTITION BY source, symbol ORDER BY timestamp) as gap_duration
FROM crypto_market_data.candles_1m
WHERE source = 'BINANCE' AND symbol = 'ETHUSDT'
  AND timestamp > now() - INTERVAL 7 DAYS
HAVING gap_duration > INTERVAL 1 MINUTE;
```

2. **Kafka Replay:**
```python
# Spark Streaming job to replay Kafka messages
spark_replay = (spark
    .readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "kafka-cluster:9092")
    .option("subscribe", "aggregated-price-events")
    .option("startingOffsets", f'{{"aggregated-price-events":{{"0":{start_offset}}}}}')
    .option("endingOffsets", f'{{"aggregated-price-events":{{"0":{end_offset}}}}}')
    .load()
)

# Re-aggregate 1m candles
recovered_candles = aggregate_to_1m(spark_replay, gap_start, gap_end)
write_to_scylla(recovered_candles, "candles_1m")
```

3. **Trigger downstream aggregation jobs** for affected timeframes

---

### Scenario 2: Corrupted Higher Timeframe Candles

**Trigger:** Data validation failure (e.g., High < Low, Volume = 0)
**Recovery Source:** Re-aggregate from lower timeframe

**Process:**
1. **Validation Check:**
```python
def validate_candles(df):
    invalid_candles = df.filter(
        (col("high") < col("low")) |
        (col("open") > col("high")) |
        (col("close") > col("high")) |
        (col("open") < col("low")) |
        (col("close") < col("low")) |
        (col("volume") < 0)
    )
    return invalid_candles
```

2. **Delete Corrupted Records:**
```python
# Delete corrupted candles from ScyllaDB
corrupted_ids = get_corrupted_candle_ids()
for candle_id in corrupted_ids:
    delete_from_scylla(candle_id)
```

3. **Re-aggregate from Source:**
```python
# Example: Recover 1h candles from 5m data
df_5m = read_candles_for_period("5m", corrupted_start, corrupted_end)
recovered_1h = aggregate_to_1h(df_5m)
write_to_scylla(recovered_1h, "candles_aggregated")
```

---

### Scenario 3: Historical Data Backfill (> 90 days old)

**Trigger:** Manual backfill request or new symbol addition
**Recovery Source:** S3 Parquet archives

**Process:**
1. **Read from S3:**
```python
# Read archived 1m candles from S3
df_archived = (spark.read
    .format("parquet")
    .load("s3://crypto-data-lake/candles/1m/BINANCE/ETHUSDT/2023/06/*/")
)
```

2. **Aggregate All Timeframes:**
```python
# Generate all timeframes from archived data
for timeframe in ['2m', '5m', '1h', '2h', '1d', '1w']:
    aggregated = aggregate_timeframe(df_archived, timeframe)
    write_to_scylla(aggregated, "candles_aggregated")
```

3. **Verify Completeness:**
```python
# Check that all expected candles were created
expected_count = calculate_expected_candles(start_date, end_date, timeframe)
actual_count = count_candles_in_scylla(start_date, end_date, timeframe)
assert actual_count == expected_count, "Backfill incomplete"
```

---

### Scenario 4: Complete Data Loss (Disaster Recovery)

**Trigger:** ScyllaDB cluster failure, data corruption
**Recovery Source:** S3 archives + Kafka replay

**Process:**
```
Step 1: Restore from S3 (older data)
    ↓
Step 2: Replay Kafka (last 7 days)
    ↓
Step 3: Re-aggregate all timeframes
    ↓
Step 4: Validate data integrity
    ↓
Step 5: Resume normal operations
```

**Recovery Script:**
```python
def disaster_recovery(recovery_point_date):
    # Step 1: Load all data from S3 up to recovery point
    df_s3 = load_from_s3(start_date='2023-01-01', end_date=recovery_point_date)
    write_to_scylla(df_s3, "candles_1m")

    # Step 2: Replay Kafka for data after recovery point
    df_kafka = replay_kafka(start_date=recovery_point_date, end_date='now')
    write_to_scylla(df_kafka, "candles_1m")

    # Step 3: Re-aggregate all timeframes
    for timeframe in ALL_TIMEFRAMES:
        regenerate_timeframe(timeframe)

    # Step 4: Validation
    validate_all_candles()

    # Step 5: Enable real-time streaming
    start_streaming_jobs()
```

---

## Data Validation and Quality Checks

### Pre-Aggregation Validation

**Rule Set:**
```python
validation_rules = {
    'OHLC_relationship': [
        'high >= open',
        'high >= close',
        'low <= open',
        'low <= close',
        'high >= low'
    ],
    'volume': 'volume >= 0',
    'timestamp': 'timestamp IS NOT NULL',
    'tick_count': 'tick_count > 0',
    'price_sanity': [
        'open > 0',
        'high > 0',
        'low > 0',
        'close > 0'
    ]
}
```

### Post-Aggregation Validation

**Checksum Verification:**
```python
def verify_aggregation(source_df, aggregated_df):
    # Volume should sum correctly
    source_volume = source_df.agg(sum("volume")).collect()[0][0]
    aggregated_volume = aggregated_df.agg(sum("volume")).collect()[0][0]

    assert abs(source_volume - aggregated_volume) < 0.01, "Volume mismatch"

    # Tick count should sum correctly
    source_ticks = source_df.agg(sum("tick_count")).collect()[0][0]
    aggregated_ticks = aggregated_df.agg(sum("tick_count")).collect()[0][0]

    assert source_ticks == aggregated_ticks, "Tick count mismatch"
```

### Gap Detection

**Continuous Monitoring:**
```sql
-- Query to detect gaps in any timeframe
WITH candle_gaps AS (
    SELECT
        source,
        symbol,
        timeframe,
        timestamp,
        LAG(timestamp) OVER (PARTITION BY source, symbol, timeframe ORDER BY timestamp) as prev_timestamp,
        timestamp - LAG(timestamp) OVER (PARTITION BY source, symbol, timeframe ORDER BY timestamp) as gap
    FROM crypto_market_data.candles_aggregated
    WHERE timestamp > now() - INTERVAL 1 DAY
)
SELECT source, symbol, timeframe, prev_timestamp, timestamp, gap
FROM candle_gaps
WHERE gap > expected_interval(timeframe) * 1.5  -- 50% tolerance
ORDER BY gap DESC;
```

---

## Monitoring and Alerting

### Key Metrics

**Aggregation Job Metrics:**
- Job execution time per timeframe
- Records processed per job
- Data validation failure rate
- Gap detection count
- Recovery job invocations

**Data Quality Metrics:**
- Candle completeness percentage (per timeframe)
- OHLC validation failure rate
- Volume anomalies (zero volume, extreme spikes)
- Timestamp consistency

### Alerting Rules

```yaml
alerts:
  - name: MissingCandles
    condition: gap_detected AND gap_duration > 5 minutes
    severity: high
    action: trigger_recovery_job

  - name: ValidationFailure
    condition: validation_failure_rate > 0.1%
    severity: critical
    action: stop_aggregation_jobs, notify_oncall

  - name: AggregationJobFailure
    condition: job_status == 'FAILED'
    severity: critical
    action: retry_with_backoff, notify_oncall

  - name: RecoveryJobRunning
    condition: recovery_job_duration > 30 minutes
    severity: warning
    action: notify_data_team

  - name: DataLag
    condition: latest_candle_timestamp < now() - 10 minutes
    severity: high
    action: check_upstream_pipeline, trigger_recovery
```

### Dashboard Metrics

**Grafana Dashboards:**
1. **Candle Completeness Dashboard**
   - Heatmap showing candle availability by timeframe
   - Gap detection timeline
   - Recovery job status

2. **Aggregation Job Performance**
   - Job execution time trends
   - Records processed per job
   - Error rates

3. **Data Quality Dashboard**
   - OHLC validation pass rate
   - Volume distribution
   - Price anomaly detection

---

## Recovery Runbook

### Manual Recovery Procedure

**Step 1: Identify the Issue**
```bash
# Check for missing candles
python scripts/detect_gaps.py \
  --source BINANCE \
  --symbol ETHUSDT \
  --timeframe 1m \
  --start-date "2023-12-01 00:00:00" \
  --end-date "2023-12-01 23:59:59"
```

**Step 2: Determine Recovery Source**
```bash
# Check data age
gap_age=$(calculate_gap_age)

if [ $gap_age -lt 7 ]; then
    echo "Use Kafka replay"
    recovery_source="kafka"
elif [ $gap_age -lt 90 ]; then
    echo "Re-aggregate from ScyllaDB"
    recovery_source="scylladb"
else
    echo "Load from S3 archive"
    recovery_source="s3"
fi
```

**Step 3: Execute Recovery**
```bash
# Trigger Airflow recovery DAG
airflow dags trigger chart_recovery \
  --conf '{
    "source": "BINANCE",
    "symbol": "ETHUSDT",
    "timeframe": "1m",
    "start_date": "2023-12-01 00:00:00",
    "end_date": "2023-12-01 23:59:59",
    "recovery_source": "'$recovery_source'"
  }'
```

**Step 4: Verify Recovery**
```bash
# Check recovery status
python scripts/verify_recovery.py \
  --recovery-job-id $JOB_ID \
  --validate-ohlc \
  --validate-volume \
  --check-gaps
```

**Step 5: Re-aggregate Downstream**
```bash
# Trigger downstream aggregation for affected timeframes
for timeframe in 2m 5m 1h 2h 1d 1w; do
    airflow dags trigger aggregate_${timeframe} \
      --conf '{"backfill_date": "2023-12-01"}'
done
```

---

## Performance Optimization

### Batch Size Tuning

**Optimal Batch Sizes by Timeframe:**
- 2m, 5m: Process in 1-hour batches
- 1h: Process in 1-day batches
- 2h, 1d: Process in 1-week batches
- 1w: Process in 1-month batches

### Parallel Processing

**Spark Configuration:**
```yaml
Short Intervals (2m, 5m):
  executors: 5
  executor_memory: 4GB
  executor_cores: 2
  parallelism: 20  # 2 symbols × 2 sources × 5 partitions

Hourly (1h, 2h):
  executors: 10
  executor_memory: 8GB
  executor_cores: 4
  parallelism: 40

Daily/Weekly (1d, 1w):
  executors: 5
  executor_memory: 8GB
  executor_cores: 4
  parallelism: 10
```

### ScyllaDB Write Optimization

**Batch Write Strategy:**
```python
# Use batch statements for better write performance
batch_size = 1000
consistency_level = "LOCAL_QUORUM"

def write_candles_batch(candles_df):
    candles_df.coalesce(10).write \
        .format("org.apache.spark.sql.cassandra") \
        .options(table="candles_aggregated", keyspace="crypto_market_data") \
        .mode("append") \
        .option("spark.cassandra.output.batch.size.rows", batch_size) \
        .option("spark.cassandra.output.consistency.level", consistency_level) \
        .option("spark.cassandra.output.concurrent.writes", 10) \
        .save()
```

---

## Conclusion

This chart recovery design provides:

1. **Resilience**: Multiple recovery sources (Kafka, ScyllaDB, S3, Provider APIs)
2. **Automation**: Airflow DAGs for scheduled aggregation and recovery
3. **Data Quality**: Comprehensive validation at every step
4. **Performance**: Optimized batch sizes and parallel processing
5. **Observability**: Detailed metrics and alerting for proactive issue detection
6. **Scalability**: Efficient aggregation hierarchy (1m → 2m → 5m → 1h → 2h → 1d → 1w)

The batch aggregation jobs ensure that all required timeframes (2m, 5m, 1h, 2h, 1d, 1w) are continuously generated with minimal latency and maximum reliability.