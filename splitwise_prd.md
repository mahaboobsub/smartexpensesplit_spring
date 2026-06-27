# SplitEase — Smart Expense Splitter

Stack: HTML + CSS + Tailwind CSS + Vanilla JS (frontend) · Spring Boot + Spring Security + JWT (backend) · MySQL 8 · Maven · Java 17

---

## 1. Problem statement

When a group of friends or roommates share expenses (rent, groceries, trips), figuring out who owes whom becomes messy. Naive splits create a web of transactions — A pays B, B pays C, A pays C — when fewer settlements would clear everything. SplitEase calculates the **minimum number of transactions** needed to settle all debts in a group, not just a per-expense split.

That algorithm is the core interview differentiator. Everything else (auth, CRUD, UI) is scaffolding that supports it.

---

## 2. Goals / non-goals

**In scope:** JWT-based registration/login, group creation + invite, expense logging, debt-simplification algorithm, settlement tracking, per-group dashboard with balance summary.

**Out of scope (say this in interview — shows scoping judgment):** payment gateway integration, push notifications, currency conversion, receipt OCR, mobile app.

---

## 3. User roles

| Role | Can do |
|---|---|
| User (member) | Register, create groups, join groups, add expenses, view balances, mark settlements |
| Group Admin | All of the above + remove members, delete group expenses |

Group admin = whoever created the group. No separate app-level admin role needed for this scope.

---

## 4. Functional requirements

### Auth
- Register: name, email, password (≥ 8 chars)
- Login: returns a signed JWT (stored in `localStorage` on client)
- All API endpoints except `/api/auth/**` require `Authorization: Bearer <token>` header
- Spring Security validates the token on every request via a `JwtFilter` (extends `OncePerRequestFilter`)
- No session, no cookie — stateless REST

### Groups
- Create a group: name, optional description
- Invite members by email (they must already be registered)
- View all groups the logged-in user belongs to
- Leave a group (only if your balance is zero — enforced server-side)

### Expenses
- Add an expense to a group: description, total amount, paid-by (a group member), split among (subset or all members), date
- Split type: **equal split only** for this scope (equal split among selected members) — the algorithm handles the complexity, not the split type
- View all expenses in a group, sorted by date
- Delete an expense (group admin only) — recomputes balances on delete

### Debt simplification (the core feature)
- On every balance view, run the simplification algorithm across all expenses in the group
- Algorithm output: a minimal list of `{ from, to, amount }` transactions that settles everyone to zero
- Example: if A owes B ₹200, B owes C ₹200 — output is just "A pays C ₹200" (1 transaction, not 2)
- This runs in-memory on the backend, not stored as SQL rows — it's recomputed on every `/api/groups/{id}/balances` call

### Settlements
- A user can mark a `{ from, to, amount }` transaction as "settled" (i.e., cash was exchanged)
- This creates a row in `settlements` table, which is treated as a reverse expense in the balance computation
- Settled transactions reduce the net balances before the algorithm runs

### Dashboard (per group)
- Net balance per member (positive = is owed, negative = owes)
- Simplified settlement list
- Full expense history
- "Who paid most" stat — good interview talking point on aggregation queries

---

## 5. Non-functional requirements

- JWT signed with HS256, secret from env var `JWT_SECRET`, expiry 24h
- Passwords hashed with BCrypt (Spring Security default)
- All SQL via Spring Data JPA / `@Query` with named parameters — no string-concatenated queries
- CORS configured in Spring Security to allow the frontend origin
- Input validation via `jakarta.validation` (`@NotBlank`, `@Email`, `@Positive`) on all request DTOs — return `400` with field-level error messages, not a generic 500
- Frontend renders all server data via DOM APIs (`.textContent`, `.createElement`) — no `innerHTML` with user data to avoid XSS

---

## 6. Tech stack

