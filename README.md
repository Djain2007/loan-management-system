# Loan Management System

A clean, console-based loan management application in **Java 17 + MySQL (JDBC)**.
Customers register with a PAN-based CIBIL check, receive auto-generated credentials by e-mail,
get interest rates based on their credit score, and apply for loans. Administrators manage
customers, approve or reject applications, and the system computes the EMI, total interest
and repayment schedule.

```
================================================================
                     LOAN MANAGEMENT SYSTEM
================================================================

  1. Register (CIBIL check)
  2. Customer login
  3. Admin login
  4. Forgot password
  0. Exit
```

## Features

**Customer**
- Registration with validation (name, 10-digit mobile, e-mail, PAN format) and duplicate checks
- Simulated CIBIL check; username + password are generated and "e-mailed" (printed to console)
- Login by username *or* e-mail; account locks after 3 wrong passwords
- Forgot password (temporary password issued) and change password
- Interest-rate quote for every loan type, derived from the CIBIL score
- Loan application with a live EMI preview before submitting
- My loans (status, EMI, rejection reason) and a month-by-month repayment schedule

**Admin**
- Dashboard with pending / approved / rejected counts
- Search, view, edit, activate, deactivate and delete customers (deleting cascades to their loans)
- View loan applications (filter by status) and full loan details with an EMI-to-income ratio
- Approve a loan (EMI, total interest and total payable are calculated and stored) or reject it with a reason
- Every admin action is written to an audit log and the customer is notified by e-mail

**Under the hood**
- Layered design: `model` → `dao` → `service` → `ui`, wired together in `Main`
- Passwords stored as salted **PBKDF2-SHA256** hashes, never in plain text
- `BigDecimal` for all money; reducing-balance EMI with a schedule that ends at exactly zero
- Real transactions for multi-step changes (approve = update loan + audit entry, or nothing)
- Foreign keys, unique constraints and indexes in a single idempotent `schema.sql`
- Prepared statements everywhere (no SQL injection), configuration via `config.properties`
- Unit tests for every piece of business logic (EMI, rates, validation, hashing, credentials)

## Quick start

**Prerequisites:** JDK 17+, MySQL Server 8+ running locally.

Set your MySQL credentials in `src/main/resources/config.properties` (defaults: `root` / `root`).
The `loan_management` database and its tables are created automatically on first run.

### Option A - Maven (recommended)

```bash
mvn package                                  # compiles, runs the tests, builds a runnable jar
java -jar target/loan-management-system.jar
```

Opening the folder in VS Code (Extension Pack for Java) or IntelliJ IDEA also works out of the box;
just run `Main`.

### Option B - without Maven

1. Download **mysql-connector-j** (Platform Independent) from
   <https://dev.mysql.com/downloads/connector/j/> and copy the `.jar` into the `lib/` folder.
2. Run `run.bat` (Windows) or `./run.sh` (macOS/Linux). The script compiles into `out/` and starts the app.

In VS Code without Maven, `.vscode/settings.json` already adds every JAR in `lib/` to the classpath,
so the **Run** button on `Main.java` works too.

### Default admin

| Username | Password    |
|----------|-------------|
| `admin`  | `Admin@123` |

Created on first start from `admin.username` / `admin.password` in `config.properties`.

## Configuration

`src/main/resources/config.properties` holds the defaults. Override without touching the jar by either
placing a `config.properties` next to it, or passing JVM flags:

```bash
java -Ddb.user=loanapp -Ddb.password=secret -jar target/loan-management-system.jar
```

| Key                            | Default                                              |
|--------------------------------|------------------------------------------------------|
| `db.url`                       | `jdbc:mysql://localhost:3306/loan_management?createDatabaseIfNotExist=true&serverTimezone=UTC` |
| `db.user` / `db.password`      | `root` / `root`                                      |
| `admin.username` / `admin.password` | `admin` / `Admin@123`                           |
| `security.max-failed-attempts` | `3`                                                  |

## A typical session

```
--- New customer registration ----------------------------------
  First name: Asha
  Last name: Sharma
  Mobile number (10 digits): 9876543210
  Email: asha@example.com
  Location (optional): Pune
  PAN number (e.g. ABCDE1234F): ABCDE1234F
  Running CIBIL check...

  [EMAIL] ------------------------------------------------------------
  To:      asha@example.com
  Subject: Your Username and Password for Loan Application

  Username : ashsha3210
  Password : gLS%6@u&aE
  CIBIL    : 645 (Needs Improvement)
  ------------------------------------------------------------
```

