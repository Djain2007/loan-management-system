package com.loanmanager.service;

import com.loanmanager.dao.AuditDao;
import com.loanmanager.dao.UserDao;
import com.loanmanager.db.Database;
import com.loanmanager.exception.AppException;
import com.loanmanager.model.AccountStatus;
import com.loanmanager.model.AuditEntry;
import com.loanmanager.model.User;
import com.loanmanager.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Admin operations on customer accounts. Every change is audited and the customer is notified. */
public class UserService {

    private final Database db;
    private final UserDao users;
    private final AuditDao audit;
    private final Notifier notifier;

    public UserService(Database db, UserDao users, AuditDao audit, Notifier notifier) {
        this.db = db;
        this.users = users;
        this.audit = audit;
        this.notifier = notifier;
    }

    public List<User> search(String term) {
        String t = Validator.requireText(term, "Search text", 100);
        return db.execute(con -> users.search(con, t));
    }

    public User get(int userId) {
        return db.execute(con -> requireCustomer(con, userId));
    }

    public User updateContact(User admin, int userId, String email, String mobile, String location) {
        String mail = Validator.requireEmail(email);
        String phone = Validator.requireMobile(mobile);
        String place = Validator.optionalText(location, 100);

        User updated = db.transaction(con -> {
            User existing = requireCustomer(con, userId);
            if (!existing.email().equals(mail) && users.existsByEmail(con, mail)) {
                throw new AppException("Another account already uses the email " + mail + ".");
            }
            users.updateContact(con, userId, mail, phone, place);
            audit.log(con, admin.id(), "EDIT_USER", "user#" + userId,
                    "email=" + mail + ", mobile=" + phone + ", location=" + place);
            return users.findById(con, userId).orElseThrow();
        });

        notifier.accountUpdated(updated, "Your contact details were updated by an administrator.");
        return updated;
    }

    public User setStatus(User admin, int userId, AccountStatus status) {
        User updated = db.transaction(con -> {
            User existing = requireCustomer(con, userId);
            if (existing.status() == status) {
                throw new AppException("User is already " + status + ".");
            }
            users.updateStatus(con, userId, status);
            audit.log(con, admin.id(), status == AccountStatus.ACTIVE ? "ACTIVATE_USER" : "DEACTIVATE_USER",
                    "user#" + userId, existing.username());
            return users.findById(con, userId).orElseThrow();
        });

        notifier.accountUpdated(updated, status == AccountStatus.ACTIVE
                ? "Your account has been activated. You can now log in."
                : "Your account has been deactivated. Please contact support for more information.");
        return updated;
    }

    /** Deletes the customer and, via the foreign key, all of their loan applications. */
    public User delete(User admin, int userId) {
        User removed = db.transaction(con -> {
            User existing = requireCustomer(con, userId);
            users.delete(con, userId);
            audit.log(con, admin.id(), "DELETE_USER", "user#" + userId,
                    existing.username() + " <" + existing.email() + ">");
            return existing;
        });

        notifier.accountUpdated(removed,
                "Your account has been deleted from the system. If this was a mistake, please contact support.");
        return removed;
    }

    public List<AuditEntry> recentAudit(int limit) {
        return db.execute(con -> audit.findRecent(con, limit));
    }

    /** Admin accounts are never managed through these screens, so treat them as "not found". */
    private User requireCustomer(Connection con, int userId) throws SQLException {
        return users.findById(con, userId)
                .filter(u -> !u.isAdmin())
                .orElseThrow(() -> new AppException("No customer found with ID " + userId + "."));
    }
}
