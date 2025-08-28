# Bookhouse CLI – Technical Design & Architecture

> **Audience:** Interview reviewers and engineers  
> **Document Type:** Comprehensive technical design for a Spring Boot + Spring Shell interactive CLI  
> **Project Name:** `bookhouse` (interactive library management CLI)  
> **Tone:** Plain, direct, technical

---

## 1) Purpose & Context

The **Bookhouse CLI** is a small but production-minded interactive shell application for basic library management flows: managing users and books, borrowing/returning, waitlists, and reports. The original assignment was intentionally small, but the implementation demonstrates **clean modular structure**, **security (RBAC)**, **auditability**, **externalized configuration & i18n-readiness**, **sane data consistency (Single Source of Truth)**, and a **pleasant REPL UX** via Spring Shell. Where appropriate, forward-looking placeholders (e.g., **MCP/LLM** integration) are included to emphasize extensibility.

---

## 2) Requirements & Compliance Strategy

A `requirements.md` from the interview describes core features. Bookhouse:

- **Implements the base requirements**; and
- **Adds enhancements** (security, audit log, UX polish, reports, copies, waitlist conveniences, etc.).
- Command names can differ from sample names, but **functional parity** is maintained.

### 2.1 Requirements Mapping Template

> Replace with concrete rows for your submission. This clarifies parity even if command names differ.

| Req ID | Spec Title / Intent | Spec Snippet | Implemented Command / Flow | Notes |
|---|---|---|---|---|
| REQ-1 | List Books | “List all books …” | `book list [--filter …]` | Colored headers; wildcard `*` supported |
| REQ-2 | Add Book | “Add a new book …” | `book add <bookId> [<copies>]` | ADMIN only; copies default to 1; cumulative |
| REQ-3 | Borrow Book | “Borrow a book …” | `loan borrow --book <id>` | Authenticated users only; SSOT checks |
| REQ-4 | Return Book | “Return a book …” | `loan return (--book <id> | --loan <id>)` | Flexible input; see §10.3 |
| REQ-5 | Waitlist | “Waitlist when unavailable …” | `waitlist add --book <id>` / `waitlist cancel --book <id>` | Auto-cancel on successful borrow |
| REQ-6 | Reports | “Basic reports …” | `report loans [--user <id>|*]` | RBAC: ADMIN=all; USER=self |

> **Submission note:** “All base requirements are implemented. Additional features (RBAC, audit logging, UX via Spring Shell, reports framework) were added deliberately.”

---

## 3) Architecture Overview

### 3.1 High-Level Flow

```
User (Terminal)
    │
    ▼
Spring Shell (Commands; help/completions/prompt/colors; JLine history)
    │
    ▼
Services (business logic; validations; atomic flows)
    │
    ▼
In-memory Store (SSOT: users, books, loans, waitlist — flat collections)
    │
    ├─► Audit Logger (JSON lines → session/audit file; shipper-ready)
    └─► App Logger (Log4j2 RollingFile; no console logs)
```

### 3.2 Module / Package Layout

- `com.bookhouse.commands` — **Gateway** to shell commands (API). Separate files per domain for granularity:
  - `AdminCommands`, `BookCommands`, `AuthCommands`, `LoanCommands`, `WaitlistCommands`, `ReportCommands`.
- `com.bookhouse.service` — **Business services** (`AuthService`, `UserService`, `BookService`, `LoanService`, `WaitlistService`, `ReportService`). Transactional/atomic flows live here.
- `com.bookhouse.models` — **Data modeling** classes (`User`, `Book`, `Loan`, `WaitlistEntry`, `Result`, etc.).
- `com.bookhouse.security` — **Spring Security** config; role mapping; availability.
- `com.bookhouse.shell` — **Spring Shell UX customizations** (prompt with current user, color helpers, value providers).
- `com.bookhouse.core` — **Utilities** (global constants, print/util, message formatter, audit logger, persistence IO helpers).

---

## 4) Technology Considerations

### 4.1 Stack & Rationale

- **Spring Boot**: Mature, lightweight bootstrap; dependency injection; auto-config; testing support; easy maintenance; best practices friendly.
- **Spring Shell**: Out-of-the-box interactive shell, **help**, **auto-completion**, **history**, **colors**, **prompt customization**, **command availability**, **input validation**, **aliases** — avoids reinventing a REPL.
  - **Custom prompt** shows **current logged-in user** for context: e.g., `bookhouse(admin01)> `.
