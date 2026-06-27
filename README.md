# SplitEase — Smart Expense Splitter

SplitEase calculates the **minimum number of transactions** needed to settle all debts in a group. This README serves as a complete architectural guide, REST API catalog, CRUD execution flow document, and a comprehensive 50-question learning curriculum for beginners to master Spring Boot and full-stack development through this project.

---

## 1. Project Architecture & Flow

SplitEase follows a stateless, three-tier enterprise web application architecture:

```mermaid
graph TD
    %% Frontend Layer
    subgraph Client [Client Layer (HTML5 + CSS + Vanilla JS)]
        UI[index.html / login.html / dashboard.html / group.html]
        Fetch[Fetch API with Authorization: Bearer JWT]
    end

    %% Web Security Filter Chain
    subgraph Security [Security Filter Chain]
        CORS[CorsFilter] --> CSRF[CSRF Disabler]
        CSRF --> JWT_F[JwtFilter (OncePerRequestFilter)]
    end

    %% Spring Boot Backend Layers
    subgraph Backend [Spring Boot Application Layer]
        Controller[REST Controllers: Auth, Group, Expense, Balance, Settlement]
        DTO[DTO Layer: request/ & response/]
        Service[Service Layer: Business Logic, DebtSimplifier]
        Repository[Spring Data JPA Repositories]
    end

    %% Database
    subgraph Storage [Database Layer]
        MySQL[(MySQL 8 Database)]
    end

    %% Connections
    Fetch --> CORS
    JWT_F --> Controller
    Controller --> DTO
    Controller --> Service
    Service --> Repository
    Repository --> MySQL
```

### Flow of a Request
1. **Request Interception**: The client sends an HTTP request with an `Authorization: Bearer <token>` header. Spring Security passes the request through the filter chain.
2. **JWT Authentication**: `JwtFilter` extracts the token, checks its validity via `JwtUtil`, loads the user email, sets a `UsernamePasswordAuthenticationToken` in the `SecurityContext`, and lets the request proceed.
3. **Controller Mapping**: The request matches a route in a `@RestController` (e.g., `ExpenseController`). Inputs are checked by `jakarta.validation` using `@Valid`.
4. **Service Execution**: The Controller delegates tasks to a `@Service` (e.g., `ExpenseService`). The service runs inside a transactional context (`@Transactional`), performing validations and business calculations.
5. **Database Transaction**: The service queries the database using `@Repository` (Spring Data JPA) interfaces.
6. **Response Serialization**: The service returns a DTO (Data Transfer Object) which the controller wraps in a `ResponseEntity` and serializes to JSON for the client.

---

## 2. Database Schema & JPA Model Mappings

The MySQL database schema is structured as follows:

```
                  +-------------------+
                  |       users       |
                  +-------------------+
                  | PK  user_id       |<---------+
                  |     full_name     |          |
                  |     email         |          |
                  |     password_hash |          |
                  +-------------------+          |
                            |                    |
                            |                    |
       +--------------------+--------------------+--------------------+
       |                    |                    |                    |
       v                    v                    v                    v
+--------------+     +--------------+     +--------------+     +--------------+
|    groups    |     | group_members|     |   expenses   |     |  settlements |
+--------------+     +--------------+     +--------------+     +--------------+
| PK  group_id |     | PK  id       |     | PK expense_id|     |PKsettlement_id
| FK  created_by     | FK  group_id  |     | FK  group_id  |     | FK group_id  |
+--------------+     | FK  user_id  |     | FK  paid_by  |     | FK paid_by   |
       ^             +--------------+     +--------------+     | FK paid_to   |
       |                                         |             +--------------+
       +-----------------------------------------+
                                                 |
                                                 v
                                          +--------------+
                                          |expense_splits|
                                          +--------------+
                                          | PK  split_id |
                                          | FK  expense_id
                                          | FK  user_id  |
                                          +--------------+
```

### Table Schemas & Relationships

1. **`users`**: Stores user credentials.
   * *Fields*: `user_id` (PK, Auto-increment), `full_name`, `email` (Unique), `password_hash`, `created_at`.
   * *JPA Mapping*: `User` Entity.

