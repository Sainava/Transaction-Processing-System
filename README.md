# Event-Driven Transaction Processing System (Kafka + Spring Boot)

## Project Overview

This project implements an event-driven transaction processing system using Apache Kafka and Spring Boot. It simulates real-world financial transaction workflows with emphasis on scalability, consistency, and asynchronous processing.

Originally developed as part of the JPMorgan Chase Software Engineering Virtual Experience (Forage), and extended to explore backend system design patterns used in high-throughput financial systems.

## Key Features

- Event-driven architecture using Apache Kafka for asynchronous transaction processing
- Transaction validation logic ensuring balance consistency and user integrity
- RESTful APIs for querying account balances
- Integration with external services for incentive processing
- Persistent storage using Spring Data JPA with relational mapping
- Fault-tolerant design with structured validation and error handling

## System Design

- Producers publish transaction events to Kafka topics
- Consumers process transactions asynchronously via Kafka listeners
- Business logic validates transactions and updates balances
- External incentive service is invoked post-validation
- Data is persisted using JPA with transactional integrity

This architecture enables decoupled services, improved scalability, and reliable transaction processing.

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

**Note:** This is an educational project completed as part of a virtual internship program.
