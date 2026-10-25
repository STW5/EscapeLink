package com.stw.escapelink.admin.service;

import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizSecretRepository;
import com.stw.escapelink.submission.repository.ImageSubmissionRepository;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import org.springframework.stereotype.Component;

/**
 * Removes the rows that reference a team or quiz before the row itself is deleted.
 * Uploaded image files are intentionally left on disk (audit trail / manual cleanup).
 */
@Component
class AdminContentCleaner {

    private final QuizProgressRepository quizProgressRepository;
    private final ImageSubmissionRepository imageSubmissionRepository;
    private final TeamSessionRepository teamSessionRepository;
    private final QuizSecretRepository quizSecretRepository;

    AdminContentCleaner(QuizProgressRepository quizProgressRepository,
                        ImageSubmissionRepository imageSubmissionRepository,
                        TeamSessionRepository teamSessionRepository,
                        QuizSecretRepository quizSecretRepository) {
        this.quizProgressRepository = quizProgressRepository;
        this.imageSubmissionRepository = imageSubmissionRepository;
        this.teamSessionRepository = teamSessionRepository;
        this.quizSecretRepository = quizSecretRepository;
    }

    void requireNoTeamActivityWhileRunning(boolean gameRunning, Long teamId) {
        if (gameRunning && (quizProgressRepository.existsByTeamId(teamId)
                || imageSubmissionRepository.existsByTeamId(teamId))) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE);
        }
    }

    void requireNoQuizActivityWhileRunning(boolean gameRunning, Long quizId) {
        if (gameRunning && (quizProgressRepository.existsByQuizId(quizId)
                || imageSubmissionRepository.existsByQuizId(quizId))) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE);
        }
    }

    void purgeTeam(Long teamId) {
        imageSubmissionRepository.deleteByTeamId(teamId);
        quizProgressRepository.deleteByTeamId(teamId);
        teamSessionRepository.deleteByTeamId(teamId);
    }

    void purgeQuiz(Long quizId) {
        imageSubmissionRepository.deleteByQuizId(quizId);
        quizProgressRepository.deleteByQuizId(quizId);
        quizSecretRepository.deleteByQuizId(quizId);
    }
}
