package com.stw.escapelink.team.service;

import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.QuizProgressStatus;
import com.stw.escapelink.quiz.dto.QuizListResponse;
import com.stw.escapelink.quiz.service.QuizQueryService;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FinalStageService {

    private final TeamRepository teamRepository;
    private final QuizQueryService quizQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public FinalStageService(TeamRepository teamRepository, QuizQueryService quizQueryService,
                              ApplicationEventPublisher eventPublisher) {
        this.teamRepository = teamRepository;
        this.quizQueryService = quizQueryService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Call right after any quiz transitions to COMPLETED (text answer, image
     * approval, or admin force-complete). If every quiz for the team is now
     * complete, the final stage unlocks on its own — unless an admin already
     * force-opened it, in which case there's nothing left to unlock.
     */
    @Transactional
    public void checkAndUnlockIfAllCompleted(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (team.isFinalStageUnlocked()) {
            return;
        }

        QuizListResponse quizzes = quizQueryService.listForTeam(teamId);
        boolean allCompleted = !quizzes.quizzes().isEmpty()
                && quizzes.quizzes().stream().allMatch(q -> q.status() == QuizProgressStatus.COMPLETED);
        if (!allCompleted) {
            return;
        }

        team.unlockFinalStage();
        teamRepository.save(team);
        eventPublisher.publishEvent(TeamControlEvent.finalStageEnabled(teamId));
    }

    /** Idempotent: a repeated clear request never changes the recorded clear time. */
    @Transactional
    public Instant clearFinalStage(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!team.isFinalStageUnlocked()) {
            throw new BusinessException(ErrorCode.FINAL_STAGE_NOT_UNLOCKED);
        }

        boolean alreadyCleared = team.getFinalStageClearedAt() != null;
        team.clearFinalStage();
        teamRepository.save(team);

        if (!alreadyCleared) {
            eventPublisher.publishEvent(TeamControlEvent.finalStageCleared(teamId));
        }
        return team.getFinalStageClearedAt();
    }
}
