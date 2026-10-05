package com.stw.escapelink.submission.domain;

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

@Entity
@Getter
@Table(name = "image_submission")
public class ImageSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "run_no", nullable = false)
    private int runNo;

    @Column(name = "submission_version", nullable = false)
    private int submissionVersion;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ImageSubmissionStatus status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Admin username. Plain string for now — Admin auth lands in a later phase. */
    @Column(name = "reviewed_by")
    private String reviewedBy;

    protected ImageSubmission() {
    }

    public ImageSubmission(Long teamId, Long quizId, int runNo, int submissionVersion, String filePath) {
        this.teamId = teamId;
        this.quizId = quizId;
        this.runNo = runNo;
        this.submissionVersion = submissionVersion;
        this.filePath = filePath;
        this.status = ImageSubmissionStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isPending() {
        return status == ImageSubmissionStatus.PENDING;
    }

    public void approve(String adminUsername) {
        this.status = ImageSubmissionStatus.APPROVED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = adminUsername;
    }

    public void reject(String adminUsername, String reason) {
        this.status = ImageSubmissionStatus.REJECTED;
        this.rejectReason = reason;
        this.reviewedAt = Instant.now();
        this.reviewedBy = adminUsername;
    }
}
