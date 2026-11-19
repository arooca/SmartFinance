package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.ResourceNotFoundException;
import com.adrianaroca.smartfinance.model.*;
import com.adrianaroca.smartfinance.repository.GoalContributionRepository;
import com.adrianaroca.smartfinance.repository.GoalInvitationRepository;
import com.adrianaroca.smartfinance.repository.GoalNotificationRepository;
import com.adrianaroca.smartfinance.repository.SavingsGoalRepository;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.SavingsGoalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SavingsGoalServiceImpl implements SavingsGoalService {

    private final SavingsGoalRepository repository;
    private final GoalContributionRepository contributionRepository;
    private final GoalInvitationRepository invitationRepository;
    private final GoalNotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public SavingsGoalServiceImpl(SavingsGoalRepository repository,
                                  GoalContributionRepository contributionRepository,
                                  GoalInvitationRepository invitationRepository,
                                  GoalNotificationRepository notificationRepository,
                                  CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.contributionRepository = contributionRepository;
        this.invitationRepository = invitationRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<SavingsGoal> getGoals() {
        return repository.findAllVisibleTo(currentUserProvider.getCurrentUser());
    }

    @Override
    public SavingsGoal findById(Long id) {
        SavingsGoal goal = repository.findVisibleById(id, currentUserProvider.getCurrentUser());
        if (goal == null) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        return goal;
    }

    @Override
    public SavingsGoal findOwnedById(Long id) {
        User user = currentUserProvider.getCurrentUser();
        SavingsGoal goal = repository.findVisibleById(id, user);
        if (goal == null || !canManage(goal, user)) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        return goal;
    }

    @Override
    @Transactional
    public SavingsGoal save(SavingsGoal goal) {
        User user = currentUserProvider.getCurrentUser();
        if (goal.getId() == null) {
            goal.setCurrentAmount(BigDecimal.ZERO);
            goal.setParticipant(null);
            goal.setSettled(false);
            goal.setActive(true);
            goal.getMembers().clear();
            goal.addMember(user);
        } else {
            SavingsGoal existing = repository.findVisibleById(goal.getId(), user);
            if (existing == null || existing.isSettled() || !canManage(existing, user)) {
                throw new ResourceNotFoundException("Meta de ahorro no encontrada");
            }
            // Editing goal details must not reset contributions already recorded.
            goal.setCurrentAmount(existing.getCurrentAmount());
            goal.setParticipant(existing.getParticipant());
            goal.setUser(existing.getUser());
            goal.setActive(existing.isActive());
            goal.setMembers(existing.getMembers());
        }
        goal.setUser(user);
        return repository.save(goal);
    }

    @Override
    @Transactional
    public SavingsGoal addContribution(Long id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("La aportación debe ser mayor que 0");
        }
        BigDecimal normalizedAmount = amount.stripTrailingZeros();
        if (Math.max(normalizedAmount.scale(), 0) > 2
                || Math.max(normalizedAmount.precision() - normalizedAmount.scale(), 0) > 10) {
            throw new IllegalArgumentException("La aportación admite hasta 10 enteros y 2 decimales.");
        }
        User user = currentUserProvider.getCurrentUser();
        SavingsGoal goal = repository.findVisibleById(id, user);
        if (goal == null) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        if (goal.isSettled() || !goal.getTargetDate().isAfter(today)) {
            throw new IllegalArgumentException("Esta meta ya alcanzó su fecha y no admite nuevas aportaciones.");
        }
        BigDecimal updatedTotal = goal.getCurrentAmount().add(amount);
        if (updatedTotal.compareTo(goal.getTargetAmount()) > 0) {
            throw new IllegalArgumentException("La aportación supera el importe que falta para alcanzar la meta.");
        }
        GoalContribution contribution = new GoalContribution();
        contribution.setGoal(goal);
        contribution.setUser(user);
        contribution.setAmount(amount);
        contribution.setContributedAt(today);
        contributionRepository.save(contribution);
        goal.setCurrentAmount(updatedTotal);
        return repository.save(goal);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = currentUserProvider.getCurrentUser();
        SavingsGoal goal = repository.findVisibleById(id, user);
        if (goal == null) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        if (goal.isSettled()) {
            throw new IllegalArgumentException("Una meta liquidada no se puede eliminar.");
        }
        if (!goal.getTargetDate().isAfter(LocalDate.now(ZoneId.of("Europe/Madrid")))) {
            throw new IllegalArgumentException("No se puede eliminar una meta que ya alcanzó su fecha objetivo.");
        }
        if (!goal.isMember(user.getId())) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        List<User> otherMembers = goal.getMembers().stream()
                .filter(member -> !user.getId().equals(member.getId())).toList();
        if (!otherMembers.isEmpty()) {
            for (User member : otherMembers) {
                GoalNotification notification = new GoalNotification();
                notification.setRecipient(member);
                notification.setActor(user);
                notification.setGoal(goal);
                notification.setGoalName(goal.getName());
                notification.setType(GoalNotificationType.GOAL_REMOVAL_REQUEST);
                notification.setStatus(GoalNotificationStatus.PENDING);
                notification.setCreatedAt(java.time.LocalDateTime.now());
                notificationRepository.save(notification);
            }
            goal.removeMember(user.getId());
            repository.save(goal);
            return;
        }
        if (contributionRepository.countByGoal(goal) > 0) {
            throw new IllegalArgumentException("No se puede eliminar una meta con aportaciones registradas.");
        }
        invitationRepository.deleteAllByGoalAndStatus(goal, GoalInvitationStatus.PENDING);
        repository.delete(goal);
    }

    private boolean canManage(SavingsGoal goal, User user) {
        return goal.isMember(user.getId())
                && (user.getId().equals(goal.getUser().getId())
                || !goal.isMember(goal.getUser().getId()));
    }
}
