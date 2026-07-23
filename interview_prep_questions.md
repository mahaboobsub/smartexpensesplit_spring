# SplitEase Technical Interview Preparation: REST API, SQL & Core Implementation

This guide contains **15 highly detailed, codebase-specific interview questions and answers** designed to showcase your knowledge of the SplitEase architecture, database design, and algorithmic choices.

---

## Part 1: REST API & Architecture

### Q1: Describe the API Endpoint Design for SplitEase. How are groups, expenses, and settlements mapped RESTfully?
**Answer:**
SplitEase uses nested, resource-oriented endpoint mappings in Spring MVC controllers:
- **Authentication**: `/api/auth/register` and `/api/auth/login` (unsecured).
- **Groups**: `/api/groups` (GET to list, POST to create) and `/api/groups/{id}/invite` (POST to add members).
- **Expenses**: `/api/groups/{groupId}/expenses` (POST to add an expense, GET to list group expenses).
- **Balances/Settlements**: `/api/groups/{groupId}/balances` (GETs net balances and suggested simplified transactions) and `/api/groups/{groupId}/settlements` (POST to record a settlement payment).

**Code Reference:**
- [GroupController.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/controller/GroupController.java)
- [ExpenseController.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/controller/ExpenseController.java)

---

### Q2: Why did you separate JPA Entity classes from Request/Response DTOs in this project? What problem does this solve?
**Answer:**
Separating JPA Entities (e.g., [User.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/model/User.java)) from DTOs (e.g., [GroupResponse.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/dto/response/GroupResponse.java)) serves two critical purposes:
1. **Prevents Infinite JSON Recursion**: JPA relationships are bidirectional. For instance, a `User` has a list of `groups`, and a `Group` has a list of `members` (who are `Users`). Attempting to serialize these directly results in a stack overflow. DTOs flatten these structures into plain data objects containing only IDs and basic primitives.
2. **Security & Validation Isolation**: DTOs enforce validation rules (using `@NotBlank`, `@Size`) specific to the incoming request payload without polluting the database model rules. They also ensure internal database keys or user password hashes are never serialized and sent over HTTP.

---

### Q3: How is authentication handled on the REST layer to keep the application stateless? Walk through the authentication filter workflow.
**Answer:**
The application uses stateless JWT-based authentication.
1. The client logs in via `/api/auth/login` and receives a JWT.
2. For subsequent requests, the client adds the `Authorization: Bearer <JWT>` header.
3. Every request passes through [JwtFilter.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/security/JwtFilter.java) (which extends `OncePerRequestFilter`).
4. Inside `doFilterInternal`, the filter:
   - Extracts the token from the header.
   - Parses the token to extract the user's `email`.
   - Validates the token status using [JwtUtil.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/security/JwtUtil.java).
   - Loads the user details via `UserDetailsService`.
   - Sets a `UsernamePasswordAuthenticationToken` in Spring Security's `SecurityContextHolder`.
5. Spring Security is configured with `SessionCreationPolicy.STATELESS`, meaning it does not create a `HttpSession`. Authentication must be re-validated on every request using the JWT header.

---

