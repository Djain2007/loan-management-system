package com.loanmanager.service;

import com.loanmanager.dao.AuditDao;
import com.loanmanager.dao.LoanDao;
import com.loanmanager.db.Database;
import com.loanmanager.exception.AppException;
import com.loanmanager.model.EmiDetails;
import com.loanmanager.model.Loan;
import com.loanmanager.model.LoanStatus;
import com.loanmanager.model.LoanType;
import com.loanmanager.model.User;
import com.loanmanager.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loan applications: rate quotes, submission, and admin approval / rejection with EMI calculation. */
public class LoanService {

    public static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(10_000);
    public static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(100_000_000);
    public static final int MIN_TENURE_MONTHS = 6;
    public static final int MAX_TENURE_MONTHS = 360;

    private final Database db;
    private final LoanDao loans;
    private final AuditDao audit;
    private final Notifier notifier;

    public LoanService(Database db, LoanDao loans, AuditDao audit, Notifier notifier) {
        this.db = db;
        this.loans = loans;
        this.audit = audit;
        this.notifier = notifier;
    }

    /** Annual rate the applicant would get for a loan type, based on their CIBIL score. */
    public BigDecimal quoteRate(User applicant, LoanType type) {
        return type.rateFor(requireCibil(applicant));
    }

    /** What the EMI would look like before the application is submitted. */
    public EmiDetails preview(User applicant, LoanType type, BigDecimal amount, int tenureMonths) {
        validateTerms(amount, tenureMonths);
        return EmiCalculator.calculate(amount, quoteRate(applicant, type), tenureMonths);
    }

    public Loan apply(User applicant, LoanType type, BigDecimal amount, int tenureMonths,
                      BigDecimal annualIncome, String purpose) {
        validateTerms(amount, tenureMonths);
        if (annualIncome == null || annualIncome.signum() <= 0) {
            throw new AppException("Annual income must be greater than zero.");
        }
        String why = Validator.requireText(purpose, "Purpose", 500);
        BigDecimal rate = quoteRate(applicant, type);

        Loan draft = new Loan(0, applicant.id(), applicant.fullName(), applicant.email(), type, amount,
                tenureMonths, rate, annualIncome, why, LoanStatus.PENDING, null, null, null, null, null, null);
        return db.execute(con -> {
            int id = loans.insert(con, draft);
            return loans.findById(con, id).orElseThrow();
        });
    }

    public List<Loan> loansOf(User applicant) {
        return db.execute(con -> loans.findByUser(con, applicant.id()));
    }

    /** All loans, or only those with the given status when {@code status} is not null. */
    public List<Loan> all(LoanStatus status) {
        return db.execute(con -> loans.findAll(con, status));
    }

    public Optional<Loan> find(int loanId) {
        return db.execute(con -> loans.findById(con, loanId));
    }

    public Map<LoanStatus, Integer> countByStatus() {
        return db.execute(loans::countByStatus);
    }

    /** Approves a pending loan, computing and storing its EMI, then notifies the applicant. */
    public Loan approve(User admin, int loanId) {
        Loan approved = db.transaction(con -> {
            Loan loan = requirePending(con, loanId);
            EmiDetails emi = EmiCalculator.calculate(loan.amount(), loan.interestRate(), loan.tenureMonths());
            loans.approve(con, loanId, emi.emi(), emi.totalInterest(), emi.totalPayable());
            audit.log(con, admin.id(), "APPROVE_LOAN", "loan#" + loanId, "EMI " + emi.emi().toPlainString());
            return loans.findById(con, loanId).orElseThrow();
        });
        notifier.loanApproved(approved);
        return approved;
    }

    public Loan reject(User admin, int loanId, String reason) {
        String why = Validator.requireText(reason, "Rejection reason", 500);
        Loan rejected = db.transaction(con -> {
            requirePending(con, loanId);
            loans.reject(con, loanId, why);
            audit.log(con, admin.id(), "REJECT_LOAN", "loan#" + loanId, why);
            return loans.findById(con, loanId).orElseThrow();
        });
        notifier.loanRejected(rejected);
        return rejected;
    }

    /** Full repayment schedule for a loan (approved loans use their stored terms). */
    public EmiDetails schedule(Loan loan) {
        return EmiCalculator.calculate(loan.amount(), loan.interestRate(), loan.tenureMonths());
    }

    /** EMI as a percentage of monthly income; a quick affordability signal for the reviewer. */
    public BigDecimal emiToIncomePercent(Loan loan) {
        BigDecimal emi = loan.emiAmount() != null ? loan.emiAmount() : schedule(loan).emi();
        BigDecimal monthlyIncome = loan.annualIncome().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        if (monthlyIncome.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return emi.multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 1, RoundingMode.HALF_UP);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private static void validateTerms(BigDecimal amount, int tenureMonths) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new AppException("Loan amount must be between " + MIN_AMOUNT.toPlainString()
                    + " and " + MAX_AMOUNT.toPlainString() + ".");
        }
        if (tenureMonths < MIN_TENURE_MONTHS || tenureMonths > MAX_TENURE_MONTHS) {
            throw new AppException("Tenure must be between " + MIN_TENURE_MONTHS + " and "
                    + MAX_TENURE_MONTHS + " months.");
        }
    }

    private static int requireCibil(User applicant) {
        if (applicant.cibilScore() == null) {
            throw new AppException("No CIBIL score on record for this account.");
        }
        return applicant.cibilScore();
    }

    private Loan requirePending(Connection con, int loanId) throws SQLException {
        Loan loan = loans.findById(con, loanId)
                .orElseThrow(() -> new AppException("No loan found with ID " + loanId + "."));
        if (!loan.isPending()) {
            throw new AppException("Loan #" + loanId + " is already " + loan.status() + ".");
        }
        return loan;
    }
}
