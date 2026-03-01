# Development task
## Architecture design
### Design a data lake system for stock exchange historical data
- Background: After retrieval data from 2 provider source Houbi and Binance, you need to create a pipeline stream data
to generate candestick. The lowest level candle stick is chart 1 minute.
- Action: 
  - Write a architecture design in file architure-design.md. 
  - You have apply kafka for stream data, you persit data into scylla Database.
  - Apply apache spark to map and reduce data base batch size
  - Desig a structure database to store OHLC after map and reduce
- Do not: You **must not** implement Java code

### Design chart recovery
- Background based on task ` Design a data lake system for stock exchange historical data` 
- Action
  - Design a chart recovery flow and write your solution to file chart-recovery.md
  - Design flow Batch Aggregation Job to generate chart m2, m5, h1, h2, D1, w1
- Do not: You **must not** implement Java code
### Update chart recovery 
- Background based on task `Design chart recovery`
- Action
  - You apply apache airlfow to create and manage scheduler time to generate chart.
  - Update Workflow generate chart m2, m5, m15, h1, D1, W1,... using apache air flow
- Do not: You **must not** implement Java code

### Docker task
#### Task 1
- Background: Read architecture-design.md, you focus on Kafka-cluster
- Action: Write a docker-compose.yml. You config kafka cluster included 1 master and slave. 
  You need to you kafka latest version, config Kraft. You expose port kafka in machine network.
  You must mount Data Kafka in level root project and named data folder.
- *DO NOT*: Do not implement Java. Do not add ssl on Kafka cluster.
#### Task 2
- Background: Read architecture-design.md, you focus on Kafka-cluster
- Action: Create Scylla DB with 1 master and 1 slave.
- *DO NOT*: implement Java code or unit test

### Apache Spark task
#### Task 1
- Background: Read architecture-design.md, you focus on apache spark.
- Action: Write a producers to send event from 2 provider sources which are Houbi and Binance to Kafa
  - You break down topics into 2: inbound-data-<provider-name>
  - You add apache spark into gradle.build.
  - You must implement function and create apache spark based on Java.
  - You must add rety mechanism in case of kafka crash or failed.
  - You must configurate partition based on provider sources. And sub-partition based on symbol
#### Task 2
- Background: Read architecture-design.md, you focus on Apache Spark and task 1.
- Action: You **must** review previous task (Task 1) and imporve some points below:
  - Write a source connector in to file draft.md
  - You need to design pattern for source connector.
  - Design key based on provider source.
- **Do not**: Create Java codes
#### Task 3
- Background: Read architecture-design.md, you focus on Apache Spark and task 1.
- Action: You create a script.sql located at db.migration.
  - You need to create a Key Space is market
  - You need to create a user named market_user and password: abc@1234. Grant full permission to help this user can access market
  - In market Key Space you need to create a table named chart_m1 to store ohlc

#### Task 4
- Background: Read architecture-design.md, you focus on Apache Spark and task 1.
- Action: Create a sink connector using Java, apache spark and ksql. Sink connector is a project at /Users/rubynam/Desktop/workspace/sink-connect.  I have set up basic project. You must apply springboot, you use application.yaml to store config.
- **Do Not **: Implement unit test and **DO NOT** write file MD

### Task 5:
- Background: I want to connect scylla db localhost
- Action: Check build.gradle add scylla db driver or cassandra drive if needed, You have to integrate with jpa.
- The config connection below

```yaml

  cassandra:
    contact-points: ${SCYLLA_CONTACT_POINTS:localhost}
    port: ${SCYLLA_PORT:9042}
    keyspace-name: market
    local-datacenter: ${SCYLLA_DATACENTER:datacenter1}
    schema-action: none
    username: market_user
    password: abc@1234
    request:
      timeout: 30s
      consistency: quorum
    connection:
      connect-timeout: 10s
      init-query-timeout: 10s
    pool:
      idle-timeout: 60s
      heartbeat-interval: 30s
```

### Task 6: [BUG] url jdbc connection string error
```text
2026-03-01T13:46:57.583+07:00 ERROR 65823 --- [           main] o.s.b.d.LoggingFailureAnalysisReporter   : 

***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.

Reason: Failed to determine a suitable driver class


Action:

Consider the following:
	If you want an embedded database (H2, HSQL or Derby), please put it on the classpath.
	If you have database settings to be loaded from a particular profile you may need to activate it (no profiles are currently active).


```
- Check the config, you must REMOVE h2db datasource
### Task 7: [BUG] ScyllaDB connection error
```text
2026-03-01T14:20:21.158+07:00  WARN 67592 --- [     s0-admin-1] c.d.o.d.internal.core.pool.ChannelPool   : [s0|/172.18.0.4:9042]  Error while opening new channel (ConnectionInitException: [s0|connecting...] Protocol initialization request, step 1 (STARTUP {CQL_VERSION=3.0.0, DRIVER_NAME=Apache Cassandra Java Driver, DRIVER_VERSION=4.19.0, CLIENT_ID=4b42ffb6-3bd1-4b68-90fc-b1b79e872db2}): failed to send request (java.nio.channels.NotYetConnectedException))

```

Read the wanr log above. You must fix it.