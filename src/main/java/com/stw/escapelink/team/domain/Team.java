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
@Table(name = "team", uniqueConstraints = @UniqueConstraint(columnNames = "invite_token"))
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private String name;

    @Column(name = "invite_token", nullable = false, updatable = false)
    private String inviteToken;

    @Column(name = "current_run_no", nullable = false)
    private int currentRunNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Team() {
    }

    public Team(Long gameId, String name, String inviteToken) {
        this.gameId = gameId;
        this.name = name;
        this.inviteToken = inviteToken;
        this.currentRunNo = 1;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * Advances to a fresh run instead of deleting history, so stale in-flight
     * requests carrying the old runNo are rejected rather than corrupting state.
     */
    public void resetToNewRun() {
        this.currentRunNo += 1;
    }

    public boolean isCurrentRun(int runNo) {
        return this.currentRunNo == runNo;
    }
}
