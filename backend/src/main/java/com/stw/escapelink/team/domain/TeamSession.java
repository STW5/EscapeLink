package com.stw.escapelink.team.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "team_session", uniqueConstraints = @UniqueConstraint(columnNames = "session_token"))
public class TeamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "session_token", nullable = false, updatable = false)
    private String sessionToken;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_access_at", nullable = false)
    private Instant lastAccessAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected TeamSession() {
    }

    public TeamSession(Long teamId, String sessionToken, String deviceId, Instant expiresAt) {
        this.teamId = teamId;
        this.sessionToken = sessionToken;
        this.deviceId = deviceId;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastAccessAt = now;
    }

    public void touch() {
        this.lastAccessAt = Instant.now();
    }

    public boolean isValid(Instant now) {
        return !revoked && now.isBefore(expiresAt);
    }
}
