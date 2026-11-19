ALTER TABLE savings_goals
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE goal_memberships (
    goal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (goal_id, user_id),
    CONSTRAINT fk_goal_memberships_goal
        FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_goal_memberships_user (user_id, goal_id)
);

INSERT INTO goal_memberships (goal_id, user_id)
SELECT id, user_id FROM savings_goals;

INSERT INTO goal_memberships (goal_id, user_id)
SELECT id, participant_id
FROM savings_goals
WHERE participant_id IS NOT NULL AND participant_id <> user_id;

ALTER TABLE goal_invitations
    ADD COLUMN goal_id BIGINT NULL,
    ADD CONSTRAINT fk_goal_invitations_goal
        FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE SET NULL,
    ADD INDEX idx_goal_invitations_goal_invitee_status (goal_id, invitee_id, status);

CREATE TABLE goal_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    goal_id BIGINT NULL,
    goal_name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_goal_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_notifications_actor
        FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_goal_notifications_goal
        FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE SET NULL,
    INDEX idx_goal_notifications_recipient_status (recipient_id, status, created_at)
);
