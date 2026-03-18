# JP Morgan Chase Software Engineering Virtual Internship - Midas Core

**My Implementation** of the JP Morgan Chase Software Engineering Virtual Experience Program on Forage.

##  Project Overview

This project is my complete implementation of the Midas Core transaction processing system, developed as part of the JP Morgan Chase Software Engineering Virtual Internship. The system demonstrates microservices architecture, event-driven design with Apache Kafka, REST API development, and database integration using Spring Boot.

##  Completed Tasks

### Task 1: Project Setup & Dependency Management
- Configured Spring Boot project with Java 17
- Added required Maven dependencies for Spring Data JPA, Kafka, H2 Database, and testing frameworks
- Set up development environment in IntelliJ IDEA

### Task 2: Kafka Integration
- Implemented `TransactionListener` to consume messages from Kafka topics
- Configured JSON deserialization for incoming transaction messages
- Integrated Spring Kafka with the application using `@KafkaListener`

### Task 3: Database Integration & Transaction Validation
- Created `TransactionRecord` entity with JPA annotations for database persistence
- Implemented `TransactionService` with business logic for:
  - Validating sender and recipient existence
  - Checking sender balance sufficiency
  - Processing valid transactions and updating balances
  - Maintaining many-to-one relationships between transactions and users
- Integrated H2 in-memory database for data persistence

### Task 4: External API Integration
- Created `IncentiveService` to communicate with external Incentive API
- Configured `RestTemplate` for making HTTP POST requests
- Modified transaction processing to:
  - Call Incentive API after validation
  - Add incentive amounts to recipient balances (without deducting from sender)
  - Store incentive amounts in transaction records

### Task 5: REST API Development
- Built `BalanceController` exposing `/balance` GET endpoint
- Implemented `BalanceService` for querying user balances
- Configured application to run on port 33400
- Returns JSON-formatted balance data with proper error handling

##  Tech Stack

- **Framework:** Spring Boot 3.2.5
- **Language:** Java 17
- **Message Broker:** Apache Kafka 3.1.4
- **Database:** H2 (in-memory)
- **ORM:** Spring Data JPA / Hibernate
- **Build Tool:** Maven
- **Testing:** JUnit, Spring Test, Testcontainers

  
##  Certificate

Successfully completed all 5 tasks and earned the JP Morgan Chase Software Engineering Virtual Experience Certificate.

##  Author

**SAINAVA MODAK**
- LinkedIn: https://www.linkedin.com/in/sainava-modak-212942282/

## License

This project was completed as part of the JP Morgan Chase Virtual Experience Program on Forage.

##  Acknowledgments

- JP Morgan Chase for providing this virtual internship opportunity
- Forage for hosting the platform
- The original project scaffold provided by vagabond-systems

---

**Note:** This is an educational project completed as part of a virtual internship program.
