CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    user_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_categories_type_user (type, user_id)
);

INSERT INTO categories (name, type, user_id) VALUES
    ('Salario', 'INCOME', NULL),
    ('Freelance', 'INCOME', NULL),
    ('Inversión', 'INCOME', NULL),
    ('Otros', 'INCOME', NULL),
    ('Vivienda', 'EXPENSE', NULL),
    ('Alimentación', 'EXPENSE', NULL),
    ('Transporte', 'EXPENSE', NULL),
    ('Ocio', 'EXPENSE', NULL),
    ('Salud', 'EXPENSE', NULL),
    ('Otros', 'EXPENSE', NULL);

INSERT INTO categories (name, type, user_id)
SELECT DISTINCT i.category, 'INCOME', i.user_id
FROM incomes i
LEFT JOIN categories c
    ON c.name = i.category AND c.type = 'INCOME'
        AND (c.user_id = i.user_id OR c.user_id IS NULL)
WHERE c.id IS NULL;

INSERT INTO categories (name, type, user_id)
SELECT DISTINCT e.category, 'EXPENSE', e.user_id
FROM expenses e
LEFT JOIN categories c
    ON c.name = e.category AND c.type = 'EXPENSE'
        AND (c.user_id = e.user_id OR c.user_id IS NULL)
WHERE c.id IS NULL;

INSERT INTO categories (name, type, user_id)
SELECT DISTINCT b.category, 'EXPENSE', b.user_id
FROM budgets b
LEFT JOIN categories c
    ON c.name = b.category AND c.type = 'EXPENSE'
        AND (c.user_id = b.user_id OR c.user_id IS NULL)
WHERE c.id IS NULL;

INSERT INTO categories (name, type, user_id)
SELECT DISTINCT r.category, r.type, r.user_id
FROM recurring_movements r
LEFT JOIN categories c
    ON c.name = r.category AND c.type = r.type
        AND (c.user_id = r.user_id OR c.user_id IS NULL)
WHERE r.type IN ('INCOME', 'EXPENSE') AND c.id IS NULL;
