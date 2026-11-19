package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "goal_notifications")
public class GoalNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    private SavingsGoal goal;

    @Column(name = "goal_name", nullable = false, length = 100)
    private String goalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GoalNotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalNotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public SavingsGoal getGoal() { return goal; }
    public void setGoal(SavingsGoal goal) { this.goal = goal; }
    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }
    public GoalNotificationType getType() { return type; }
    public void setType(GoalNotificationType type) { this.type = type; }
    public GoalNotificationStatus getStatus() { return status; }
    public void setStatus(GoalNotificationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
