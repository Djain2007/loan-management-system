package com.loanmanager.ui;

import com.loanmanager.exception.AppException;
import com.loanmanager.model.User;
import com.loanmanager.service.AuthService;
import com.loanmanager.service.AuthService.Registration;
import com.loanmanager.service.CreditBureauService;
import com.loanmanager.service.LoanService;
import com.loanmanager.service.UserService;

/** Top-level menu: registration, login and password recovery. Hands off to the role-specific menus. */
public class ConsoleApp {

    private final Console console = new Console();
    private final AuthService auth;
    private final UserService users;
    private final LoanService loans;

    public ConsoleApp(AuthService auth, UserService users, LoanService loans) {
        this.auth = auth;
        this.users = users;
        this.loans = loans;
    }

    public void run() {
        while (true) {
            console.header("LOAN MANAGEMENT SYSTEM");
            console.menu(
                    "1. Register (CIBIL check)",
                    "2. Customer login",
                    "3. Admin login",
                    "4. Forgot password",
                    "0. Exit");
            int choice = console.readChoice(4);
            if (choice == 0) {
                console.info("Goodbye!");
                return;
            }
            console.attempt(() -> {
                switch (choice) {
                    case 1 -> register();
                    case 2 -> login(false);
                    case 3 -> login(true);
                    case 4 -> forgotPassword();
                    default -> console.error("Invalid option.");
                }
            });
        }
    }

    private void register() {
        console.section("New customer registration");
        String first = console.readRequired("First name");
        String last = console.readRequired("Last name");
        String mobile = console.readRequired("Mobile number (10 digits)");
        String email = console.readRequired("Email");
        String location = console.readOptional("Location (optional)");
        String pan = console.readRequired("PAN number (e.g. ABCDE1234F)");

        console.info("Running CIBIL check...");
        Registration reg = auth.register(first, last, mobile, email, location, pan);
        User u = reg.user();

        console.success("Registration complete. Your credentials have been e-mailed to " + u.email() + ".");
        console.field("CIBIL score", u.cibilScore() + " (" + CreditBureauService.band(u.cibilScore()) + ")");
        console.field("Username", u.username());
        console.field("Password", reg.password());
        console.info("Log in with these credentials to check rates and apply for a loan.");
    }

    private void login(boolean adminOnly) {
        console.section(adminOnly ? "Admin login" : "Customer login");
        String identifier = console.readRequired("Username or email");
        String password = console.readPassword("Password");
        User user = auth.login(identifier, password);

        if (adminOnly && !user.isAdmin()) {
            throw new AppException("This account does not have administrator access.");
        }
        if (user.isAdmin()) {
            new AdminMenu(console, users, loans, user).run();
        } else {
            new UserMenu(console, auth, loans, user).run();
        }
    }

    private void forgotPassword() {
        console.section("Forgot password");
        String identifier = console.readRequired("Username or email");
        auth.forgotPassword(identifier);
        console.success("A temporary password has been e-mailed. Log in with it, then change it from the menu.");
    }
}