2. **`groups`**: Represents split groups.
   * *Fields*: `group_id` (PK, Auto-increment), `group_name`, `description`, `created_by` (FK to `users`), `created_at`.
   * *JPA Mapping*: `Group` Entity. Many-to-One relation to `User` (`created_by`).

3. **`group_members`**: Join table representing group membership.
   * *Fields*: `id` (PK, Auto-increment), `group_id` (FK to `groups`), `user_id` (FK to `users`), `joined_at`.
   * *JPA Mapping*: `GroupMember` Entity. Many-to-One to `Group` and `User`.

4. **`expenses`**: Logs group expenditures.
   * *Fields*: `expense_id` (PK, Auto-increment), `group_id` (FK to `groups`), `paid_by` (FK to `users`), `description`, `total_amount`, `expense_date`, `created_at`.
   * *JPA Mapping*: `Expense` Entity. Many-to-One to `Group` and `User`.

5. **`expense_splits`**: Maps how expenses are divided.
   * *Fields*: `split_id` (PK, Auto-increment), `expense_id` (FK to `expenses`), `user_id` (FK to `users`), `share`.
   * *JPA Mapping*: `ExpenseSplit` Entity. Many-to-One to `Expense` and `User`.

6. **`settlements`**: Records manual cash transfers clearing balances.
   * *Fields*: `settlement_id` (PK, Auto-increment), `group_id` (FK to `groups`), `paid_by` (FK to `users`), `paid_to` (FK to `users`), `amount`, `note`, `settled_at`.
   * *JPA Mapping*: `Settlement` Entity. Many-to-One to `Group`, `User` (payer), and `User` (receiver).

---

## 3. REST API Specifications

All endpoints (except `/api/auth/**`) require the HTTP header: `Authorization: Bearer <JWT_TOKEN>`.

### Authentication
* **POST `/api/auth/register`**: Register a new user.
  * *Request Body*: `{"name": "Akbar", "email": "akbar@example.com", "password": "password123"}`
  * *Response*: `{"message": "Registration successful"}` (HTTP 200) or validation errors (HTTP 400).
* **POST `/api/auth/login`**: Authenticate credentials.
  * *Request Body*: `{"email": "akbar@example.com", "password": "password123"}`
  * *Response*: `{"token": "<JWT_STRING>", "name": "Akbar", "userId": 1}` (HTTP 200) or error message (HTTP 400/401).

### Groups
* **POST `/api/groups`**: Create a new group.
  * *Request Body*: `{"name": "Road Trip", "description": "Trip to Goa"}`
  * *Response*: Group Details DTO (HTTP 200).
* **GET `/api/groups`**: List all groups the logged-in user belongs to.
  * *Response*: Array of Group Details (HTTP 200).
* **GET `/api/groups/{id}`**: Get specific group metadata and list of members.
  * *Response*: Group Details including members list (HTTP 200).
* **POST `/api/groups/{id}/members`**: Invite a member by email (user must already be registered).
  * *Request Body*: `{"email": "tabassum@example.com"}`
  * *Response*: `{"message": "Member invited successfully"}` (HTTP 200).
* **DELETE `/api/groups/{id}/members/me`**: Leave a group.
  * *Response*: Success message (HTTP 200) or validation error if balance is non-zero (HTTP 400).

### Expenses
* **POST `/api/groups/{id}/expenses`**: Add an expense.
  * *Request Body*: `{"description": "Fuel", "totalAmount": 9000, "paidBy": 1, "splitAmong": [1, 2, 3], "date": "2026-06-27"}`
  * *Response*: Created Expense Details DTO (HTTP 200).
* **GET `/api/groups/{id}/expenses`**: List all group expenses sorted by date descending.
  * *Response*: Array of Expense details (HTTP 200).
* **DELETE `/api/groups/{id}/expenses/{expId}`**: Delete an expense (only available to group creator).
  * *Response*: `{"message": "Expense deleted successfully"}` (HTTP 200).

