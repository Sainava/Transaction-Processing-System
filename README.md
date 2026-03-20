# Event-Driven Transaction Processing System (Kafka + Spring Boot)

## Project Overview

This project implements an event-driven transaction processing system using Apache Kafka and Spring Boot. It simulates real-world financial transaction workflows with emphasis on scalability, consistency, and asynchronous processing.

Originally developed as part of the JPMorgan Chase Software Engineering Virtual Experience (Forage), and extended to explore backend system design patterns used in high-throughput financial systems.

## Key Features

- Event-driven architecture using Apache Kafka for asynchronous transaction processing
- **Idempotency & Replay Protection** — Exactly-once transaction semantics with distributed lock mechanism using database-level unique constraints and three-state lock machine (PROCESSING → COMPLETED/FAILED)
- Transaction validation logic ensuring balance consistency and user integrity
- RESTful APIs for querying account balances
- Integration with external services for incentive processing
- Persistent storage using Spring Data JPA with relational mapping
- Fault-tolerant design with structured validation and error handling

## System Design

- Producers publish transaction events to Kafka topics
- Consumers process transactions asynchronously via Kafka listeners
- **Idempotency Layer:** Each transaction is assigned a unique `transactionId` (auto-generated UUID if not provided). The system maintains a `ProcessedEventId` register with database-level unique constraints to detect and prevent duplicate processing. Transactions progress through three states (PROCESSING → COMPLETED/FAILED), ensuring exactly-once semantics even in the presence of retries and network failures.
- Business logic validates transactions and updates balances
- External incentive service is invoked post-validation
- Data is persisted using JPA with transactional integrity and audit trails

This architecture enables decoupled services, improved scalability, reliable transaction processing, and protection against replay attacks in distributed systems.

## Tech Stack

- **Framework:** Spring Boot 3.2.5
- **Language:** Java 17
- **Message Broker:** Apache Kafka 3.1.4
- **Database:** H2 (in-memory)
- **ORM:** Spring Data JPA / Hibernate
- **Build Tool:** Maven
- **Testing:** JUnit, Spring Test, Testcontainers

  
## Author

**SAINAVA MODAK**
- LinkedIn: https://www.linkedin.com/in/sainava-modak-212942282/

## License

This project was completed as part of the JP Morgan Chase Virtual Experience Program on Forage.

## Acknowledgments

This project was initially based on a scaffold provided as part of the JPMorgan Chase Virtual Experience Program on Forage, and has been adapted to explore real-world backend system design concepts.

---