| Layer | Choice |
|---|---|
| Frontend | HTML5, Tailwind CSS (CDN), Vanilla JS (fetch API) |
| Backend | Spring Boot 3.x, Spring Security 6, Spring Data JPA |
| Auth | JWT (io.jsonwebtoken / jjwt 0.12.x) |
| ORM | Hibernate via Spring Data JPA |
| DB | MySQL 8 |
| Build | Maven, Java 17 |
| Extra | Lombok (reduce boilerplate), ModelMapper or manual DTOs |

---

## 7. Maven key dependencies (pom.xml)

| Purpose | artifactId | Notes |
|---|---|---|
| Web layer | spring-boot-starter-web | REST controllers |
| Security | spring-boot-starter-security | Filter chain, BCrypt |
| JPA | spring-boot-starter-data-jpa | Hibernate, repositories |
| Validation | spring-boot-starter-validation | DTO validation |
| MySQL | mysql-connector-java | runtime scope |
| JWT | jjwt-api, jjwt-impl, jjwt-jackson | version 0.12.x |
| Lombok | lombok | optional |
| DevTools | spring-boot-devtools | optional, dev only |

```xml
<properties>
  <java.version>17</java.version>
</properties>
```

---

## 8. Database schema

```sql
CREATE TABLE users (
  user_id   INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email     VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE groups (
  group_id    INT AUTO_INCREMENT PRIMARY KEY,
  group_name  VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  created_by  INT NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE group_members (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  group_id  INT NOT NULL,
  user_id   INT NOT NULL,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_group_user (group_id, user_id),
  FOREIGN KEY (group_id) REFERENCES groups(group_id),
  FOREIGN KEY (user_id)  REFERENCES users(user_id)
);

CREATE TABLE expenses (
  expense_id  INT AUTO_INCREMENT PRIMARY KEY,
  group_id    INT NOT NULL,
  paid_by     INT NOT NULL,
  description VARCHAR(200) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  expense_date DATE NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES groups(group_id),
  FOREIGN KEY (paid_by)  REFERENCES users(user_id)
);

CREATE TABLE expense_splits (
  split_id   INT AUTO_INCREMENT PRIMARY KEY,
  expense_id INT NOT NULL,
  user_id    INT NOT NULL,
  share      DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (expense_id) REFERENCES expenses(expense_id),
  FOREIGN KEY (user_id)    REFERENCES users(user_id)
);

CREATE TABLE settlements (
  settlement_id INT AUTO_INCREMENT PRIMARY KEY,
  group_id      INT NOT NULL,
  paid_by       INT NOT NULL,
  paid_to       INT NOT NULL,
  amount        DECIMAL(10,2) NOT NULL,
  note          VARCHAR(200),
  settled_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES groups(group_id),
  FOREIGN KEY (paid_by)  REFERENCES users(user_id),
  FOREIGN KEY (paid_to)  REFERENCES users(user_id)
);
```

---

## 9. REST API map

### Auth  (`/api/auth/**` — public)
| Method | URL | Body | Response |
|---|---|---|---|
| POST | /api/auth/register | `{name, email, password}` | `{message}` |
| POST | /api/auth/login | `{email, password}` | `{token, name, userId}` |

