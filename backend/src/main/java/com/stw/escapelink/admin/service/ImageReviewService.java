package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.domain.AdminAction;
import com.stw.escapelink.admin.domain.AdminActionLog;
import com.stw.escapelink.admin.dto.PendingSubmissionResponse;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.submission.domain.ImageSubmission;
import com.stw.escapelink.submission.domain.ImageSubmissionStatus;
import com.stw.escapelink.submission.repository.ImageSubmissionRepository;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.service.FinalStageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ImageReviewService {

    private final ImageSubmissionRepository imageSubmissionRepository;
    private final QuizProgressRepository quizProgressRepository;
    private final TeamRepository teamRepository;
    private final QuizRepository quizRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final FinalStageService finalStageService;
    private final ApplicationEventPublisher eventPublisher;

    public ImageReviewService(ImageSubmissionRepository imageSubmissionRepository,
                               QuizProgressRepository quizProgressRepository,
                               TeamRepository teamRepository, QuizRepository quizRepository,
                               AdminActionLogRepository adminActionLogRepository,
                               FinalStageService finalStageService,
                               ApplicationEventPublisher eventPublisher) {
        this.imageSubmissionRepository = imageSubmissionRepository;
        this.quizProgressRepository = quizProgressRepository;
        this.teamRepository = teamRepository;
        this.quizRepository = quizRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.finalStageService = finalStageService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<PendingSubmissionResponse> listPending() {
        List<ImageSubmission> pending = imageSubmissionRepository
                .findByStatusOrderByCreatedAtAsc(ImageSubmissionStatus.PENDING);

        // A superseded submission must stay hidden even after the newer one it
        // lost to gets reviewed — "latest" is checked against ALL versions ever
        // created for that (team, quiz, run), not just the other pending ones.
        return pending.stream()
                .filter(this::isCurrentRun)
                .filter(this::isLatestVersion)
                .sorted(Comparator.comparing(ImageSubmission::getCreatedAt))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImageSubmission getSubmissionOrThrow(Long submissionId) {
        return imageSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    @Transactional
    public void approve(String adminUsername, Long submissionId) {
        ReviewContext review = getReviewableSubmission(submissionId);
        ImageSubmission submission = review.submission();
        QuizProgress progress = review.progress();
        progress.complete();
        quizProgressRepository.save(progress);

        submission.approve(adminUsername);
        imageSubmissionRepository.save(submission);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.APPROVE_IMAGE,
                submission.getTeamId(), submission.getQuizId(), "submissionId=" + submission.getId()));

        eventPublisher.publishEvent(
                new ImageReviewedEvent(submission.getTeamId(), submission.getQuizId(), true, null));
        finalStageService.checkAndUnlockIfAllCompleted(submission.getTeamId());
    }

    @Transactional
    public void reject(String adminUsername, Long submissionId, String reason) {
        ReviewContext review = getReviewableSubmission(submissionId);
        ImageSubmission submission = review.submission();
        QuizProgress progress = review.progress();
        progress.revertToUnsolved();
        quizProgressRepository.save(progress);

        submission.reject(adminUsername, reason);
        imageSubmissionRepository.save(submission);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.REJECT_IMAGE,
                submission.getTeamId(), submission.getQuizId(), reason));

        eventPublisher.publishEvent(
                new ImageReviewedEvent(submission.getTeamId(), submission.getQuizId(), false, reason));
    }

    private ReviewContext getReviewableSubmission(Long submissionId) {
        Long teamId = imageSubmissionRepository.findTeamIdById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
        Team team = teamRepository.findForUpdate(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        ImageSubmission submission = imageSubmissionRepository.findForUpdate(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
        if (!team.isCurrentRun(submission.getRunNo())) {
            throw new BusinessException(ErrorCode.INVALID_RUN);
        }

        QuizProgress progress = quizProgressRepository.findForUpdate(
                        submission.getTeamId(), submission.getQuizId(), submission.getRunNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        if (!submission.isPending()) {
            throw new BusinessException(ErrorCode.SUBMISSION_ALREADY_REVIEWED);
        }

        if (!isLatestVersion(submission)) {
            // A newer resubmission superseded this one — treat it as moot, not reviewable.
            throw new BusinessException(ErrorCode.SUBMISSION_ALREADY_REVIEWED);
        }

        return new ReviewContext(submission, progress);
    }

    private boolean isCurrentRun(ImageSubmission submission) {
        return teamRepository.findById(submission.getTeamId())
                .map(team -> team.isCurrentRun(submission.getRunNo()))
                .orElse(false);
    }

    private boolean isLatestVersion(ImageSubmission submission) {
        return imageSubmissionRepository
                .findTopByTeamIdAndQuizIdAndRunNoOrderBySubmissionVersionDesc(
                        submission.getTeamId(), submission.getQuizId(), submission.getRunNo())
                .map(latest -> latest.getId().equals(submission.getId()))
                .orElse(false);
    }

    private PendingSubmissionResponse toResponse(ImageSubmission submission) {
        String teamName = teamRepository.findById(submission.getTeamId())
                .map(Team::getName).orElse("(알 수 없음)");
        String quizTitle = quizRepository.findById(submission.getQuizId())
                .map(Quiz::getTitle).orElse("(알 수 없음)");

        return new PendingSubmissionResponse(
                submission.getId(), submission.getTeamId(), teamName,
                submission.getQuizId(), quizTitle, submission.getSubmissionVersion(), submission.getCreatedAt());
    }

    private record ReviewContext(ImageSubmission submission, QuizProgress progress) {
    }
}
