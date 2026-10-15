package com.loanmanager.ui;

import com.loanmanager.exception.AppException;
import com.loanmanager.model.EmiDetails;
import com.loanmanager.model.Loan;
import com.loanmanager.model.LoanType;
import com.loanmanager.model.User;
import com.loanmanager.service.AuthService;
import com.loanmanager.service.CreditBureauService;
import com.loanmanager.service.LoanService;
import com.loanmanager.util.Dates;
import com.loanmanager.util.Money;

import java.math.BigDecimal;
import java.util.List;

/** Screens available to a logged-in customer. */
public class UserMenu {

    private final Console console;
    private final AuthService auth;
    private final LoanService loans;
    private User user;

    public UserMenu(Console console, AuthService auth, LoanService loans, User user) {
        this.console = console;
        this.auth = auth;
        this.loans = loans;
        this.user = user;
    }

    public void run() {
        console.success("Welcome, " + user.fullName() + "!  CIBIL score: " + user.cibilScore()
                + " (" + CreditBureauService.band(user.cibilScore()) + ")");
        while (true) {
            console.header("CUSTOMER MENU  -  " + user.fullName());
            console.menu(
                    "1. Check interest rates",
                    "2. Apply for a loan",
                    "3. My loans",
                    "4. Repayment schedule",
                    "5. Change password",
                    "0. Logout");
            int choice = console.readChoice(5);
            if (choice == 0) {
                console.success("Logged out.");
                return;
            }
            console.attempt(() -> {
                switch (choice) {
                    case 1 -> showRates();
                    case 2 -> applyForLoan();
                    case 3 -> myLoans();
                    case 4 -> repaymentSchedule();
                    case 5 -> changePassword();
                    default -> console.error("Invalid option.");
                }
            });
        }
    }

    private void showRates() {
        console.section("Interest rates for PAN " + user.panNumber() + "  (CIBIL " + user.cibilScore() + ")");
        Table table = new Table("#", "Loan type", "Rate band", "Your rate");
        int i = 1;
        for (LoanType type : LoanType.values()) {
            table.row(i++, type.label(), Money.percent(type.minRate()) + " - " + Money.percent(type.maxRate()),
                    Money.percent(loans.quoteRate(user, type)));
        }
        table.print();
        console.info("A higher CIBIL score earns a rate closer to the bottom of the band.");
    }

    private void applyForLoan() {
        console.section("Loan application");
        LoanType type = chooseLoanType();
        BigDecimal amount = console.readAmount("Loan amount (" + LoanService.MIN_AMOUNT.toPlainString()
                + " - " + LoanService.MAX_AMOUNT.toPlainString() + ")");
        int tenure = console.readInt("Tenure in months", LoanService.MIN_TENURE_MONTHS, LoanService.MAX_TENURE_MONTHS);
        BigDecimal income = console.readAmount("Annual income");
        String purpose = console.readRequired("Purpose of the loan");

        EmiDetails preview = loans.preview(user, type, amount, tenure);
        console.section("Loan summary");
        console.field("Loan type", type.label());
        console.field("Amount", Money.format(amount));
        console.field("Tenure", tenure + " months");
        console.field("Interest rate", Money.percent(loans.quoteRate(user, type)) + " p.a.");
        console.field("Monthly EMI", Money.format(preview.emi()));
        console.field("Total interest", Money.format(preview.totalInterest()));
        console.field("Total payable", Money.format(preview.totalPayable()));
        console.blank();

        if (!console.confirm("Submit this application?")) {
            console.info("Application cancelled.");
            return;
        }
        Loan loan = loans.apply(user, type, amount, tenure, income, purpose);
        console.success("Application #" + loan.id() + " submitted. Status: " + loan.status()
                + ". You will be notified once it is reviewed.");
    }

    private void myLoans() {
        console.section("My loans");
        List<Loan> mine = loans.loansOf(user);
        Table table = new Table("ID", "Type", "Amount", "Tenure", "Rate", "EMI", "Status", "Applied", "Note");
        for (Loan l : mine) {
            table.row(l.id(), l.type().label(), Money.format(l.amount()), l.tenureMonths() + " mo",
                    Money.percent(l.interestRate()), Money.format(l.emiAmount()), l.status(),
                    Dates.format(l.appliedAt()), l.rejectionReason());
        }
        table.print();
    }

    private void repaymentSchedule() {
        List<Loan> approved = loans.loansOf(user).stream().filter(Loan::isApproved).toList();
        if (approved.isEmpty()) {
            throw new AppException("You have no approved loans yet.");
        }
        console.section("Approved loans");
        Table table = new Table("ID", "Type", "Amount", "Tenure", "EMI");
        for (Loan l : approved) {
            table.row(l.id(), l.type().label(), Money.format(l.amount()), l.tenureMonths() + " mo",
                    Money.format(l.emiAmount()));
        }
        table.print();

        int id = console.readInt("Loan ID");
        Loan loan = approved.stream().filter(l -> l.id() == id).findFirst()
                .orElseThrow(() -> new AppException("Loan #" + id + " is not one of your approved loans."));

        EmiDetails details = loans.schedule(loan);
        console.section("Repayment schedule for loan #" + loan.id());
        Table schedule = new Table("Month", "EMI", "Principal", "Interest", "Balance");
        for (EmiDetails.Instalment row : details.schedule()) {
            schedule.row(row.month(), Money.format(row.principal().add(row.interest())),
                    Money.format(row.principal()), Money.format(row.interest()), Money.format(row.balance()));
        }
        schedule.print();
        console.field("Total interest", Money.format(details.totalInterest()));
        console.field("Total payable", Money.format(details.totalPayable()));
    }

    private void changePassword() {
        console.section("Change password");
        String current = console.readPassword("Current password");
        String fresh = console.readPassword("New password");
        String again = console.readPassword("Confirm new password");
        if (!fresh.equals(again)) {
            throw new AppException("New passwords do not match.");
        }
        user = auth.changePassword(user, current, fresh);
        console.success("Password changed successfully.");
    }

    private LoanType chooseLoanType() {
        Table table = new Table("#", "Loan type", "Your rate");
        int i = 1;
        for (LoanType type : LoanType.values()) {
            table.row(i++, type.label(), Money.percent(loans.quoteRate(user, type)) + " p.a.");
        }
        table.print();
        return LoanType.fromMenuChoice(console.readInt("Loan type", 1, LoanType.values().length));
    }
}
