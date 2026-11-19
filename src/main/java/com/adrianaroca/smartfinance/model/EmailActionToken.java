package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_action_tokens")
public class EmailActionToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailActionType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected EmailActionToken() { }

    public EmailActionToken(String tokenHash, EmailActionType type, User user, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.type = type;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash() { return tokenHash; }
    public EmailActionType getType() { return type; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}
