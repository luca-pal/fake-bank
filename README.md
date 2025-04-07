# 💰 Fake Bank – Java Console Banking System

A Java-based banking system that simulates real-world backend logic with a focus on clean architecture, data persistence, and testable service design.
Developed independently as a hands-on practice project, it uses JDBC for SQLite integration, Gradle for build automation, and JUnit + Mockito for test coverage, all structured in a modular and maintainable way.


---

## 🏦 Features

- Account creation with unique account number, PIN, holder name and timestamp.
- Login system.
- Deposit / Withdraw / Transfer funds functionalities.
- Account number validation using the Luhn algorithm.
- Account closure functionality.
- SQLite database with safe PreparedStatement usage.
- Logging system (INFO, WARNING, ERROR).
- Modular package structure.
- User-friendly console formatting.
- Unit testing with JUnit and Mockito.

---

## ⚙️ Technologies

- Java 11
- SQLite (JDBC)
- Gradle
- JUnit 5
- Mockito
- IntelliJ IDEA

---

## 🗂️ Project Structure

```text
src/
├── main/java/banking/
│   ├── app/         # Entry point (Main class)
│   ├── ui/          # Console user interface
│   ├── service/     # Business logic (BankService)
│   ├── domain/      # Data models (BankAccount)
│   └── persistence/ # Database access (DatabaseManager)
└── test/java/banking/
    └── service/     # Unit tests with JUnit + Mockito
```
---

## ▶️ How to Run

```bash
gradle run --args='-fileName mybank.db'
```
- Replace mybank.db with your desired database name.
- The database file will be created automatically in your project directory if it doesn’t exist.
- All account data will be stored in this file using SQLite.

---

## 🔬 How to Run Tests

This project includes unit tests for the business logic layer using JUnit 5 and Mockito.
To execute the tests, run:

```bash
./gradlew test
```
This will automatically:
- Compile test sources.
- Run all tests inside src/test/java.
- Display the results in the terminal.

---

## 📝 License

This project is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).

You’re free to use, modify, and share it — just don’t hold me liable if something breaks. 👾