### Balances & Settlements
* **GET `/api/groups/{id}/balances`**: Compute current net balances and suggested simplified settlements.
  * *Response*:
    ```json
    {
      "netBalances": [
        { "userId": 1, "userName": "Fatma", "balance": 5000.00 },
        { "userId": 2, "userName": "Tabassum", "balance": -1000.00 },
        { "userId": 3, "userName": "Akbar", "balance": -4000.00 }
      ],
      "simplifiedSettlements": [
        { "fromUserId": 2, "fromUserName": "Tabassum", "toUserId": 1, "toUserName": "Fatma", "amount": 1000.00 },
        { "fromUserId": 3, "fromUserName": "Akbar", "toUserId": 1, "toUserName": "Fatma", "amount": 4000.00 }
      ]
    }
    ```
* **POST `/api/groups/{id}/settlements`**: Log a settlement (e.g. paying cash back).
  * *Request Body*: `{"fromUserId": 2, "toUserId": 1, "amount": 1000, "note": "Cash payback"}`
  * *Response*: Created Settlement Details DTO (HTTP 200).
* **GET `/api/groups/{id}/settlements`**: Fetch full history of recorded group settlements.
  * *Response*: Array of Settlements (HTTP 200).

---

## 4. Business Logic: Debt Simplification & Splits

### Greedy Simplification Algorithm
The system computes balances in-memory on every request rather than storing dynamic balance columns in SQL.
1. Fetch all members of the group. Set initial balances to `0.00`.
2. Sum all expenses paid by each user. Add this to their balance.
3. Sum all splits (debts) owed by each user. Subtract this from their balance.
4. Sum all settlements paid by each user. Add this to their balance.
5. Sum all settlements received by each user. Subtract this from their balance.
6. Split members into two groups:
   * **Creditors**: Net balance > 0 (owed money).
   * **Debtors**: Net balance < 0 (owes money).
7. Pair the largest creditor with the largest debtor:
   * Settle amount = `Min(Creditor Owed Amount, Absolute(Debtor Owes Amount))`.
   * Register a suggested transaction: `Debtor pays Creditor -> Settle Amount`.
   * Adjust both balances by the Settle Amount.
   * If a balance reaches `0.00`, remove that user from active processing.
   * Repeat until all balances are resolved to zero.

### Division Penny Rounding
When splitting ₹100.00 equally among 3 people:
* $100.00 \div 3 = 33.33333...$
* If we split ₹33.33 to each, the total split is ₹99.99, leaving a ₹0.01 remainder.
* *Solution*: The system calculates the base floor split (e.g., ₹33.33). It computes the remainder (`totalAmount - (baseSplit * N)`). The remainder (₹0.01) is added to the first member's split, making it:
  * Member 1: ₹33.34
  * Member 2: ₹33.33
  * Member 3: ₹33.33
  * *Total*: ₹100.00 (exact match).

---

## 5. 50 Detailed Spring Boot & Project Q&As

### Database, Schema & Hibernate Naming
#### Q1: Why is `spring.jpa.hibernate.ddl-auto` set to `validate` in our application.properties?
Setting `ddl-auto=validate` forces Hibernate to compare the Java `@Entity` class definitions with the actual database tables at startup. If there is a mismatch (e.g., a missing column or mismatched data type), the application fails to start immediately. This prevents Spring Boot from silently modifying production schemas or throwing runtime SQL execution errors.

#### Q2: What issue did the `groups` table cause in MySQL, and how was it solved?
`groups` is a reserved SQL keyword in MySQL 8. Running raw SQL statements like `CREATE TABLE groups` or `SELECT * FROM groups` throws SQL syntax errors unless the identifier is wrapped in backticks (`` `groups` ``). We solved this on the Hibernate side by setting:
`spring.jpa.properties.hibernate.globally_quoted_identifiers=true` in `application.properties`. This instructs Hibernate to wrap all generated table and column names in backticks automatically.

#### Q3: Why did we write `ON DELETE CASCADE` in our MySQL schema constraints?
If a group is deleted, any memberships, expenses, splits, and settlements associated with that group must be cleaned up to prevent orphaned foreign keys. `ON DELETE CASCADE` ensures that deleting a record automatically triggers deletion of all related child records referencing it.