- **Spring Security**: Authentication/Authorization with **RBAC**; clean separation using annotations; no ad-hoc checks.
- **Maven**: Standardized build, dependency management; easy CI integration.
- **Minimal dependencies**: Spring Boot + Spring Shell + Spring Security + Log4j2 (and test libs).
- **GraalVM Native** (optional): Smaller footprint, fast startup if needed; enabled via build plugin and Spring AOT.

### 4.2 Example Maven Snippet

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
      <exclusion>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-logging</artifactId>
      </exclusion>
    </exclusions>
  </dependency>

  <dependency>
    <groupId>org.springframework.shell</groupId>
    <artifactId>spring-shell-starter</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
  </dependency>

  <!-- Test -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>

<!-- Optional: native images -->
<build>
  <plugins>
    <plugin>
      <groupId>org.graalvm.buildtools</groupId>
      <artifactId>native-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

---

## 5) Data Model & Single Source of Truth (SSOT)

### 5.1 Entities (in-memory, flat collections)

- `User(id, username, roles)`  
- `Book(id, title, author, totalCopies, availableCopies, …)`  
- `Loan(id, userId, bookId, borrowedAt, returnedAt?)`  
- `WaitlistEntry(id, userId, bookId, position, createdAt)`

> Implemented as **arrays/lists/maps** in memory; **no secondary in-memory indexes**.

### 5.2 SSOT Rationale

- **Always-consistent** single data source; avoids dual writes and drift.
- **Atomic operations** within service methods to keep state coherent (e.g., borrow updates availableCopies + creates Loan + cancels waitlist entry if applicable, in one unit).
- **Failsafe/rollback-safe** by ordering operations and performing validations upfront; on failure, state is unchanged.

### 5.3 Future Persistence

- When scale demands, **migrate to an external DB** and leverage **DB-native indexes/materialized views** — avoid DIY performance hacks.
- SSOT principle remains: the **DB** becomes the single truth.

### 5.4 Retrieval Strategy (Streams)

- All queries use **Java Streams** over the collections for **clear, efficient** filtering/sorting/mapping.
- Example:
```java
var myOpenLoans = loans.stream()
  .filter(l -> l.getUserId().equals(currentUserId))
  .filter(l -> l.getReturnedAt() == null)
  .sorted(Comparator.comparing(Loan::getBorrowedAt))
  .toList();
```

---

## 6) Security (Authentication & Authorization)

### 6.1 RBAC

- Roles: `ADMIN`, `USER` (extensible).
- **Checks** are **role-based** via Spring Security annotations — not by user-id string parsing.
- **Seeding** approach: for demo convenience, users whose IDs start with `admin*` are **seeded** with `ROLE_ADMIN`; all checks still rely on roles.

### 6.2 Access Policy

- **ADMIN only**: administrative commands — e.g., `book add`, `book copies add`, certain reports.
- **Authenticated users**: `loan borrow`, `loan return`, `waitlist add/cancel`, `report loans --user self`.
- **Public**: `whoami` (shows guest or user), `book list` (optional; controllable via profile).

### 6.3 Annotation Examples

```java
@PreAuthorize("hasRole('ADMIN')")
public Result addBook(BookSpec spec) { … }

@PreAuthorize("isAuthenticated()")
public Result borrow(@P("bookId") String bookId) { … }

@PreAuthorize("permitAll()")
public Result whoami() { … }
```

---

## 7) Commands & RBAC Matrix (Representative)

| Command | Synopsis | Access | Notes |
|---|---|---|---|
| `auth login --user <id>` | Log in | Public | Sets principal; updates prompt |
| `auth whoami` | Show current user | Public | Reflects in prompt |
| `book list [--filter <expr>|*]` | List books | Public | Wildcards for IDs/fields |
| `book add <bookId> [<copies>]` | Add book/copies | ADMIN | Copies default to 1; cumulative |
| `loan borrow --book <id>` | Borrow | USER | Auto-cancel waitlist if exists |
| `loan return (--book <id> | --loan <id>)` | Return | USER | Flexible; future multi-book by loanId |
| `waitlist add --book <id>` | Add to waitlist | USER | If available, notify immediately |
| `waitlist cancel --book <id>` | Cancel waitlist | USER | Explicit cancel supported |
| `report loans [--user <id>|self|*]` | Loan report | USER/ADMIN | ADMIN=all; USER=self |
| `exit` | Exit shell | Public | Persist audit entry |

---

## 8) Reports Framework

- **Consistent framework** for tabular outputs; helper methods unify headers, widths, alignment.
- **RBAC-aware**: ADMIN sees all; USER only sees their own records (`--user self` default).
- **Wildcards**: `*` or partials match user IDs or book IDs for easy filters.
- **Extensible**: New reports follow the same pattern (registration + service-provided rows).

