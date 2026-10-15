package com.loanmanager;

import com.loanmanager.config.AppConfig;
import com.loanmanager.dao.AuditDao;
import com.loanmanager.dao.LoanDao;
import com.loanmanager.dao.UserDao;
import com.loanmanager.db.Database;
import com.loanmanager.exception.DataAccessException;
import com.loanmanager.service.AuthService;
import com.loanmanager.service.ConsoleEmailService;
import com.loanmanager.service.CreditBureauService;
import com.loanmanager.service.LoanService;
import com.loanmanager.service.Notifier;
import com.loanmanager.service.UserService;
import com.loanmanager.ui.ConsoleApp;

/** Entry point: wires configuration, database, services and the console UI together. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            start();
        } catch (IllegalStateException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
        } catch (DataAccessException e) {
            System.err.println("Could not connect to the database: " + e.getMessage());
            System.err.println("Make sure MySQL is running and the db.* settings in config.properties are correct.");
            System.exit(1);
        }
    }

    private static void start() {
        AppConfig config = AppConfig.load();
        Database db = new Database(config.dbUrl(), config.dbUser(), config.dbPassword());

        UserDao userDao = new UserDao();
        LoanDao loanDao = new LoanDao();
        AuditDao auditDao = new AuditDao();

        Notifier notifier = new Notifier(new ConsoleEmailService());
        AuthService auth = new AuthService(db, userDao, new CreditBureauService(), notifier,
                config.maxFailedAttempts());
        UserService users = new UserService(db, userDao, auditDao, notifier);
        LoanService loans = new LoanService(db, loanDao, auditDao, notifier);

        db.initSchema();
        if (auth.ensureAdminExists(config.adminUsername(), config.adminPassword())) {
            System.out.println("  First run: created admin account '" + config.adminUsername()
                    + "' with the password from config.properties.");
        }

        new ConsoleApp(auth, users, loans).run();
    }
}