#### Q4: Why is `expense_splits` designed as a separate table instead of storing list of split percentages inside `expenses`?
Storing splits as a separate table normalizes the database (conforming to 3NF). It allows each split record to be queried, aggregated, and joined easily. This schema structure makes it trivial to support future split features (like unequal or percentage splits) without changing the database structure.

#### Q5: What is the difference between `@JoinColumn` and `mappedBy` in JPA?
* `@JoinColumn` specifies the actual physical column name in the database table containing the foreign key (owner of the relationship).
* `mappedBy` is used on the non-owning side of a bi-directional relationship to point back to the field in the child entity that owns the relation, instructing JPA not to create a redundant join table.

---

### Core Spring Boot & Spring Framework Concepts
#### Q6: What is Dependency Injection (DI) and how is it used in SplitEase?
Dependency Injection is a design pattern where the Spring IoC container creates and injects dependent objects (beans) into class fields at runtime instead of classes instantiating them manually using `new`. For instance, `@Autowired private UserRepo userRepo;` injects the repository bean into the service.

#### Q7: What is Inversion of Control (IoC)?
Inversion of Control is the framework concept where control of execution flows and object lifecycles is handed over to the Spring container rather than being controlled by the main program logic.

#### Q8: What does the `@Component` annotation do?
`@Component` is a generic stereotype annotation indicating that a class is a Spring-managed bean. The Spring classpath scanner automatically detects this class and registers it in the application context.

#### Q9: What is the difference between `@Service` and `@Component`?
`@Service` is a specialized form of `@Component` used to mark classes that contain business logic. While they function identically in bean registration, `@Service` clarifies the layer definition and can be targeted specifically by AOP handlers.

#### Q10: What is the difference between `@Repository` and `@Component`?
`@Repository` is a specialized form of `@Component` used for database access classes. It enables automatic translation of database-specific JDBC exceptions into Spring's unified `DataAccessException` hierarchy.

---

### Rest Controller & Validation Layer
#### Q11: What is the purpose of `@RestController`?
`@RestController` is a convenience annotation that combines `@Controller` and `@ResponseBody`. It marks a class as a REST endpoint handler, meaning every method return value is automatically written directly into the HTTP response body as JSON.

#### Q12: What does `@RequestBody` do?
`@RequestBody` binds the incoming JSON payload from an HTTP request body to a Java object parameters list in a controller method, invoking Spring's Jackson converters under the hood.

#### Q13: What does `@Valid` do?
`@Valid` triggers validation constraints declared on request DTO parameters (e.g. `@NotBlank`, `@Size`). If a constraint fails, Spring prevents execution of the controller method and throws a `MethodArgumentNotValidException`.

#### Q14: How are custom error messages returned from validation failures instead of generic 500 crashes?
We implemented [GlobalExceptionHandler.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/config/GlobalExceptionHandler.java) annotated with `@RestControllerAdvice`. It contains an `@ExceptionHandler(MethodArgumentNotValidException.class)` method that extracts the field-level failures and returns a clean map of field names to error messages with an HTTP 400 Bad Request status.

#### Q15: What is the difference between `@PathVariable` and `@RequestParam`?
* `@PathVariable` extracts values directly from the URL path pattern (e.g., `/api/groups/{id}`).
* `@RequestParam` extracts values from URL query parameters (e.g., `/api/groups?userId=5`).

---

### Service & Business Logic Layer
#### Q16: Why are Spring Services marked with `@Service` and `@Transactional`?
Services contain critical business rules. `@Transactional` ensures that all repository calls executed inside a service method run within a single database transaction. If any exception occurs, the transaction rolls back, preventing partial writes and maintaining data integrity.

#### Q17: What does `@Transactional(readOnly = true)` do?
It optimizes transaction processing for read-only operations. Hibernate uses this hint to disable dirty checking on entities, which improves query performance and reduces locks on tables.

#### Q18: What is the role of `Lazy` autowiring in GroupService?
We used `@Autowired @Lazy private BalanceService balanceService;` in `GroupService` to break a circular dependency. Since `GroupService` calls `BalanceService` to check balances, and `BalanceService` calls `GroupService` to map details, `@Lazy` instructs Spring to initialize a proxy rather than crashing on startup due to circular references.

