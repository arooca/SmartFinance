package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la meta es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "El importe objetivo es obligatorio")
    @Positive(message = "El objetivo debe ser mayor que 0")
    @Digits(integer = 10, fraction = 2, message = "El objetivo admite hasta 10 enteros y 2 decimales")
    @Column(name = "target_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @Digits(integer = 10, fraction = 2, message = "El ahorro admite hasta 10 enteros y 2 decimales")
    @Column(name = "current_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @NotNull(message = "La fecha objetivo es obligatoria")
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "participant_id")
    private User participant;

    @Column(nullable = false)
    private boolean settled;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "goal_memberships",
            joinColumns = @JoinColumn(name = "goal_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new LinkedHashSet<>();

    @Transient
    @jakarta.validation.constraints.Size(max = 2540, message = "La lista de correos es demasiado larga")
    private String inviteEmail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> members) { this.members = members == null ? new LinkedHashSet<>() : members; }
    public boolean isMember(Long userId) {
        return userId != null && members.stream().anyMatch(member -> userId.equals(member.getId()));
    }
    @Transient
    public boolean canManage(Long userId) {
        return isMember(userId) && (user != null && user.getId().equals(userId)
                || user != null && !isMember(user.getId()));
    }
    public void addMember(User member) {
        if (member != null && !isMember(member.getId())) members.add(member);
    }
    public void removeMember(Long userId) {
        if (userId != null) members.removeIf(member -> userId.equals(member.getId()));
    }
    public List<User> getOtherMembers() {
        return members.stream().filter(member -> user == null || !user.getId().equals(member.getId())).toList();
    }
    public String getInviteEmail() { return inviteEmail; }
    public void setInviteEmail(String inviteEmail) { this.inviteEmail = inviteEmail == null ? null : inviteEmail.trim(); }

    @Transient
    public boolean isShared() { return getOtherMembers().size() > 0; }

    @Transient
    public String getParticipantName() {
        return participant == null ? "" : participant.getName();
    }

    @Transient
    public boolean isOwnedBy(User currentUser) {
        return user != null && currentUser != null && user.getId().equals(currentUser.getId());
    }

    @Transient
    public double getPercentage() {
        if (targetAmount == null || targetAmount.signum() <= 0 || currentAmount == null) {
            return 0;
        }
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 1, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Transient
    public boolean isCompleted() {
        return targetAmount != null && currentAmount != null
                && currentAmount.compareTo(targetAmount) >= 0;
    }
}
