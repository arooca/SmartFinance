package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.ResourceNotFoundException;
import com.adrianaroca.smartfinance.model.*;
import com.adrianaroca.smartfinance.repository.GoalInvitationRepository;
import com.adrianaroca.smartfinance.repository.GoalNotificationRepository;
import com.adrianaroca.smartfinance.repository.SavingsGoalRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.GoalInvitationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class GoalInvitationServiceImpl implements GoalInvitationService {

    private static final int MAX_INVITEES_PER_ACTION = 10;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final List<GoalNotificationStatus> VISIBLE_NOTIFICATION_STATUSES =
            List.of(GoalNotificationStatus.UNREAD, GoalNotificationStatus.PENDING);

    private final GoalInvitationRepository invitationRepository;
    private final GoalNotificationRepository notificationRepository;
    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public GoalInvitationServiceImpl(GoalInvitationRepository invitationRepository,
                                     GoalNotificationRepository notificationRepository,
                                     SavingsGoalRepository goalRepository,
                                     UserRepository userRepository,
                                     CurrentUserProvider currentUserProvider) {
        this.invitationRepository = invitationRepository;
        this.notificationRepository = notificationRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void validateInviteEmails(Long goalId, String emails) {
        User user = currentUserProvider.getCurrentUser();
        SavingsGoal goal = goalId == null ? null : goalRepository.findVisibleById(goalId, user);
        if (goalId != null && (goal == null || !canManage(goal, user))) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        resolveInvitees(emails, user, goal);
    }

    @Override
    @Transactional
    public void createGoalAndInvite(SavingsGoal proposedGoal, String emails) {
        User inviter = currentUserProvider.getCurrentUser();
        List<User> invitees = resolveInvitees(emails, inviter, null);
        SavingsGoal goal = new SavingsGoal();
        goal.setName(proposedGoal.getName().trim());
        goal.setTargetAmount(proposedGoal.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setTargetDate(proposedGoal.getTargetDate());
        goal.setUser(inviter);
        goal.setActive(false);
        goal.setSettled(false);
        goal.addMember(inviter);
        goalRepository.save(goal);
        saveInvitations(goal, inviter, invitees);
    }

    @Override
    @Transactional
    public void inviteMembers(Long goalId, String emails) {
        User inviter = currentUserProvider.getCurrentUser();
        SavingsGoal goal = goalRepository.findVisibleById(goalId, inviter);
        if (goal == null || !canManage(goal, inviter)) {
            throw new ResourceNotFoundException("Meta de ahorro no encontrada");
        }
        if (goal.isSettled() || !goal.getTargetDate().isAfter(LocalDate.now(ZoneId.of("Europe/Madrid")))) {
            throw new IllegalArgumentException("No se pueden añadir participantes a una meta finalizada o vencida.");
        }
        saveInvitations(goal, inviter, resolveInvitees(emails, inviter, goal));
    }

    @Override
    public List<GoalInvitation> getPendingInvitations() {
        return invitationRepository.findAllByInviteeAndStatusOrderByCreatedAtDesc(
                currentUserProvider.getCurrentUser(), GoalInvitationStatus.PENDING);
    }

    @Override
    @Transactional
    public List<GoalNotification> getNotifications() {
        List<GoalNotification> notifications = notificationRepository
                .findAllByRecipientAndStatusInOrderByCreatedAtDesc(
                        currentUserProvider.getCurrentUser(), VISIBLE_NOTIFICATION_STATUSES);
        for (GoalNotification notification : notifications) {
            if (notification.getStatus() == GoalNotificationStatus.UNREAD) {
                notification.setStatus(GoalNotificationStatus.READ);
            }
        }
        return notifications;
    }

    @Override
    public long getPendingCount() {
        User user = currentUserProvider.getCurrentUser();
        return invitationRepository.countByInviteeAndStatus(user, GoalInvitationStatus.PENDING)
                + notificationRepository.countByRecipientAndStatusIn(user, VISIBLE_NOTIFICATION_STATUSES);
    }

    @Override
    @Transactional
    public void respond(Long invitationId, boolean accepted) {
        User invitee = currentUserProvider.getCurrentUser();
        GoalInvitation invitation = invitationRepository.findByIdAndInvitee(invitationId, invitee);
        if (invitation == null || invitation.getStatus() != GoalInvitationStatus.PENDING) {
            throw new ResourceNotFoundException("La invitación ya no está disponible.");
        }
        SavingsGoal goal = invitation.getGoal();
        if (accepted) {
            if (goal != null && (goal.isSettled()
                    || !goal.getTargetDate().isAfter(java.time.LocalDate.now(ZoneId.of("Europe/Madrid"))))) {
                throw new IllegalArgumentException("La fecha de esta meta ya ha llegado y no se puede aceptar la invitación.");
            }
            if (goal == null) {
                goal = new SavingsGoal();
                goal.setName(invitation.getName());
                goal.setTargetAmount(invitation.getTargetAmount());
                goal.setCurrentAmount(BigDecimal.ZERO);
                goal.setTargetDate(invitation.getTargetDate());
                goal.setUser(invitation.getInviter());
                goal.setActive(true);
                goal.addMember(invitation.getInviter());
            }
            goal.setActive(true);
            goal.addMember(invitee);
            if (!goal.isMember(goal.getUser().getId())) {
                goal.setUser(invitee);
            }
            goalRepository.save(goal);
            invitation.setGoal(goal);
            invitation.setStatus(GoalInvitationStatus.ACCEPTED);
            saveNotification(invitation.getInviter(), invitee, goal,
                    GoalNotificationType.MEMBER_JOINED, GoalNotificationStatus.UNREAD);
        } else {
            invitation.setStatus(GoalInvitationStatus.DECLINED);
        }
        invitationRepository.save(invitation);
        if (!accepted && goal != null && !goal.isActive()
                && invitationRepository.countByGoalAndStatus(goal, GoalInvitationStatus.PENDING) == 0) {
            goalRepository.delete(goal);
        }
    }

    @Override
    @Transactional
    public void respondToRemoval(Long notificationId, boolean remove) {
        User recipient = currentUserProvider.getCurrentUser();
        GoalNotification notification = notificationRepository.findByIdAndRecipient(notificationId, recipient);
        if (notification == null
                || notification.getType() != GoalNotificationType.GOAL_REMOVAL_REQUEST
                || notification.getStatus() != GoalNotificationStatus.PENDING) {
            throw new ResourceNotFoundException("La notificación ya no está disponible.");
        }
        SavingsGoal goal = notification.getGoal();
        if (remove && goal != null) {
            goal.removeMember(recipient.getId());
            if (goal.getMembers().isEmpty()) {
                goal.setActive(false);
                invitationRepository.deleteAllByGoalAndStatus(goal, GoalInvitationStatus.PENDING);
            } else if (!goal.isMember(goal.getUser().getId())) {
                goal.setUser(goal.getMembers().iterator().next());
            }
            goalRepository.save(goal);
        } else if (goal != null && !goal.isMember(goal.getUser().getId())) {
            goal.setUser(recipient);
            goalRepository.save(goal);
        }
        notification.setStatus(remove ? GoalNotificationStatus.REMOVED : GoalNotificationStatus.KEPT);
        notificationRepository.save(notification);
    }

    private List<User> resolveInvitees(String rawEmails, User inviter, SavingsGoal goal) {
        if (rawEmails == null || rawEmails.isBlank()) return List.of();
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        for (String raw : rawEmails.split("[,;\\r\\n]+")) {
            String email = raw.trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) continue;
            if (!EMAIL.matcher(email).matches() || email.length() > 254) {
                throw new IllegalArgumentException("Revisa los correos: sepáralos por coma, punto y coma o línea y usa direcciones válidas.");
            }
            emails.add(email);
        }
        if (emails.isEmpty()) return List.of();
        if (emails.size() > MAX_INVITEES_PER_ACTION) {
            throw new IllegalArgumentException("Puedes invitar como máximo a 10 personas a la vez.");
        }
        List<User> invitees = new ArrayList<>();
        for (String email : emails) {
            User invitee = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("No hay ninguna cuenta registrada con el correo " + email + "."));
            if (Objects.equals(inviter.getId(), invitee.getId())) {
                throw new IllegalArgumentException("No puedes invitarte a tu propia meta.");
            }
            if (goal != null && goal.isMember(invitee.getId())) {
                throw new IllegalArgumentException(invitee.getName() + " ya participa en esta meta.");
            }
            if (goal != null && invitationRepository.existsByGoalAndInviteeAndStatus(
                    goal, invitee, GoalInvitationStatus.PENDING)) {
                throw new IllegalArgumentException("Ya hay una invitación pendiente para " + invitee.getName() + ".");
            }
            invitees.add(invitee);
        }
        return invitees;
    }

    private void saveInvitations(SavingsGoal goal, User inviter, List<User> invitees) {
        for (User invitee : invitees) {
            GoalInvitation invitation = new GoalInvitation();
            invitation.setName(goal.getName());
            invitation.setTargetAmount(goal.getTargetAmount());
            invitation.setTargetDate(goal.getTargetDate());
            invitation.setInviter(inviter);
            invitation.setInvitee(invitee);
            invitation.setGoal(goal);
            invitation.setStatus(GoalInvitationStatus.PENDING);
            invitation.setCreatedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
        }
    }

    private void saveNotification(User recipient, User actor, SavingsGoal goal,
                                  GoalNotificationType type, GoalNotificationStatus status) {
        GoalNotification notification = new GoalNotification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setGoal(goal);
        notification.setGoalName(goal.getName());
        notification.setType(type);
        notification.setStatus(status);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private boolean canManage(SavingsGoal goal, User user) {
        return goal.isMember(user.getId())
                && (Objects.equals(goal.getUser().getId(), user.getId())
                || !goal.isMember(goal.getUser().getId()));
    }
}