#### Q19: Why is the debt simplification algorithm designed to run in-memory instead of saving results to database tables?
If balances were stored in tables, every single expense, split modification, deletion, or cash settlement would require complex updates across multiple rows. Running the calculation in-memory dynamically on every query guarantees the data is always fresh, eliminates database synchronization issues, and enables high-performance computations.

#### Q20: How does `ExpenseService` prevent rounding errors?
By utilizing `BigDecimal` for currency and performing division using `RoundingMode.DOWN`, then tracking the difference between the total and distributed sum. This remainder is added to the first user's share to keep splits mathematically exact.

---

### Spring Data JPA & Repository Layer
#### Q21: What is a JpaRepository?
It is a Spring Data JPA interface that provides standard CRUD methods (like `save`, `findById`, `findAll`, `delete`) out-of-the-box, eliminating boilerplate JDBC and Hibernate session code.

#### Q22: What does `@Query` do in JPA Repositories?
`@Query` allows developers to write custom SQL or JPQL (Java Persistence Query Language) statements directly on repository methods when default method naming conventions are insufficient for complex actions (e.g., sum aggregates).

#### Q23: What are JPQL named parameters?
Named parameters are marked with a colon followed by a name (e.g., `:groupId`). In Java, we bind variables to them using `@Param("groupId") Integer groupId`. This prevents SQL injection attacks.

#### Q24: What does the JPQL query `SELECT e.paidBy.userId, SUM(e.totalAmount) FROM Expense e...` do?
It aggregates the total expenses paid by each member in a group, returning a list of object arrays `[userId, totalSum]`, which we parse to build member credits.

#### Q25: Why is `FetchType.LAZY` used instead of `FetchType.EAGER` in our entities?
`LAZY` loading ensures that related entities (e.g. the list of group members when loading a group) are not queried from the database until they are explicitly accessed in code. This avoids fetching unnecessary tables and prevents the N+1 select query problem.

---

### Spring Security & Stateless JWT Authentication
#### Q26: What is a stateless backend in REST API design?
A stateless backend does not store session records (e.g. HTTP Sessions) on the server. Every request from the client must contain all credentials (e.g. JWT Bearer token) needed to authenticate and authorize the operation. This enables horizontal scaling.

#### Q27: How does `OncePerRequestFilter` work?
It is a base filter class guaranteed to execute exactly once per request. We extend it in `JwtFilter` to check the authorization header of every request before it hits the REST endpoints.

#### Q28: How does the server validate a JWT signature?
The server decodes the token header and payload, hashes them using the configured `JWT_SECRET` key, and compares the resulting hash with the signature part of the token. If they do not match, the token has been tampered with and is rejected.

#### Q29: What does `SecurityContextHolder.getContext().setAuthentication(authToken)` do?
Once a JWT is verified, we build a `UsernamePasswordAuthenticationToken` containing the user's principal details and save it in Spring's SecurityContext. This marks the request as authenticated, allowing it to pass authorization rules.

#### Q30: What is the role of BCryptPasswordEncoder?
BCrypt is a one-way hashing function designed with an adjustable work factor (salt). It protects user passwords by hashing them before saving to the DB. Spring Security matches raw passwords using `matches(raw, encoded)` during login.

---

### Core Java & Algorithm Details
#### Q31: Why did we use `BigDecimal` instead of `double` or `float` for currency?
`double` and `float` use binary floating-point representation, which cannot represent base-10 fractions (like 0.1) exactly. This causes arithmetic rounding drift. `BigDecimal` provides arbitrary-precision arithmetic suitable for financial data.

#### Q32: What is the time complexity of the debt simplification algorithm?
The algorithm runs in $O(N \log N)$ time due to sorting user balances into creditors and debtors, and resolves all debts in at most $O(N - 1)$ transactions (where $N$ is the number of members in the group).

#### Q33: How does the algorithm treat cash settlements?
It treats a settlement as a "reverse expense". A payment of amount $A$ from user $X$ to user $Y$ is added to $X$'s credit score and subtracted from $Y$'s credit score, reducing their net debt balances.

