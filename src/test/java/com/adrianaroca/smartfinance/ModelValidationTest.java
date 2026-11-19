package com.adrianaroca.smartfinance;

import com.adrianaroca.smartfinance.model.Budget;
import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.RecurringMovement;
import com.adrianaroca.smartfinance.model.RegisterForm;
import com.adrianaroca.smartfinance.model.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {

        validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator =
                validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {

        validatorFactory.close();
    }

    @Test
    void incomeRejectsNegativeAmount() {

        Income income = new Income();

        income.setConcept("Salario");
        income.setAmount(new BigDecimal("-100"));
        income.setCategory("Trabajo");
        income.setDate(LocalDate.now());

        assertFalse(
                validator.validate(income).isEmpty()
        );
    }

    @Test
    void incomeRejectsMoreThanTwoDecimalPlaces() {

        Income income = new Income();

        income.setConcept("Salario");
        income.setAmount(new BigDecimal("100.123"));
        income.setCategory("Trabajo");
        income.setDate(LocalDate.now());

        assertFalse(
                validator.validate(income).isEmpty()
        );
    }

    @Test
    void incomeRejectsConceptLongerThan100Characters() {

        Income income = new Income();

        income.setConcept("a".repeat(101));
        income.setAmount(new BigDecimal("100"));
        income.setCategory("Trabajo");
        income.setDate(LocalDate.now());

        assertFalse(
                validator.validate(income).isEmpty()
        );
    }

    @Test
    void budgetRejectsZeroAmount() {

        Budget budget = new Budget();

        budget.setCategory("Alimentación");
        budget.setAmount(BigDecimal.ZERO);
        budget.setMonth(
                LocalDate.of(2026, 10, 1)
        );

        assertFalse(
                validator.validate(budget).isEmpty()
        );
    }

    @Test
    void recurringMovementRejectsDayOutsideRange() {

        RecurringMovement recurringMovement =
                new RecurringMovement();

        recurringMovement.setConcept("Netflix");
        recurringMovement.setAmount(
                new BigDecimal("15.99")
        );
        recurringMovement.setCategory("Ocio");
        recurringMovement.setType("EXPENSE");
        recurringMovement.setDayOfMonth(32);

        assertFalse(
                validator.validate(recurringMovement).isEmpty()
        );
    }

    @Test
    void recurringMovementAcceptsValidData() {

        RecurringMovement recurringMovement =
                new RecurringMovement();

        recurringMovement.setConcept("Netflix");
        recurringMovement.setAmount(
                new BigDecimal("15.99")
        );
        recurringMovement.setCategory("Ocio");
        recurringMovement.setType("EXPENSE");
        recurringMovement.setDayOfMonth(15);

        assertTrue(
                validator.validate(recurringMovement).isEmpty()
        );
    }

    @Test
    void userRejectsInvalidEmail() {

        User user = new User();

        user.setName("Adrián");
        user.setEmail("email-invalido");
        user.setPassword("password");

        assertFalse(
                validator.validate(user).isEmpty()
        );
    }

    @Test
    void userRejectsNameLongerThan100Characters() {

        User user = new User();

        user.setName("a".repeat(101));
        user.setEmail("adrian@test.com");
        user.setPassword("password");

        assertFalse(
                validator.validate(user).isEmpty()
        );
    }

    @Test
    void registerFormRejectsShortPassword() {

        RegisterForm registerForm =
                new RegisterForm();

        registerForm.setName("Adrián");
        registerForm.setEmail("adrian@test.com");
        registerForm.setPassword("1234567");

        assertFalse(
                validator.validate(registerForm).isEmpty()
        );
    }

    @Test
    void registerFormAcceptsEightCharacterPassword() {

        RegisterForm registerForm =
                new RegisterForm();

        registerForm.setName("Adrián");
        registerForm.setEmail("adrian@test.com");
        registerForm.setPassword("12345678");

        assertTrue(
                validator.validate(registerForm).isEmpty()
        );
    }
}