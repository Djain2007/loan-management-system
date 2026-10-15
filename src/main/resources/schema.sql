-- Loan Management System schema.
-- Executed on every start-up; every statement is idempotent.

CREATE TABLE IF NOT EXISTS users (
    id              INT          NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    mobile          VARCHAR(10)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    location        VARCHAR(100),
    pan_number      CHAR(10),
    cibil_score     INT,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(10)  NOT NULL DEFAULT 'USER',
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_pan (pan_number)
);

CREATE TABLE IF NOT EXISTS loans (
    id               INT           NOT NULL AUTO_INCREMENT,
    user_id          INT           NOT NULL,
    loan_type        VARCHAR(20)   NOT NULL,
    amount           DECIMAL(15,2) NOT NULL,
    tenure_months    INT           NOT NULL,
    interest_rate    DECIMAL(5,2)  NOT NULL,
    annual_income    DECIMAL(15,2) NOT NULL,
    purpose          VARCHAR(500)  NOT NULL,
    status           VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
    emi_amount       DECIMAL(15,2),
    total_interest   DECIMAL(15,2),
    total_payable    DECIMAL(15,2),
    rejection_reason VARCHAR(500),
    applied_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at       DATETIME,
    PRIMARY KEY (id),
    KEY idx_loans_user (user_id),
    KEY idx_loans_status (status),
    CONSTRAINT fk_loans_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id         INT          NOT NULL AUTO_INCREMENT,
    actor_id   INT,
    action     VARCHAR(50)  NOT NULL,
    target     VARCHAR(100),
    details    VARCHAR(500),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
);
