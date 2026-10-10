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

    @Column(name = "final_stage_unlocked", nullable = false)
    private boolean finalStageUnlocked;

    /** True only when an admin opened the final stage before all quizzes were solved. */
    @Column(name = "final_stage_forced", nullable = false)
    private boolean finalStageForced;

    @Column(name = "final_stage_entered_at")
    private Instant finalStageEnteredAt;

    /** Set once, on the first successful clear. Never overwritten after that. */
    @Column(name = "final_stage_cleared_at")
    private Instant finalStageClearedAt;

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
     * Final-stage state is run-scoped, so it resets along with the quizzes.
     */
    public void resetToNewRun() {
        this.currentRunNo += 1;
        this.finalStageUnlocked = false;
        this.finalStageForced = false;
        this.finalStageEnteredAt = null;
        this.finalStageClearedAt = null;
    }

    public boolean isCurrentRun(int runNo) {
        return this.currentRunNo == runNo;
    }

    /**
     * Admin override: opens the final stage regardless of quiz completion.
     * Persisted (not just a WebSocket event) so it survives reconnects.
     */
    public void forceFinalStage() {
        this.finalStageUnlocked = true;
        this.finalStageForced = true;
        if (this.finalStageEnteredAt == null) {
            this.finalStageEnteredAt = Instant.now();
        }
    }

    /** Earned path: all quizzes completed normally, as opposed to an admin override. */
    public void unlockFinalStage() {
        this.finalStageUnlocked = true;
        if (this.finalStageEnteredAt == null) {
            this.finalStageEnteredAt = Instant.now();
        }
    }

    /** Idempotent: clearedAt is only ever set once, on the first successful clear. */
    public void clearFinalStage() {
        if (this.finalStageClearedAt == null) {
            this.finalStageClearedAt = Instant.now();
        }
    }
}
