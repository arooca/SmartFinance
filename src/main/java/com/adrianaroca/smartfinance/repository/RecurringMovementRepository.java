package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.RecurringMovement;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringMovementRepository
        extends JpaRepository<RecurringMovement, Long> {

    List<RecurringMovement>
    findAllByUserOrderByActiveDescNextExecutionDateAsc(User user);

    RecurringMovement findByIdAndUser(
            Long id,
            User user
    );

    void deleteByIdAndUser(
            Long id,
            User user
    );

    void deleteAllByUser(User user);

    List<RecurringMovement>
    findAllByActiveTrueAndNextExecutionDateLessThanEqual(
            LocalDate date
    );

    boolean existsByUserAndCategoryIgnoreCase(User user, String category);
}
