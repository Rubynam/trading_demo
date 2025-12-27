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
