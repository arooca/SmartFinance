package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.SavingsGoal;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    @Query("select distinct g from SavingsGoal g join g.members goalMember where goalMember = :user and g.active = true order by g.targetDate asc")
    List<SavingsGoal> findAllVisibleTo(@Param("user") User user);

    @Query("select distinct g from SavingsGoal g join g.members goalMember where g.id = :id and goalMember = :user and g.active = true")
    SavingsGoal findVisibleById(@Param("id") Long id, @Param("user") User user);

    SavingsGoal findByIdAndUser(Long id, User user);

    List<SavingsGoal> findAllByActiveTrueAndSettledFalseAndTargetDateLessThanEqual(java.time.LocalDate date);

    List<SavingsGoal> findAllByMembersContaining(User user);

    void deleteByIdAndUser(Long id, User user);
}