### Q4: How is Cross-Origin Resource Sharing (CORS) configured in the Spring Security filter chain to let the front-end interact with REST endpoints?
**Answer:**
CORS is explicitly configured in [SecurityConfig.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/config/SecurityConfig.java#L58-L70):
- A `CorsConfigurationSource` bean is defined that registers paths under `/**`.
- It sets `allowedOriginPatterns` to `*` and explicitly allows HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`).
- It permits headers like `Authorization` and `Content-Type`.
- Crucially, it exposes the `Authorization` header so the client's vanilla JavaScript `fetch()` API can read and store the JWT in localStorage during login.

---

## Part 2: SQL & JPA Mapping

### Q5: "groups" is a reserved keyword in MySQL. How did you resolve this conflict globally in JPA and locally in the database schema?
**Answer:**
Using `groups` as a table name causes syntax exceptions because `GROUP` is a reserved SQL keyword used for aggregates.
1. **In SQL**: In the [schema.sql](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/schema.sql#L9) file, we escaped the table name using SQL backticks: ``CREATE TABLE IF NOT EXISTS `groups```.
2. **In Hibernate/JPA**: Instead of manually adding `@Table(name="`groups`")` annotations to all related entity classes, we resolved it globally in [application.properties](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/resources/application.properties) by setting:
   ```properties
   spring.jpa.properties.hibernate.globally_quoted_identifiers=true
   ```
   This configuration forces Hibernate to automatically wrap all generated SQL identifiers (table names, column names) in backticks, preventing any keyword collision globally.

---

### Q6: Walk through the database schema design of SplitEase. How are users, groups, expenses, splits, and settlements related?
**Answer:**
The schema is designed as a relational structure of 6 key tables:
- **`users`**: Stores user profiles.
- **`groups`**: Has a many-to-one relationship with `users` (via `created_by` as foreign key).
- **`group_members`**: A join table mapping `users` to `groups` (Many-to-Many relationship) with a unique key constraint on `(group_id, user_id)` to prevent double invites.
- **`expenses`**: Tracks group expenses. It has a foreign key to `groups` (`group_id`) and a foreign key to `users` (`paid_by`) indicating who paid for the expense.
- **`expense_splits`**: Relates members to expenses. Each record indicates what `user_id` owes what decimal `share` of a given `expense_id`.
- **`settlements`**: Records debt clearing. It contains `group_id`, `paid_by` (debtor), `paid_to` (creditor), and `amount`.

**SQL Reference:** See [schema.sql](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/schema.sql).

---

### Q7: In the repository layer, we compute net balances using JPQL instead of loading all data into Java memory. How are these JPQL aggregations written?
**Answer:**
Loading all database rows to calculate net balances inside Java is highly inefficient. Instead, we use JPQL `@Query` projections in the repository layer to perform group aggregates:
1. **Sum of expenses paid by user**:
   ```java
   @Query("SELECT e.paidBy.userId, SUM(e.totalAmount) FROM Expense e WHERE e.group.groupId = :groupId GROUP BY e.paidBy.userId")
   List<Object[]> sumExpensesPaidByGroupMembers(@Param("groupId") Integer groupId);
   ```
2. **Sum of expense shares owed by user**:
   ```java
   @Query("SELECT es.user.userId, SUM(es.share) FROM ExpenseSplit es WHERE es.expense.group.groupId = :groupId GROUP BY es.user.userId")
   List<Object[]> sumExpenseSplitsForGroupMembers(@Param("groupId") Integer groupId);
   ```

These aggregate results are returned as list of raw Object arrays (`List<Object[]>`) mapping user ID to sum, which the service layer aggregates in-memory to compute the final net balance: `Net = (Paid + Settlements Paid) - (Owed + Settlements Received)`.

**Code Reference:** See [ExpenseRepo.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/repository/ExpenseRepo.java#L15-L16) and [ExpenseSplitRepo.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/repository/ExpenseSplitRepo.java#L15-L16).

---

### Q8: Explain the `ON DELETE CASCADE` implementation in this schema. What happens when an expense is deleted by the group administrator?
**Answer:**
We implement referential integrity cascades both in MySQL and Hibernate configurations:
1. **In SQL DDL**: All foreign key declarations (e.g., in `expense_splits`) are created with `ON DELETE CASCADE`. If a parent `expenses` record is deleted, the DBMS automatically removes all related rows in `expense_splits`.
2. **In Service Layer**: Before deleting an expense, the application first deletes the split mappings explicitly to prevent Hibernate caching mismatch:
   ```java
   List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseExpenseId(expenseId);
   expenseSplitRepo.deleteAll(splits);
   expenseRepo.delete(expense);
   ```
   This ensures JPA session state stays in sync with the database and avoids orphaned associations.

**Code Reference:** See [ExpenseService.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/service/ExpenseService.java#L137-L142).

---

## Part 3: Core Implementation & Algorithm

### Q9: Explain the Debt-Simplification Algorithm used in SplitEase. What is the algorithm's time complexity?
**Answer:**
The debt simplification algorithm in [DebtSimplifier.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/algorithm/DebtSimplifier.java) is a **greedy two-pointer settlement algorithm**.
1. **Process**:
   - Split group members into two lists: **Creditors** (members with net balance > 0) and **Debtors** (members with net balance < 0).
   - Sort or iterate through both lists.
   - At each step, take the largest creditor (who is owed the most) and the largest debtor (who owes the most).
   - Create a transaction where the debtor pays the creditor the minimum of the two balances: `settle = min(creditVal, debitVal)`.
   - Update both balances by subtracting the settled amount.
   - Remove whichever member has their balance reduced to zero and move the pointer.
2. **Complexity**:
   - Since every transaction clears at least one user's balance, the loop runs at most $N-1$ times (where $N$ is the number of group members with non-zero balances).
   - Thus, the algorithm has a time complexity of $O(N \log N)$ (due to grouping and listing) and produces the minimum possible number of transaction settlements.

---

### Q10: Why did you choose `BigDecimal` over `double` or `float` for representing amounts and shares? How are rounding errors handled?
**Answer:**
Floating-point types (`double`/`float`) use binary representation (IEEE 754), which cannot represent numbers like `0.1` or `0.2` with exact precision. This leads to cumulative rounding errors (e.g., sums ending in `.000000000000004`), which is unacceptable in financial software.
- `BigDecimal` represents numbers as arbitrary-precision decimals, preventing representation errors.
- Every currency calculation specifies an explicit scale and rounding mode, e.g.:
  `val.setScale(2, RoundingMode.HALF_UP)` (standard round-to-nearest with ties rounding up).

---

### Q11: How does SplitEase handle division remainders (penny splits)? (e.g., split ₹100.00 equally among 3 users).
**Answer:**
When splitting an amount equally among $M$ users, direct division might leave a remaining fraction of a penny. For example, dividing `100.00` among 3 users yields `33.33` each, which totals `99.99`. This leaves `0.01` unaccounted for.
SplitEase resolves this by:
1. Calculating the equal integer-downwards share: `equalShare = totalAmount / M` rounded down to 2 decimal places.
2. Calculating the remainder: `remainder = totalAmount - (equalShare * M)`.
3. Adding the remainder to the first user in the split list:
   ```java
   BigDecimal share = equalShare;
   if (i == 0) {
       share = share.add(remainder);
   }
   ```
This guarantees that the sum of the shares stored in `expense_splits` matches the parent `total_amount` exactly.

**Code Reference:** See [ExpenseService.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/service/ExpenseService.java#L77-L90).

---

### Q12: How was the circular dependency between `GroupService` and `BalanceService` resolved during context startup?
**Answer:**
**The Problem:**
- [GroupService.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/service/GroupService.java) needs to call `BalanceService` inside its `leaveGroup` method to verify if a user's net balance is zero before letting them leave.
- `BalanceService.java` needs `GroupRepo` and `GroupMemberRepo` to query groups and members. If any other method in `BalanceService` delegates or makes reference to `GroupService` methods, Spring Boot throws a circular reference injection error on startup.
- In general, circular references happen when bean A depends on bean B, and bean B depends on bean A.

**The Solution:**
We resolved the loop by adding the `@Lazy` annotation on the `BalanceService` injection in `GroupService`:
```java
@Autowired
@Lazy
private BalanceService balanceService;
```
This tells Spring Boot to inject a lazy-initialization proxy bean instead of initializing `BalanceService` immediately during startup, resolving the circular loop.

---

### Q13: How does the server enforce the business constraint that a user cannot leave a group unless their net balance is exactly zero?
**Answer:**
We perform a validation check in [GroupService.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/service/GroupService.java#L118-L124):
1. When a user requests to leave, we retrieve all net balances in the group via `balanceService.calculateNetBalances(groupId)`.
2. We extract the user's specific balance.
3. We compare it to zero using `compareTo()` instead of `equals()` (since `equals` checks scale: `0.0` is not equal to `0.00`, whereas `compareTo` checks numeric value):
   ```java
   if (userBalance.compareTo(BigDecimal.ZERO) != 0) {
       throw new IllegalArgumentException("Cannot leave group: Your net balance must be zero. Current balance: " + userBalance);
   }
   ```
4. If it's non-zero, an exception is thrown and the database transaction is rolled back, preventing the user from leaving.

---

### Q14: Explain the difference between `@Transactional(readOnly = true)` and standard `@Transactional` annotations used in SplitEase services.
**Answer:**
- **`@Transactional(readOnly = true)`**: Used on read operations like [BalanceService.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/service/BalanceService.java#L21-L22). It optimizes Hibernate performance by disabling dirty checking (Hibernate does not need to check if entities have changed to flush them to the DB) and allowing the underlying database driver to optimize queries (e.g., routing reads to read-replicas).
- **`@Transactional`**: Used on write operations like [ExpenseService.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/service/ExpenseService.java#L18). It ensures that if any part of the expense creation (saving the expense entity, calculating splits, writing multiple `expense_splits` records) fails, the entire transaction rolls back, keeping the database in a consistent state.

---

### Q15: How does the application secure REST endpoints? Which endpoints are secured, and how is security bypassed for resources?
**Answer:**
We configure the request matchers in [SecurityConfig.java](file:///c:/Users/Lenovo/Desktop/smart_expense_splitter_sp/src/main/java/com/splitease/config/SecurityConfig.java#L47-L52):
- **PermitAll (Unsecured)**: 
  - API endpoints: `/api/auth/**` (Registration/Login).
  - Front-end static assets: HTML pages (`/index.html`, `/login.html`, etc.), styling sheets, images, and script assets (`/css/**`, `/js/**`).
- **Authenticated**:
  - All other API endpoints under `/api/**` (e.g., `/api/groups/**`, `/api/groups/{id}/expenses/**`) require authentication.
- **Enforcement**: Any unauthorized request to a protected REST API is blocked and returns a `401 Unauthorized` status code. The authentication is checked using `JwtFilter` before standard authentication filters.
