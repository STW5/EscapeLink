package com.stw.escapelink.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** Audit trail: who did what, to which team/quiz, and when. Never updated or deleted. */
@Entity
@Getter
@Table(name = "admin_action_log")
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", nullable = false)
    private String adminUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAction action;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "quiz_id")
    private Long quizId;

    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminActionLog() {
    }

    public AdminActionLog(String adminUsername, AdminAction action, Long teamId, Long quizId, String detail) {
        this.adminUsername = adminUsername;
        this.action = action;
        this.teamId = teamId;
        this.quizId = quizId;
        this.detail = detail;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
