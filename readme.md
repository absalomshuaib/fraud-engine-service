# Fraud Engine Service

This project is a Spring Boot application that implements fraud detection for payments using:

- Java 8
- Spring Boot 2.7.1
- Spring Data JPA + Hibernate
- MySQL database
- Kafka for event-driven processing
---
## Architecture Overview
1. **Payment Initiated Event**: The application receives payment events via Kafka topics.
2. **Fraud Rules**: Each payment is validated against rules such as Geo-impossible travel, last transaction time, and more.
    Scenarios for fruad rules  include:
    a. Average amount for the last 30 days for a client
    b. Count of payments in the last 3 minutes for velocity check
    c. Check if a duplicate payment exists (same client, merchant, amount in last 3 min)
    d. Get last known country for Geo-Impossible Travel
    e. Independant fraud checks should also be done ( can walk you through how this can be achieved)
3. **Risk Scoring**: Each payment is scored based on risk. Decisions are persisted in MySQL.
4. **Haversine Calculation**: Calculates the distance between the last known location and the current payment location.
5. **Kafka Integration**: Sends and consumes payment events asynchronously.
6. **Database Persistence**: Uses JPA/Hibernate to persist fraud decisions and payment records.
---
## Prerequisites
- Docker >= 20.10
- Docker Compose >= 1.29
- Git
---
## Run Application Locally via Docker
1. Clone the repository:
## Commands you need to Run this repo/project locally
git clone https://github.com/absalomshuaib/fraud-engine-service.git
cd fraud-engine-service
docker-compose up --build ( takes about 3 minutes to build)
--> Builds the images 
--> Starts the containers immediately after building.
Or
docker-compose build
docker-compose up

Should see 4 applications start up
1. Fraud-mysql
2. zookeeper - coordination service for distributed applications
3. Kafka
4. fruad-engine-spring-app

Test curls to run: You can play/change with the transactionIDs, amounts ect to meet difference fraud criteria

API 1:
(This is Johannesburg)
curl -X POST http://localhost:8081/fraudengine/api/payments/initiatePayment \
-H "Content-Type: application/json" \
-d '{
"transactionId": "TXN10001111111",
"clientId": "C123",
"amount": 5000,
"merchantId": "M123",
"country": "ZA",
"timestamp": "2025-12-14T08:00:00Z",
"latitude": -26.2041,
"longitude": 28.0473
}'

API 1:
(This is Cape Town)
curl -X POST http://localhost:8081/fraudengine/api/payments/initiatePayment \
-H "Content-Type: application/json" \
-d '{
"transactionId": "TXN10002",
"clientId": "C123",
"amount": 2000,
"merchantId": "M456",
"country": "ZA",
"timestamp": "2025-12-14T08:30:00Z",
"latitude": -33.9249,
"longitude": 18.4241
}'

API 2: get the transaction result/decision
curl -X GET http://localhost:8081/fraudengine/api/payments/getPaymentDecision/TXN10001111111

curl -X GET http://localhost:8081/fraudengine/api/payments/getPaymentDecision/TXN12222220002


API 3:
(This is Johannesburg)
curl -X POST http://localhost:8081/fraudengine/api/payments/initiatePaymentDecision \
-H "Content-Type: application/json" \
-d '{
"transactionId": "TXN10001111111",
"clientId": "C123",
"amount": 5000,
"merchantId": "M123",
"country": "ZA",
"timestamp": "2025-12-14T08:00:00Z",
"latitude": -26.2041,
"longitude": 28.0473
}'


Checking the mysql DB inside of docker to inspect the table data:

exuc
mysql -u root -p
password root
USE your_database_name;
use fraud_engine;
SHOW TABLES;
DESCRIBE frauds_decisions;
SELECT * FROM frauds_decisions LIMIT 10;
select * from payment;
select * from payment_decision_reasons; ( seperate table made from using @ElementCollection)