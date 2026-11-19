CREATE TABLE savings_goals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(12, 2) NOT NULL,
    current_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    target_date DATE NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_savings_goals_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_savings_goals_user_date (user_id, target_date)
);
