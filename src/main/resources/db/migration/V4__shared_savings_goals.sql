ALTER TABLE savings_goals
    ADD COLUMN participant_id BIGINT NULL,
    ADD COLUMN settled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT fk_savings_goals_participant
        FOREIGN KEY (participant_id) REFERENCES users (id),
    ADD INDEX idx_savings_goals_participant_date (participant_id, target_date);

CREATE TABLE goal_contributions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    goal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    contributed_at DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_goal_contributions_goal
        FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_contributions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_goal_contributions_goal_user (goal_id, user_id),
    INDEX idx_goal_contributions_user_date (user_id, contributed_at)
);

INSERT INTO goal_contributions (goal_id, user_id, amount, contributed_at)
SELECT id, user_id, current_amount, CURRENT_DATE
FROM savings_goals
WHERE current_amount > 0;

CREATE TABLE goal_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(12, 2) NOT NULL,
    target_date DATE NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_goal_invitations_inviter
        FOREIGN KEY (inviter_id) REFERENCES users (id),
    CONSTRAINT fk_goal_invitations_invitee
        FOREIGN KEY (invitee_id) REFERENCES users (id),
    INDEX idx_goal_invitations_invitee_status (invitee_id, status, created_at)
);

INSERT INTO categories (name, type, user_id)
SELECT 'Ahorro', 'EXPENSE', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM categories
    WHERE name = 'Ahorro' AND type = 'EXPENSE' AND user_id IS NULL
);
