package com.loanmanager.service;

import com.loanmanager.dao.UserDao;
import com.loanmanager.db.Database;
import com.loanmanager.exception.AppException;
import com.loanmanager.model.AccountStatus;
import com.loanmanager.model.Role;
import com.loanmanager.model.User;
import com.loanmanager.util.PasswordHasher;
import com.loanmanager.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;

/** Registration, login with lock-out, and password management. */
public class AuthService {

    /** A freshly registered account together with the one-time plain-text password that was issued. */
    public record Registration(User user, String password) {
    }

    private final Database db;
    private final UserDao users;
    private final CreditBureauService creditBureau;
    private final Notifier notifier;
    private final int maxFailedAttempts;

    public AuthService(Database db, UserDao users, CreditBureauService creditBureau, Notifier notifier,
                       int maxFailedAttempts) {
        this.db = db;
        this.users = users;
        this.creditBureau = creditBureau;
        this.notifier = notifier;
        this.maxFailedAttempts = maxFailedAttempts;
    }

    /**
     * Registers a customer: validates input, runs the CIBIL check, issues credentials,
     * stores the account and e-mails the credentials.
     */
    public Registration register(String firstName, String lastName, String mobile, String email,
                                 String location, String pan) {
        String first = Validator.requireName(firstName, "First name");
        String last = Validator.requireName(lastName, "Last name");
        String phone = Validator.requireMobile(mobile);
        String mail = Validator.requireEmail(email);
        String place = Validator.optionalText(location, 100);
        String panNumber = Validator.requirePan(pan);

        Registration registration = db.transaction(con -> {
            if (users.existsByEmail(con, mail)) {
                throw new AppException("An account with this email already exists.");
            }
            if (users.existsByPan(con, panNumber)) {
                throw new AppException("An account with this PAN already exists.");
            }
            int cibil = creditBureau.fetchScore(panNumber);
            String username = uniqueUsername(con, CredentialGenerator.username(first, last, phone));
            String password = CredentialGenerator.password();

            User draft = new User(0, first, last, phone, mail, place, panNumber, cibil, username,
                    PasswordHasher.hash(password), Role.USER, AccountStatus.ACTIVE, 0, null, null);
            int id = users.insert(con, draft);
            return new Registration(users.findById(con, id).orElseThrow(), password);
        });

        notifier.welcome(registration.user(), registration.password());
        return registration;
    }

    /**
     * Authenticates by username or e-mail. Wrong passwords count towards the lock-out limit.
     * Runs in auto-commit mode on purpose: the failed-attempt counter must persist even though
     * the method then throws.
     */
    public User login(String identifier, String password) {
        String id = Validator.requireText(identifier, "Username or email", 120);
        return db.execute(con -> {
            User user = users.findByLogin(con, id)
                    .orElseThrow(() -> new AppException("No account found for '" + id + "'."));
            if (!user.isActive()) {
                throw new AppException("This account is locked/inactive. Please contact support.");
            }
            if (!PasswordHasher.verify(password, user.passwordHash())) {
                return handleWrongPassword(con, user);
            }
            if (user.failedAttempts() > 0) {
                users.updateFailedAttempts(con, user.id(), 0, AccountStatus.ACTIVE);
            }
            return user;
        });
    }

    /** Issues a temporary password and e-mails it; the user is expected to change it after logging in. */
    public void forgotPassword(String identifier) {
        String id = Validator.requireText(identifier, "Username or email", 120);
        String temporary = CredentialGenerator.password();
        User user = db.execute(con -> {
            User u = users.findByLogin(con, id)
                    .orElseThrow(() -> new AppException("No account found for '" + id + "'."));
            users.updatePassword(con, u.id(), PasswordHasher.hash(temporary));
            return u;
        });
        notifier.passwordReset(user, temporary);
    }

    /** Changes the password and returns the refreshed account so the caller's session stays current. */
    public User changePassword(User user, String currentPassword, String newPassword) {
        if (!PasswordHasher.verify(currentPassword, user.passwordHash())) {
            throw new AppException("Current password is incorrect.");
        }
        String fresh = Validator.requireStrongPassword(newPassword);
        if (PasswordHasher.verify(fresh, user.passwordHash())) {
            throw new AppException("New password must be different from the current one.");
        }
        return db.execute(con -> {
            users.updatePassword(con, user.id(), PasswordHasher.hash(fresh));
            return users.findById(con, user.id()).orElseThrow();
        });
    }

    /** Creates the built-in admin on first start. Returns true if it was created now. */
    public boolean ensureAdminExists(String username, String password) {
        return db.transaction(con -> {
            if (users.existsAdmin(con)) {
                return false;
            }
            User admin = new User(0, "System", "Admin", "0000000000", "admin@loanmanager.local", "Head Office",
                    null, null, username, PasswordHasher.hash(password), Role.ADMIN, AccountStatus.ACTIVE, 0,
                    null, null);
            users.insert(con, admin);
            return true;
        });
    }

    // ---- helpers -----------------------------------------------------------------------------

    private User handleWrongPassword(Connection con, User user) throws SQLException {
        int attempts = user.failedAttempts() + 1;
        if (attempts >= maxFailedAttempts) {
            users.updateFailedAttempts(con, user.id(), attempts, AccountStatus.INACTIVE);
            throw new AppException("Account locked after " + maxFailedAttempts
                    + " failed attempts. Please contact support.");
        }
        users.updateFailedAttempts(con, user.id(), attempts, AccountStatus.ACTIVE);
        int remaining = maxFailedAttempts - attempts;
        throw new AppException("Invalid password. " + remaining + " attempt" + (remaining == 1 ? "" : "s")
                + " remaining before the account is locked.");
    }

    private String uniqueUsername(Connection con, String base) throws SQLException {
        String candidate = base;
        for (int n = 2; users.existsByUsername(con, candidate); n++) {
            candidate = base + n;
        }
        return candidate;
    }
}
