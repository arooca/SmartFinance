ALTER TABLE budgets
    ADD COLUMN warning_percentage INT NOT NULL DEFAULT 80;

CREATE TABLE budget_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    budget_id BIGINT NULL,
    category VARCHAR(100) NOT NULL,
    budget_month DATE NOT NULL,
    type VARCHAR(20) NOT NULL,
    warning_percentage INT NOT NULL,
    spent_amount DECIMAL(12, 2) NOT NULL,
    budget_amount DECIMAL(12, 2) NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_budget_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_notifications_budget
        FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE SET NULL,
    CONSTRAINT uk_budget_notifications_budget_month_category_type
        UNIQUE (budget_id, budget_month, category, type),
    INDEX idx_budget_notifications_user_read_date (user_id, read_at, created_at)
);