#### Q34: What happens if a user's net balance is non-zero and they try to leave the group?
The `leaveGroup` service throws an `IllegalArgumentException` (which is converted to HTTP 400). This enforces group database integrity.

#### Q35: What is the role of DTOs (Data Transfer Objects) in SplitEase?
DTOs isolate database models from API payloads. They prevent internal JPA entities from leaking into JSON serialization, which avoids infinite recursion loops and protects sensitive columns (like `password_hash`).

---

### Frontend & Client-side Architecture
#### Q36: Why does the frontend store the JWT in `localStorage`?
Storing the token in `localStorage` allows client scripts to retrieve it and inject it as a Bearer token in the `Authorization` header of all subsequent API fetch calls.

#### Q37: How does the client handle unauthorized API responses (HTTP 401)?
On receiving an HTTP 401 Unauthorized status, the JavaScript client clears the invalid token from `localStorage` and redirects the page back to `login.html`.

#### Q38: Why did we write DOM nodes using `.textContent` and `.createElement` instead of `.innerHTML`?
Using `.innerHTML` with user-supplied inputs (like expense descriptions or user names) exposes the site to Cross-Site Scripting (XSS) attacks. `.textContent` treats inputs as text strings rather than executable HTML markup.

#### Q39: How does the client compute the "Who paid most" spended metric?
The client loops through the list of expenses returned by the server, accumulates a total paid sum per member, and displays the user with the maximum value dynamically.

#### Q40: How does the client pass the groupId parameter to sub-pages?
It reads query parameters using the URLSearchParams API on page load:
`new URLSearchParams(window.location.search).get('groupId')`.

---

### Miscellaneous & Deployments
#### Q41: Why is `@EnableWebSecurity` placed on our config class?
It activates Spring Security's web configuration support, allowing us to configure customized URL route permissions and attach filters.

#### Q42: What does `@CrossOrigin` or CORS configuration solve?
Cross-Origin Resource Sharing (CORS) prevents browsers from blocking REST API calls originating from a host domain different from the backend server domain.

#### Q43: What is the role of Spring Boot DevTools?
It speeds up development by automatically restarting the application context whenever Java code changes are detected in classpaths.

#### Q44: How does Spring Boot automatically map `server.port=${PORT:8080}`?
It uses the environment variable `PORT` if defined (such as on cloud environments like Railway), otherwise defaulting to `8080` for local dev.

#### Q45: What does `@Builder` do in Lombok?
It automatically generates the Builder pattern boilerplate for instantiation, allowing readable construction code (e.g. `User.builder().name("Akbar").build()`).

#### Q46: What is the difference between Jws and Jwt?
* JWT (JSON Web Token) is the format specification.
* JWS (JSON Web Signature) is a JWT whose payload is digitally signed, ensuring signature verification.

#### Q47: Why is `@Column(insertable = false, updatable = false)` placed on `created_at`?
It delegates responsibility for setting timestamp values to MySQL's database-level defaults (`DEFAULT CURRENT_TIMESTAMP`), preventing Java overrides.

#### Q48: Why do we write `@RestControllerAdvice`?
It is a meta-annotation that registers a global exception handling class as a controller helper.

#### Q49: What is a circular dependency?
A circular dependency occurs when Bean A depends on Bean B, and Bean B simultaneously depends on Bean A. This prevents Spring from instantiating either bean during context startup.

#### Q50: How do we secure public routes in Spring Security?
By specifying `.requestMatchers("/api/auth/**").permitAll()` in the `SecurityFilterChain` bean.

---

## 6. Key Developer Interview Q&As (Project-Specific Deep Dives)

#### Q51: How is the greedy debt-simplification algorithm designed, and is it always mathematically optimal?
The algorithm is a greedy heuristic that pairs the largest creditor with the largest debtor on every step. It runs in $O(N \log N)$ time (due to sorting) and resolves all debts in at most $N-1$ transactions.
* **Optimality**: While it minimizes the *number* of transactions, it is not mathematically guaranteed to yield the *absolute global minimum* number of transactions. Finding the absolute minimum transaction count is an NP-Complete problem, reducible to the **Subset Sum Problem** (we would have to search for subsets of users whose balances sum to exactly zero to isolate sub-settlements). The greedy approach is highly efficient, runs in sub-millisecond times, and is the industry-standard heuristic used in tools like Splitwise.