### Groups  (all require JWT)
| Method | URL | Body | Response |
|---|---|---|---|
| POST | /api/groups | `{name, description}` | GroupDTO |
| GET | /api/groups | — | List\<GroupDTO\> (caller's groups) |
| GET | /api/groups/{id} | — | GroupDTO + members |
| POST | /api/groups/{id}/members | `{email}` | `{message}` |
| DELETE | /api/groups/{id}/members/me | — | `{message}` (balance-zero check) |

### Expenses
| Method | URL | Body | Response |
|---|---|---|---|
| POST | /api/groups/{id}/expenses | `{description, totalAmount, paidBy, splitAmong[], date}` | ExpenseDTO |
| GET | /api/groups/{id}/expenses | — | List\<ExpenseDTO\> |
| DELETE | /api/groups/{id}/expenses/{expId} | — | `{message}` (admin only) |

### Balances + Settlements
| Method | URL | Body | Response |
|---|---|---|---|
| GET | /api/groups/{id}/balances | — | `{netBalances[], simplifiedSettlements[]}` |
| POST | /api/groups/{id}/settlements | `{from, to, amount, note}` | SettlementDTO |
| GET | /api/groups/{id}/settlements | — | List\<SettlementDTO\> |

---

## 10. Debt simplification algorithm

This is the piece to be able to explain and whiteboard in an interview.

**Concept:** compute each person's net balance across all expenses and settlements, then greedily pair the biggest creditor with the biggest debtor until all balances are zero.

```java
public List<Transaction> simplify(Map<Integer, BigDecimal> netBalance) {
    // netBalance: userId → net (positive = owed to them, negative = they owe)
    // Filter out zero balances
    List<BigDecimal> credits = new ArrayList<>();  // positive values
    List<Integer>    creditIds = new ArrayList<>();
    List<BigDecimal> debits  = new ArrayList<>();  // positive values (abs of negative)
    List<Integer>    debitIds  = new ArrayList<>();

    for (Map.Entry<Integer, BigDecimal> e : netBalance.entrySet()) {
        int cmp = e.getValue().compareTo(BigDecimal.ZERO);
        if (cmp > 0) { credits.add(e.getValue()); creditIds.add(e.getKey()); }
        if (cmp < 0) { debits.add(e.getValue().abs()); debitIds.add(e.getKey()); }
    }

    List<Transaction> result = new ArrayList<>();
    int i = 0, j = 0;
    while (i < credits.size() && j < debits.size()) {
        BigDecimal settle = credits.get(i).min(debits.get(j));
        result.add(new Transaction(debitIds.get(j), creditIds.get(i), settle));
        credits.set(i, credits.get(i).subtract(settle));
        debits.set(j,  debits.get(j).subtract(settle));
        if (credits.get(i).compareTo(BigDecimal.ZERO) == 0) i++;
        if (debits.get(j).compareTo(BigDecimal.ZERO)  == 0) j++;
    }
    return result;
}
```

**Net balance computation** (before algorithm runs):
```
net[user] = Σ(expenses where paid_by = user → total paid)
          - Σ(expense_splits where user_id = user → share owed)
          + Σ(settlements where paid_by = user → amount)   // user paid someone
          - Σ(settlements where paid_to = user → amount)   // someone paid user back
```

This SQL aggregation runs per group on every `/balances` request — not persisted, always fresh.

---

## 11. Spring Security + JWT setup

```
SecurityFilterChain:
  - Stateless (no session)
  - Permit: POST /api/auth/register, POST /api/auth/login, static assets
  - All others: authenticated
  - Add JwtFilter before UsernamePasswordAuthenticationFilter

JwtFilter (OncePerRequestFilter):
  1. Read Authorization header
  2. Extract + validate token (signature, expiry)
  3. Load UserDetails from DB by subject (email)
  4. Set Authentication in SecurityContext

JwtUtil:
  - generateToken(UserDetails) → signed JWT, 24h expiry, secret from env
  - extractEmail(token) → subject
  - isTokenValid(token, userDetails) → boolean
```

Never return the JWT from a GET endpoint. Issue it only on successful POST `/api/auth/login`.

---

## 12. Folder structure

```
splitease/
├── pom.xml
└── src/main/
    ├── java/com/splitease/
    │   ├── config/        SecurityConfig.java, CorsConfig.java
    │   ├── controller/    AuthController, GroupController, ExpenseController,
    │   │                  BalanceController, SettlementController
    │   ├── service/       UserService, GroupService, ExpenseService,
    │   │                  BalanceService (owns the algorithm), SettlementService
    │   ├── repository/    UserRepo, GroupRepo, GroupMemberRepo,
    │   │                  ExpenseRepo, ExpenseSplitRepo, SettlementRepo
    │   ├── model/         User, Group, GroupMember, Expense, ExpenseSplit, Settlement
    │   ├── dto/           request/ and response/ subpackages per feature
    │   ├── security/      JwtFilter.java, JwtUtil.java, UserDetailsServiceImpl.java
    │   └── algorithm/     DebtSimplifier.java, Transaction.java
    └── resources/
        └── application.properties
```

---

## 13. application.properties (env-var driven)

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:splitease}?useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASS:}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
app.jwt.secret=${JWT_SECRET:local-dev-secret-change-in-prod}
app.jwt.expiry-ms=86400000
```

Use `ddl-auto=validate` (not `create` or `update`) in dev after the schema is manually created — so you catch entity-column mismatches early instead of letting Hibernate silently alter your DB.

---

## 14. Frontend pages

| Page | File | What it does |
|---|---|---|
| Landing | index.html | Hero, features, login/register CTA |
| Login | login.html | Email + password, stores JWT, redirects by role |
| Register | register.html | Name, email, password, confirm-password |
| Dashboard | dashboard.html | All groups of logged-in user + "Create group" |
| Group detail | group.html?id=X | Members, expense list, balance cards, settlement list |
| Add expense | add-expense.html?groupId=X | Form: description, amount, paid-by, split-among checkboxes, date |

All pages check JWT in `localStorage` on load; if missing or expired (`401` from API), redirect to `login.html`. All server data written via `.textContent` or `createElement`, never `innerHTML` with user data.

Tailwind design: dark theme (`bg-gray-900`), green accent (`#22c55e` — money/positive balance), red for debt (`#ef4444`), glass cards (`bg-white/5 backdrop-blur-md border border-white/10`).

