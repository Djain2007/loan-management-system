package com.loanmanager.ui;

import com.loanmanager.exception.AppException;
import com.loanmanager.model.AccountStatus;
import com.loanmanager.model.AuditEntry;
import com.loanmanager.model.Loan;
import com.loanmanager.model.LoanStatus;
import com.loanmanager.model.User;
import com.loanmanager.service.LoanService;
import com.loanmanager.service.UserService;
import com.loanmanager.util.Dates;
import com.loanmanager.util.Money;

import java.util.List;
import java.util.Map;

/** Screens available to an administrator: customer management and loan decisions. */
public class AdminMenu {

    private static final int AUDIT_ROWS = 30;

    private final Console console;
    private final UserService users;
    private final LoanService loans;
    private final User admin;

    public AdminMenu(Console console, UserService users, LoanService loans, User admin) {
        this.console = console;
        this.users = users;
        this.loans = loans;
        this.admin = admin;
    }

    public void run() {
        console.success("Welcome, " + admin.fullName() + ".");
        while (true) {
            console.header("ADMIN DASHBOARD");
            console.attempt(this::printLoanSummary);
            console.menu(
                    "Customers",
                    "  1. Search customers",
                    "  2. View customer details",
                    "  3. Edit customer contact details",
                    "  4. Activate customer",
                    "  5. Deactivate customer",
                    "  6. Delete customer",
                    "Loans",
                    "  7. View loan applications",
                    "  8. View loan details",
                    "  9. Approve loan (calculates EMI)",
                    " 10. Reject loan",
                    "System",
                    " 11. View audit log",
                    "  0. Logout");
            int choice = console.readChoice(11);
            if (choice == 0) {
                console.success("Logged out.");
                return;
            }
            console.attempt(() -> {
                switch (choice) {
                    case 1 -> searchCustomers();
                    case 2 -> viewCustomer();
                    case 3 -> editCustomer();
                    case 4 -> setStatus(AccountStatus.ACTIVE);
                    case 5 -> setStatus(AccountStatus.INACTIVE);
                    case 6 -> deleteCustomer();
                    case 7 -> viewLoans();
                    case 8 -> viewLoanDetails();
                    case 9 -> approveLoan();
                    case 10 -> rejectLoan();
                    case 11 -> viewAuditLog();
                    default -> console.error("Invalid option.");
                }
            });
        }
    }

    // ---- customers ---------------------------------------------------------------------------

    private void searchCustomers() {
        console.section("Search customers");
        String term = console.readRequired("Name, email, mobile, username or status");
        List<User> found = users.search(term);
        Table table = new Table("ID", "Name", "Username", "Email", "Mobile", "CIBIL", "Status", "Role");
        for (User u : found) {
            table.row(u.id(), u.fullName(), u.username(), u.email(), u.mobile(), u.cibilScore(),
                    u.status(), u.role());
        }
        table.print();
    }

    private void viewCustomer() {
        User u = users.get(console.readInt("Customer ID"));
        console.section("Customer #" + u.id());
        console.field("Name", u.fullName());
        console.field("Username", u.username());
        console.field("Email", u.email());
        console.field("Mobile", u.mobile());
        console.field("Location", u.location());
        console.field("PAN", u.panNumber());
        console.field("CIBIL score", u.cibilScore());
        console.field("Status", u.status());
        console.field("Failed logins", u.failedAttempts());
        console.field("Registered", Dates.format(u.createdAt()));
        console.field("Last updated", Dates.format(u.updatedAt()));
    }

    private void editCustomer() {
        User u = users.get(console.readInt("Customer ID"));
        console.section("Editing " + u.fullName() + "  (press Enter to keep the current value)");
        String email = orDefault(console.readOptional("Email [" + u.email() + "]"), u.email());
        String mobile = orDefault(console.readOptional("Mobile [" + u.mobile() + "]"), u.mobile());
        String location = orDefault(console.readOptional("Location [" + nullSafe(u.location()) + "]"), u.location());
        users.updateContact(admin, u.id(), email, mobile, location);
        console.success("Customer #" + u.id() + " updated.");
    }

    private void setStatus(AccountStatus status) {
        int id = console.readInt("Customer ID");
        User u = users.setStatus(admin, id, status);
        console.success(u.fullName() + " is now " + u.status() + ".");
    }

    private void deleteCustomer() {
        User u = users.get(console.readInt("Customer ID"));
        console.error("This permanently deletes " + u.fullName() + " (" + u.username()
                + ") and all of their loan applications.");
        if (!console.confirm("Are you sure?")) {
            console.info("Deletion cancelled.");
            return;
        }
        users.delete(admin, u.id());
        console.success("Customer #" + u.id() + " deleted.");
    }

