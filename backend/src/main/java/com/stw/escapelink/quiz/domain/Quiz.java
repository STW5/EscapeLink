package com.stw.escapelink.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * Never carries the answer — that lives in {@link QuizSecret}, a server-only table.
 */
@Entity
@Getter
@Table(name = "quiz")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuizType type;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    private String hint;

    @Column(name = "hint_delay_seconds", nullable = false)
    private int hintDelaySeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Quiz() {
    }

    public Quiz(Long gameId, String title, String content, QuizType type, int orderNo,
                String hint, int hintDelaySeconds) {
        this.gameId = gameId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.orderNo = orderNo;
        this.hint = hint;
        this.hintDelaySeconds = hintDelaySeconds;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isText() {
        return type == QuizType.TEXT;
    }

    public boolean isImage() {
        return type == QuizType.IMAGE;
    }

    public boolean isHintAvailableAt(Instant firstEnteredAt, Instant now) {
        if (firstEnteredAt == null) {
            return false;
        }
        return !now.isBefore(firstEnteredAt.plusSeconds(hintDelaySeconds));
    }
}