---

## 9) UX (Spring Shell + JLine)

- **Help**: auto-generated command list; command-level & argument-level help.
- **Auto-completion**: based on command names and custom **ValueProviders** (e.g., known book IDs).
- **Colorized outputs**: ANSI styles for headers/success/warnings/errors.
- **Prompt customization**: shows **current user**: `bookhouse(admin01)>`.
- **History navigation**: **Up/Down arrows** (JLine history).
- **Aliases** & **short options**: friendly syntax.
- **Friendly errors**: technical stack traces stay in logs; users see clean messages.
- **Branding**: `banner.txt` (supports Unicode) and externalized prompt/header strings.
- **Optional parameters**: e.g., return by **book-id** or **loan-id**.
- **Highlighted variables**: message formatter **bolds** dynamic `{0}`, `{1}`, … placeholders for visibility.
- **Icons/Unicode**: permitted in messages for emphasis.

_Additional Spring Shell conveniences leveraged:_ `@ShellMethod`, `@ShellOption`, `@ShellMethodAvailability` (command enable/disable), custom ValueProviders for completions, logical command grouping.

---

## 10) Core Functional Features

### 10.1 Books with Copies
- `book add <bookId> [copies]` adds a new book or **tops up copies**; if `copies` absent, defaults to **1**.

### 10.2 Borrow/Return
- **Borrow** (`loan borrow --book <id>`): decrements `availableCopies`, creates `Loan(borrowedAt=now)`.
- **Return** (`loan return --book <id>` or `--loan <id>`): increments `availableCopies`, sets `returnedAt`.
- Friendly flexibility: return by loan ID or by book ID.

### 10.3 Waitlist Behavior
- **Auto-notify**: If a waitlisted book becomes available, the shell immediately displays a message: “Your waitlisted book `<id>` is now available; you can borrow it.” This also appears on login/prompt refresh until actioned.
- **Auto-cancel**: When a user borrows a book they had waitlisted, the relevant waitlist entry is **removed** automatically.
- **Manual cancel**: `waitlist cancel --book <id>`.

### 10.4 Availability Calculation
- A book is **available** if `availableCopies > 0`. Priority for waitlisted users is based on **queue position**. When copies free up, the earliest waitlist entries are next in line.

### 10.5 Loan Details
- Loans record **borrowed date** (and return date). This supports future **reminders/collections**.

### 10.6 Reports
- `report loans [--user self|<id>|*]` prints tabular rows, with headings and totals; **wildcard** filters supported.
- ADMIN can query all; USER sees own by default.

---

## 11) Observability / Logging / Audit

### 11.1 Log4j2 App Logging
- **File-only** logging: terminal output remains clean. No `ConsoleAppender`.
- **RollingFile** policy for size/time rotation.
- `bookhouse.log` is the canonical app log; includes command invocations and errors (including **invalid/unknown commands** captured by a global handler).

**`log4j2.xml` (minimal, file-only):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Appenders>
    <RollingFile name="AppFile"
                 fileName="logs/bookhouse.log"
                 filePattern="logs/bookhouse-%d{yyyy-MM-dd}-%i.log.gz">
      <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger - %msg%n"/>
      <Policies>
        <TimeBasedTriggeringPolicy interval="1"/>
        <SizeBasedTriggeringPolicy size="10 MB"/>
      </Policies>
    </RollingFile>
  </Appenders>
  <Loggers>
    <Root level="info">
      <AppenderRef ref="AppFile"/>
    </Root>
  </Loggers>