    // ---- loans -------------------------------------------------------------------------------

    private void viewLoans() {
        console.menu("1. All", "2. Pending", "3. Approved", "4. Rejected");
        int filter = console.readInt("Show", 1, 4);
        LoanStatus status = switch (filter) {
            case 2 -> LoanStatus.PENDING;
            case 3 -> LoanStatus.APPROVED;
            case 4 -> LoanStatus.REJECTED;
            default -> null;
        };
        console.section(status == null ? "All loan applications" : status + " loan applications");
        Table table = new Table("ID", "Applicant", "Type", "Amount", "Tenure", "Rate", "EMI", "Status", "Applied");
        for (Loan l : loans.all(status)) {
            table.row(l.id(), l.applicantName(), l.type().label(), Money.format(l.amount()),
                    l.tenureMonths() + " mo", Money.percent(l.interestRate()), Money.format(l.emiAmount()),
                    l.status(), Dates.format(l.appliedAt()));
        }
        table.print();
    }

    private void viewLoanDetails() {
        Loan l = requireLoan(console.readInt("Loan ID"));
        console.section("Loan #" + l.id() + "  -  " + l.status());
        console.field("Applicant", l.applicantName() + " (user #" + l.userId() + ")");
        console.field("Email", l.applicantEmail());
        console.field("Loan type", l.type().label());
        console.field("Amount", Money.format(l.amount()));
        console.field("Tenure", l.tenureMonths() + " months");
        console.field("Interest rate", Money.percent(l.interestRate()) + " p.a.");
        console.field("Annual income", Money.format(l.annualIncome()));
        console.field("EMI / income", loans.emiToIncomePercent(l) + "% of monthly income");
        console.field("Purpose", l.purpose());
        console.field("Applied", Dates.format(l.appliedAt()));
        if (l.isApproved()) {
            console.field("Monthly EMI", Money.format(l.emiAmount()));
            console.field("Total interest", Money.format(l.totalInterest()));
            console.field("Total payable", Money.format(l.totalPayable()));
            console.field("Approved on", Dates.format(l.decidedAt()));
        } else if (l.status() == LoanStatus.REJECTED) {
            console.field("Rejected on", Dates.format(l.decidedAt()));
            console.field("Reason", l.rejectionReason());
        }
    }

    private void approveLoan() {
        Loan l = requireLoan(console.readInt("Loan ID"));
        console.section("Approve loan #" + l.id());
        console.field("Applicant", l.applicantName());
        console.field("Amount", Money.format(l.amount()));
        console.field("Tenure", l.tenureMonths() + " months");
        console.field("Rate", Money.percent(l.interestRate()) + " p.a.");
        console.field("EMI / income", loans.emiToIncomePercent(l) + "% of monthly income");
        if (!console.confirm("Approve this loan?")) {
            console.info("No changes made.");
            return;
        }
        Loan approved = loans.approve(admin, l.id());
        console.success("Loan #" + approved.id() + " approved. EMI " + Money.format(approved.emiAmount())
                + " x " + approved.tenureMonths() + " months, total payable " + Money.format(approved.totalPayable()) + ".");
    }

    private void rejectLoan() {
        Loan l = requireLoan(console.readInt("Loan ID"));
        console.section("Reject loan #" + l.id() + "  -  " + l.applicantName() + ", " + Money.format(l.amount()));
        String reason = console.readRequired("Reason for rejection");
        Loan rejected = loans.reject(admin, l.id(), reason);
        console.success("Loan #" + rejected.id() + " rejected and applicant notified.");
    }

    // ---- system ------------------------------------------------------------------------------

    private void viewAuditLog() {
        console.section("Audit log (latest " + AUDIT_ROWS + ")");
        Table table = new Table("ID", "When", "Admin", "Action", "Target", "Details");
        for (AuditEntry e : users.recentAudit(AUDIT_ROWS)) {
            table.row(e.id(), Dates.format(e.createdAt()), e.actorName(), e.action(), e.target(), e.details());
        }
        table.print();
    }

    // ---- helpers -----------------------------------------------------------------------------

    private void printLoanSummary() {
        Map<LoanStatus, Integer> counts = loans.countByStatus();
        console.info("Loans:  " + counts.get(LoanStatus.PENDING) + " pending   "
                + counts.get(LoanStatus.APPROVED) + " approved   "
                + counts.get(LoanStatus.REJECTED) + " rejected");
    }

    private Loan requireLoan(int id) {
        return loans.find(id).orElseThrow(() -> new AppException("No loan found with ID " + id + "."));
    }

    private static String orDefault(String value, String current) {
        return value.isEmpty() ? current : value;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
