package com.adrianaroca.smartfinance;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMigrationTest {

    @Test
    void migrationSeedsDefaultsAndImportsExistingNamesWithoutChangingRows()
            throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(
                "jdbc:h2:mem:category-migration;MODE=MySQL;NON_KEYWORDS=MONTH;DB_CLOSE_DELAY=-1"
        );

        Flyway.configure()
                .dataSource(dataSource)
                .target("1")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (name, email, password) VALUES "
                    + "('Ada', 'ada@example.com', 'password')");
            statement.executeUpdate("INSERT INTO incomes "
                    + "(concept, amount, category, date) VALUES "
                    + "('Ingreso antiguo', 25.00, 'Consultoría', DATE '2026-01-01')");
            statement.executeUpdate("INSERT INTO expenses "
                    + "(concept, amount, category, date, user_id) VALUES "
                    + "('Gasto antiguo', 12.00, 'Mascotas', DATE '2026-01-02', "
                    + "(SELECT id FROM users WHERE email = 'ada@example.com'))");
            statement.executeUpdate("INSERT INTO recurring_movements "
                    + "(concept, amount, category, type, day_of_month, active) VALUES "
                    + "('Ingreso recurrente', 30.00, 'Alquiler de plaza', 'INCOME', 1, TRUE)");
        }

        Flyway.configure()
                .dataSource(dataSource)
                .target("2")
                .load()
                .migrate();

        Set<String> categories = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT name, type FROM categories")) {
            while (result.next()) {
                categories.add(result.getString("name") + ":" + result.getString("type"));
            }
        }

        assertTrue(categories.contains("Salario:INCOME"));
        assertTrue(categories.contains("Alimentación:EXPENSE"));
        assertTrue(categories.contains("Consultoría:INCOME"));
        assertTrue(categories.contains("Mascotas:EXPENSE"));
        assertTrue(categories.contains("Alquiler de plaza:INCOME"));

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT c.user_id FROM categories c WHERE c.name = 'Mascotas'")) {
            assertTrue(result.next());
            assertEquals(1L, result.getLong("user_id"));
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT category FROM incomes WHERE concept = 'Ingreso antiguo'")) {
            assertTrue(result.next());
            assertEquals("Consultoría", result.getString("category"));
        }
    }
}
