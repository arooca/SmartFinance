CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    PRIMARY KEY (id)
);

CREATE TABLE incomes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    concept VARCHAR(255) NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    category VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(255),
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_incomes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    concept VARCHAR(255) NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    category VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(255) NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    month DATE NOT NULL,
    user_id BIGINT,
    CONSTRAINT uk_budgets_user_category_month
        UNIQUE (user_id, category, month),
    PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE recurring_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    concept VARCHAR(255) NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    category VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    day_of_month INT NOT NULL,
    active BIT NOT NULL,
    next_execution_date DATE,
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_recurring_movements_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);