```
--- Interest rates for PAN ABCDE1234F  (CIBIL 645) -------------

  #  Loan type       Rate band       Your rate
  -  --------------  --------------  ---------
  1  Home Loan       6.50% - 8.50%   8.20%
  2  Personal Loan   9.00% - 12.00%  11.55%
  3  Education Loan  4.00% - 6.00%   5.70%
  4  Vehicle Loan    7.00% - 9.00%   8.70%
```

```
--- Loan summary -----------------------------------------------
  Loan type         : Home Loan
  Amount            : Rs 25,00,000.00
  Tenure            : 240 months
  Interest rate     : 8.20% p.a.
  Monthly EMI       : Rs 21,223.25
  Total interest    : Rs 25,93,578.71
  Total payable     : Rs 50,93,578.71

  Submit this application? (yes/no): yes

  [OK] Application #1 submitted. Status: PENDING.
```

```
  [OK] Loan #1 approved. EMI Rs 21,223.25 x 240 months, total payable Rs 50,93,578.71.
```

## How the numbers work

**Interest rate.** Each loan type has a band; the CIBIL score (600-900) picks the point inside it,
so a better score pays less:

| Loan type | Band            |
|-----------|-----------------|
| Home      | 6.50% - 8.50%   |
| Personal  | 9.00% - 12.00%  |
| Education | 4.00% - 6.00%   |
| Vehicle   | 7.00% - 9.00%   |

`rate = max - (score - 600) / 300 * (max - min)`

**EMI.** Standard reducing-balance formula with `r` = monthly rate and `n` = tenure in months:

`EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)`

The schedule is built month by month; the final instalment absorbs rounding so the balance ends at 0.00.

**Lock-out.** Each wrong password increments `failed_attempts`; on the third the account becomes
`INACTIVE` and only an admin can activate it again. A successful login resets the counter.

## Project structure

```
loan-management-system/
├── pom.xml                      Maven build (runnable jar with the MySQL driver bundled)
├── run.bat / run.sh             Build & run without Maven (driver jar in lib/)
├── lib/                         Drop mysql-connector-j-*.jar here for the no-Maven path
└── src/
    ├── main/java/com/loanmanager/
    │   ├── Main.java            Wires config, database, services and UI together
    │   ├── config/AppConfig     Loads config.properties (+ local file / -D overrides)
    │   ├── db/Database          Connections, schema bootstrap, execute() / transaction()
    │   ├── model/               User, Loan, LoanType, LoanStatus, Role, AccountStatus, EmiDetails, AuditEntry
    │   ├── dao/                 UserDao, LoanDao, AuditDao - SQL only, no business rules
    │   ├── service/
    │   │   ├── AuthService      Register (CIBIL + credentials), login + lock-out, passwords
    │   │   ├── UserService      Admin customer management, audited + notified
    │   │   ├── LoanService      Quotes, applications, approve / reject with EMI
    │   │   ├── EmiCalculator    EMI, totals and repayment schedule
    │   │   ├── CreditBureauService  Simulated CIBIL lookup (deterministic per PAN)
    │   │   ├── CredentialGenerator  Username / password generation
    │   │   ├── Notifier         All e-mail templates
    │   │   └── EmailService     Transport interface; ConsoleEmailService prints to the console
    │   ├── ui/                  Console (input/output), Table, ConsoleApp, UserMenu, AdminMenu
    │   ├── util/                Validator, PasswordHasher, Money, Dates
    │   └── exception/           AppException (user-facing), DataAccessException (SQL wrapper)
    ├── main/resources/
    │   ├── config.properties
    │   └── schema.sql
    └── test/java/...            JUnit 5 tests for the business logic
```

### Database schema

| Table        | Purpose                                                                          |
|--------------|----------------------------------------------------------------------------------|
| `users`      | Customers and admins; unique e-mail / username / PAN; hashed password; status; CIBIL |
| `loans`      | Applications with terms, status, EMI figures, decision timestamp; FK → users (cascade) |
| `audit_logs` | Who did what and when; FK → users (set null on delete)                           |

## Tests

```bash
mvn test
```

Covers the EMI formula (including the textbook 1,00,000 @ 10% / 12 months = 8,791.59 case and
schedule consistency), rate bands and clamping, validation rules, password hashing, generated
credentials, the credit-bureau simulation and money formatting.

## Extending

- **Real e-mail:** implement `EmailService` with Jakarta Mail (add `org.eclipse.angus:angus-mail`
  to `pom.xml`) and pass it to `Notifier` in `Main` instead of `ConsoleEmailService`.
- **Real credit bureau:** replace the body of `CreditBureauService.fetchScore(pan)` with the API call.
- **More loan products:** add a constant to `LoanType` with its rate band - menus, quotes and
  validation pick it up automatically.
