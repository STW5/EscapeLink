package com.stw.escapelink.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "quiz_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "quiz_id", "run_no"}))
public class QuizProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "run_no", nullable = false)
    private int runNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuizProgressStatus status;

    @Column(name = "first_entered_at")
    private Instant firstEnteredAt;

    @Column(name = "solved_at")
    private Instant solvedAt;

    @Column(name = "last_wrong_answer_at")
    private Instant lastWrongAnswerAt;

    @Column(name = "last_request_id")
    private String lastRequestId;

    @Column(name = "last_request_correct")
    private Boolean lastRequestCorrect;

    @Version
    private long version;

    protected QuizProgress() {
    }

    public QuizProgress(Long teamId, Long quizId, int runNo) {
        this.teamId = teamId;
        this.quizId = quizId;
        this.runNo = runNo;
        this.status = QuizProgressStatus.UNSOLVED;
    }

    @PrePersist
    void onCreate() {
        if (this.firstEnteredAt == null) {
            this.firstEnteredAt = Instant.now();
        }
    }

    public void markEnteredIfFirst() {
        if (this.firstEnteredAt == null) {
            this.firstEnteredAt = Instant.now();
        }
    }

    public boolean isCompleted() {
        return status == QuizProgressStatus.COMPLETED;
    }

    public boolean isCooldownActive(Instant now, int cooldownSeconds) {
        return lastWrongAnswerAt != null && now.isBefore(lastWrongAnswerAt.plusSeconds(cooldownSeconds));
    }

    public void recordWrongAnswer() {
        this.lastWrongAnswerAt = Instant.now();
    }

    /** Idempotent: solvedAt is only ever set once, on the first successful completion. */
    public void complete() {
        if (this.status == QuizProgressStatus.COMPLETED) {
            return;
        }
        this.status = QuizProgressStatus.COMPLETED;
        this.solvedAt = Instant.now();
    }

    public boolean isReplayOf(String requestId) {
        return requestId != null && requestId.equals(this.lastRequestId);
    }

    public boolean lastRequestWasCorrect() {
        return Boolean.TRUE.equals(this.lastRequestCorrect);
    }

    public void recordRequest(String requestId, boolean correct) {
        this.lastRequestId = requestId;
        this.lastRequestCorrect = correct;
    }

    public void markPendingReview() {
        this.status = QuizProgressStatus.PENDING;
    }

    public void revertToUnsolved() {
        if (this.status == QuizProgressStatus.COMPLETED) {
            return;
        }
        this.status = QuizProgressStatus.UNSOLVED;
    }
}