---

## 15. Build order (5 phases)

1. **Foundation** — Spring Boot skeleton, MySQL schema, `application.properties`, DB connection verified
2. **Auth** — `User` entity, `UserRepo`, `UserService`, `AuthController`, `JwtUtil`, `JwtFilter`, `SecurityConfig`, register/login tested via Postman
3. **Groups + Expenses** — entities, repos, services, controllers for groups, group-members, expenses, expense-splits; all Postman-tested
4. **Algorithm + Balances** — `DebtSimplifier`, `BalanceService` (net balance SQL + in-memory algorithm), `BalanceController`, `SettlementController`; test with 3–4 member scenario manually
5. **Frontend** — all 6 HTML pages with Tailwind, fetch-based API calls, JWT header injection, dashboard rendering

Never start the frontend before Phase 4 is Postman-tested — UI bugs and backend bugs at the same time are very hard to isolate.

---

## 16. Deployment on Railway

Two Railway services: Spring Boot app + MySQL database service.

**Dockerfile:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/splitease-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Spring Boot reads `PORT` automatically via `server.port=${PORT:8080}` — no `entrypoint.sh` port-patching needed unlike Tomcat. That's a meaningful simplification vs. the BloodConnect setup.

Set these env vars on the Railway app service:
```
DB_HOST      → ${{MySQL.MYSQLHOST}}
DB_PORT      → ${{MySQL.MYSQLPORT}}
DB_NAME      → ${{MySQL.MYSQLDATABASE}}
DB_USER      → ${{MySQL.MYSQLUSER}}
DB_PASS      → ${{MySQL.MYSQLPASSWORD}}
JWT_SECRET   → (generate a random 64-char string)
```

---

## 17. Interview talking points

- **Debt simplification algorithm** — greedy net-balance approach; explain why naive pairwise tracking creates O(n²) transactions but this produces at most O(n-1)
- **Stateless JWT auth** vs. session-based — why it matters for horizontal scaling
- Spring Security filter chain — `OncePerRequestFilter`, where JwtFilter sits, how `SecurityContext` works
- `ddl-auto=validate` instead of `create`/`update` in dev — shows understanding of schema ownership
- `BalanceService` is pure Java (no DB write) — the algorithm runs in-memory, making it fast and independently testable with unit tests
- `expense_splits` table normalizes the share per person per expense — this is what lets you support unequal splits later with zero schema changes
- Balance = SQL aggregation + algorithm — neither alone is enough, and you can explain why
- Settlement as a "reverse expense" in the net balance formula — elegant and avoids a separate ledger
- Leave-group enforces zero-balance check server-side — prevents data integrity holes
- Same JAR runs locally and on Railway (`${PORT:8080}` fallback) — dev/prod parity without any config file changes