package com.loanmanager.service;

import com.loanmanager.model.Loan;
import com.loanmanager.model.User;
import com.loanmanager.util.Money;

/** Composes every customer-facing e-mail in one place and hands it to the {@link EmailService}. */
public class Notifier {

    private final EmailService email;

    public Notifier(EmailService email) {
        this.email = email;
    }

    public void welcome(User user, String rawPassword) {
        email.send(user.email(), "Your Username and Password for Loan Application", """
                Dear %s,

                Your CIBIL check is complete and your account has been created.

                Username : %s
                Password : %s
                CIBIL    : %d (%s)

                Please log in and change your password at your earliest convenience.
                """.formatted(user.fullName(), user.username(), rawPassword,
                user.cibilScore(), CreditBureauService.band(user.cibilScore())));
    }

    public void passwordReset(User user, String temporaryPassword) {
        email.send(user.email(), "Password Reset", """
                Dear %s,

                A temporary password has been generated for your account.

                Temporary password : %s

                Log in with it and choose a new password from "Change Password".
                If you did not request this, please contact support.
                """.formatted(user.fullName(), temporaryPassword));
    }

    public void accountUpdated(User user, String whatChanged) {
        email.send(user.email(), "Account Update Notification", """
                Dear %s,

                %s
                If you did not expect this change, please contact support.
                """.formatted(user.fullName(), whatChanged));
    }

    public void loanApproved(Loan loan) {
        email.send(loan.applicantEmail(), "Your Loan Application has been Approved!", """
                Dear %s,

                Congratulations! Your loan application #%d has been approved.

                Sanctioned Amount : %s
                Loan Type         : %s
                Tenure            : %d months
                Interest Rate     : %s per annum
                Monthly EMI       : %s
                Total Interest    : %s
                Total Payable     : %s

                Please contact support for disbursement details.
                """.formatted(loan.applicantName(), loan.id(), Money.format(loan.amount()),
                loan.type().label(), loan.tenureMonths(), Money.percent(loan.interestRate()),
                Money.format(loan.emiAmount()), Money.format(loan.totalInterest()),
                Money.format(loan.totalPayable())));
    }

    public void loanRejected(Loan loan) {
        email.send(loan.applicantEmail(), "Your Loan Application has been Rejected", """
                Dear %s,

                We regret to inform you that your %s application #%d for %s has been rejected.

                Reason: %s

                Please contact support if you would like to discuss this decision.
                """.formatted(loan.applicantName(), loan.type().label(), loan.id(),
                Money.format(loan.amount()), loan.rejectionReason()));
    }
}