#### Q52: Why run the debt simplification in-memory on every request instead of saving net balances in SQL tables?
If balances were stored in tables, any addition, deletion, or modification of an expense would require updating the balance table for all group members, introducing locking bottlenecks. Running it in-memory via database sum aggregates (`SUM(share)` and `SUM(total_amount)`) pushed to DB query optimization plans scales cleanly and performs at sub-millisecond speeds.

#### Q53: How does the system ensure precision in monetary operations? Why not double/float?
Double and float values use binary floating-point representation, which results in cumulative precision loss (e.g. `0.1 + 0.2` becomes `0.30000000000000004`). We use `BigDecimal` for all calculations, setting the scale explicitly to 2 decimal places with `RoundingMode.HALF_UP` to prevent drift.

#### Q54: Explain how equal split division remainders are handled in the codebase.
Dividing a sum like ₹100.00 among 3 people equals ₹33.33 each, leaving a ₹0.01 remainder. To prevent this leak, the code calculates the base share (`100.00 / 3 = 33.33`), checks the remainder (`100.00 - 33.33 * 3 = 0.01`), and adds the remainder to the first member's split (so the first user gets ₹33.34 and the rest get ₹33.33).

#### Q55: Where does the security filter sit, and how does JWT verification prevent database lookup overhead?
`JwtFilter` extends `OncePerRequestFilter` and is added before the `UsernamePasswordAuthenticationFilter` in the Security Filter Chain. To prevent scanning the database on every REST call to verify if the user exists, we extract the username/email from the JWT payload claims and inspect the token's expiration date. Since the JWT is signed with our secret key, a verified signature guarantees the integrity and validity of the claims without making a database roundtrip.

#### Q56: Explain the DB relationships in the schema and why they are mapped as LAZY.
We have a bi-directional one-to-many relationship (e.g., one Group has many GroupMembers, one User has many GroupMembers). All `@ManyToOne` and `@OneToMany` relationships are mapped with `FetchType.LAZY`. This avoids the N+1 select problem by only loading related entities when they are explicitly requested (e.g. calling `group.getMembers()`).

#### Q57: How does the server handle Cross-Origin Resource Sharing (CORS)?
We configured a custom `CorsConfigurationSource` bean in `SecurityConfig.java` allowing all headers, HTTP methods, credentials, and origins. This prevents cross-origin blocks when deploying the frontend and backend on separate servers or ports.

#### Q58: How does the server handle validation exceptions dynamically?
We implemented a Controller Advice class using `@RestControllerAdvice` and `@ExceptionHandler(MethodArgumentNotValidException.class)`. It parses binding results to extract field-level errors and returns a 400 Bad Request response containing a JSON map of field names to validation constraints.

#### Q59: Explain the database check when leaving a group and why it is critical.
When a user leaves a group, the backend calls `BalanceService.calculateNetBalances(groupId)`. If the user's balance is non-zero, it blocks the deletion by throwing an error. This prevents data loss and integrity holes.

#### Q60: Explain how ModelMapper or manual mapping DTOs protects the API layer.
Direct serialization of JPA entities can result in circular reference loops (infinite JSON serialization loop) due to bi-directional mapping. DTOs break these loops, prevent exposing sensitive fields (like password hashes), and decouple database schemas from API schemas.

---

## 7. How to Run Locally

### Prerequisites
1. **Java 17** (JDK) and **Maven** installed.
2. **MySQL 8** running locally with database `splitease`.

### Step 1: Set up MySQL Database
Create the database and apply the table schemas:
```bash
mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS splitease;"
mysql -u root -padmin splitease < schema.sql
```

### Step 2: Build & Package the Jar
Build the production package using Maven:
```bash
mvn clean package
```

### Step 3: Run the Application
Start the Spring Boot server:
```bash
java -jar target/splitease-0.0.1-SNAPSHOT.jar
```
The server will start up on `http://localhost:8080/index.html`. You can open this URL in your browser to test the full expense splitting application.