</Configuration>
```

### 11.2 Audit (Session) Log
- Every **activity** is written as structured JSON to a dedicated **audit** file (e.g., `logs/session.log`).
- Fields: timestamp, user-id, command, args, output summary, status, duration, traceId; **PII masking** can be added later.

```json
{
  "ts": "2025-08-28T10:21:34.567Z",
  "user": "admin01",
  "command": "loan borrow --book BK-101",
  "args": {"book":"BK-101"},
  "status": "SUCCESS",
  "durationMs": 12,
  "traceId": "c9e0d2f3d4"
}
```

- Design is **shipper-friendly** (Filebeat/Fluent Bit) with no code changes — configure a shipper to forward.

---

## 12) Externalized Configuration & i18n

- **Messages** in `messages.properties` so admins can edit **without recompiling**.
- Easy **multi-lingual** support by adding locale variants: `messages_fr.properties`, etc.
- Spring **Environment**: `application.yaml/properties` for toggles and paths.
- Log4j2 configuration is **externalized**, too.
- **Branding** via external `banner.txt` (Unicode supported).

### 12.1 Result Object & Message Codes

All command handlers return a **`Result<T>`** with:
- `success: boolean`
- `code: String` (message code like `book.add.ok`)
- `params: Object[]` (message parameters `{0}`, `{1}` …)
- `payload: T` (optional domain data)
- `errors: List<ErrorDetail>`

This enables:
- **Unit tests to assert on `Result`** rather than grepping text.
- Final user-facing output assembled from **externalized messages**.
- **Bolding** of dynamic values via a message renderer that wraps parameters with ANSI **bold** escape codes.

**Example message entry:**
```
book.add.ok=Added book \u001B[1m{0}\u001B[0m with copies \u001B[1m{1}\u001B[0m.
```

---

## 13) Coding Standards & Static Analysis

- **Visibility discipline**: no unnecessary `public` API; prefer package-private for internal utilities.
- **Consistent naming** and small methods; SRP for services.
- **Linter/static analysis** warnings addressed (e.g., SpotBugs/PMD/Checkstyle if configured).
- **Null-safety**: clear contracts; `Optional` where appropriate; guard clauses.
- **Exceptions**: map to `Result` error codes; no stack traces to console.
- **Tests**: cover core logic and error paths.
- **Immutability** where viable for models; defensive copies.
- **Logs**: structured, succinct; avoid leaking PII.

---

## 14) Testing Strategy

### 14.1 Unit Tests (Shift-Left)
- JUnit 5; Mockito as needed.
- Cover: validation, borrowing/return rules, waitlist transitions, availability calc, report filters, message formatting.
- Tests assert on **`Result`** codes/params/payloads.

### 14.2 Integration Tests (Interactive Shell)
- **Simple Bash script** for smoke & assertions (portable; no heavy frameworks).
- Location: `test/cli/integration-test.sh`.
- Emulates input via **here-doc**, captures output, **grep** assertions.

**Example:**
```bash
#!/usr/bin/env bash
set -euo pipefail
JAR="${JAR:-target/bookhouse.jar}"

output="$(cat <<'EOF' | java -jar "$JAR"
auth login --user admin01
book add BK-101 2
book list
exit
EOF
)"
echo "$output" | grep -qi "AVAILABLE COMMANDS"
echo "$output" | grep -qi "BK-101"
echo "OK: integration smoke test passed."
```

_Alternatives:_ `expect` for prompt-aware testing; in-JVM JLine test terminals for deeper coverage.

- **Repo practice**: Integration tests live **in the repo**; can be wired in CI after packaging.

---

## 15) Build, Packaging, Run & Native

- **Build**: Maven + Spring Boot plugin → **fat JAR** (`bookhouse.jar`).
- **Run**: `java -jar target/bookhouse.jar`
  - Default profile: public `book list`, easy local runs.
  - `secure` profile: enables Spring Security; RBAC enforced.
- **Native** (optional): `mvn -Pnative native:compile` with GraalVM & Spring AOT (start fast, small footprint).

### 15.1 Sample `application.yml`

```yaml
spring:
  main:
    banner-mode: "console"

app:
  audit:
    file: "logs/session.log"
  prompt:
    pattern: "bookhouse({user})> "
  branding:
    banner: "classpath:banner.txt"

logging:
  config: "classpath:log4j2.xml"
```

---

## 16) Observability & Error Handling Behavior

- User sees **clean**, friendly messages; **no** stack traces.
- **Errors** go to `bookhouse.log` with trace IDs; audit captures **FAILURE** status.
- Global exception handler logs unknown commands/parse errors for traceability.

---

## 17) Future Enhancements & Roadmap

### 17.1 MCP / LLM Integration (COMING SOON)
- Placeholder command group: `ai` or `mcp` → prints a banner **“AI/MCP integration coming soon.”**
- Provider SPI: `AiClient` interface (no-op default). Later implementation can call LLM endpoints for:
  - Explaining errors, suggesting next commands, generating templates, summarizing reports.
- Config-driven enablement (`app.ai.enabled=true`).

### 17.2 Persistence
- Swap in a relational or document DB; keep SSOT; leverage **indexes** and **materialized views** in DB.

### 17.3 Notifications
- Optional hooks to email/Slack when waitlisted items become available.

### 17.4 Advanced UX
- More completions (user/book IDs), fuzzy finders, theming, accessibility tweaks.

---

## 18) Risks & Trade-offs

- **Spring for CLI** increases footprint slightly but gives **DI, testability, security, and rich UX** quickly.
- **No in-memory indexes** → O(n) scans; acceptable at current scale; future DB migration planned.
- **Interactive tests** can vary by OS; mitigated with simple Bash and CI containers.

---

## 19) Decision Log (Key Decisions)

| Decision | Why | Alternative | Status |
|---|---|---|---|
| Spring Shell over custom REPL | Faster delivery; help/completions/history/colors | Hand-rolled REPL | Accepted |
| Spring Security (RBAC) | Gate sensitive ops; proper authz | Ad-hoc checks | Accepted |
| SSOT (no secondary indexes) | Consistency, simplicity | Custom in-memory indexes | Accepted |
| Streams for queries | Readable, modern, low-maintenance | Manual loops/indexing | Accepted |
| File-only logging | Clean terminal; better UX | Console logging | Accepted |
| Audit JSON lines | Shipper-friendly | Free-text logs | Accepted |
| Bash IT harness | Simple & portable | Heavy IT frameworks | Accepted |
| Messages externalized | i18n-ready, admin-editable | Hard-coded strings | Accepted |

---

## 20) Appendix

### 20.1 Shell Prompt Customization

```java
@Component
public class PromptProvider implements org.springframework.shell.jline.PromptProvider {
  @Autowired AuthService auth;

  @Override
  public AttributedString getPrompt() {
    var user = auth.currentUsername().orElse("guest");
    return new AttributedString(String.format("bookhouse(%s)> ", user));
  }
}
```

### 20.2 AI/MCP Placeholder Command

```java
@ShellComponent
public class AiCommands {
  @ShellMethod(key = "ai", value = "AI/MCP features (coming soon).")
  public String ai() {
    return "🤖  AI/MCP integration is coming soon. Stay tuned.";
  }
}
```

### 20.3 Audit Logger Helper (Sketch)

```java
public final class AuditLogger {
  private final Path file;
  private final Logger log = LoggerFactory.getLogger(AuditLogger.class);

  public AuditLogger(Path file) { this.file = file; }

  public void log(AuditRecord r) {
    try (var out = Files.newBufferedWriter(file,
      StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      out.write(toJson(r));
      out.write(System.lineSeparator());
    } catch (IOException e) {
      log.error("Failed to write audit record", e);
    }
  }
}
```

### 20.4 Integration Test – Bash (Smoke)

```bash
#!/usr/bin/env bash
set -euo pipefail

JAR="${JAR:-target/bookhouse.jar}"

mvn -q -DskipTests package

output="$(cat <<'EOF' | java -jar "$JAR"
auth login --user admin01
book add BK-200 3
book list
loan borrow --book BK-200
report loans --user self
exit
EOF
)"

echo "$output" | grep -qi "AVAILABLE COMMANDS"
echo "$output" | grep -qi "BK-200"
echo "$output" | grep -qi "report"
echo "OK: integration test passed."
```

### 20.5 Logging Notes
- No `ConsoleAppender` configured.
- Rotations via time/size limits.
- MDC `traceId` can be added if concurrency grows.

### 20.6 Coding Conventions (Quick Tips)
- Keep services cohesive; prefer constructor injection.
- Avoid static state in services; model objects are POJOs.
- Distinguish **domain errors** vs **system errors** in `Result` codes.
- Validate inputs at the **service** boundary.
- Favor **final** fields and immutability in models when possible.

---

## 21) Submission Checklist

- [x] Base requirements implemented; mapping prepared.  
- [x] Commands structured under Spring Shell; help & completions working.  
- [x] Services encapsulate logic; SSOT in-memory model; Streams-based queries.  
- [x] Audit trail for all commands (session log).  
- [x] Application logging to **file-only**; clean terminal.  
- [x] RBAC with Spring Security; annotation-driven checks.  
- [x] Externalized messages; i18n-ready; banner branding.  
- [x] Unit tests for core services & flows; `Result` asserts.  
- [x] Integration test harness (`test/cli/integration-test.sh`).  
- [x] Reports framework (tabular, wildcards, RBAC-aware).  
- [x] MCP/LLM placeholder command.  
- [x] This design document added to repo.

---

## 22) Conclusion

Bookhouse demonstrates a **clean, maintainable** CLI with **proper RBAC**, **traceability**, and **extensibility** beyond the minimum assignment. It intentionally avoids premature complexity (no secondary in-memory indexes) while providing a **migration path** to a database. UX is pleasant and consistent thanks to Spring Shell, and testing includes both **unit** and **interactive** integration coverage. The project is ready for review and future evolution (MCP, persistence, notifications).
