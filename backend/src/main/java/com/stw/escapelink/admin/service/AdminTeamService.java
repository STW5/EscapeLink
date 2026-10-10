package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.domain.AdminAction;
import com.stw.escapelink.admin.domain.AdminActionLog;
import com.stw.escapelink.admin.dto.TeamSummaryResponse;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.service.QuizQueryService;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminTeamService {

    private final TeamRepository teamRepository;
    private final QuizRepository quizRepository;
    private final QuizProgressRepository quizProgressRepository;
    private final QuizQueryService quizQueryService;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminTeamService(TeamRepository teamRepository, QuizRepository quizRepository,
                             QuizProgressRepository quizProgressRepository, QuizQueryService quizQueryService,
                             AdminActionLogRepository adminActionLogRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.teamRepository = teamRepository;
        this.quizRepository = quizRepository;
        this.quizProgressRepository = quizProgressRepository;
        this.quizQueryService = quizQueryService;
        this.adminActionLogRepository = adminActionLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams() {
        return teamRepository.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional
    public void forceCompleteQuiz(String adminUsername, Long teamId, Long quizId) {
        Team team = getTeamOrThrow(teamId);
        Quiz quiz = quizRepository.findById(quizId)
                .filter(q -> q.getGameId().equals(team.getGameId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        int runNo = team.getCurrentRunNo();
        quizProgressRepository.insertIfAbsent(teamId, quiz.getId(), runNo);
        QuizProgress progress = quizProgressRepository.findByTeamIdAndQuizIdAndRunNo(teamId, quiz.getId(), runNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        progress.complete();
        quizProgressRepository.save(progress);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.FORCE_COMPLETE,
                teamId, quiz.getId(), null));

        eventPublisher.publishEvent(TeamControlEvent.quizForceCompleted(teamId, quiz.getId()));
    }

    @Transactional
    public void resetTeam(String adminUsername, Long teamId) {
        Team team = getTeamOrThrow(teamId);
        team.resetToNewRun();
        teamRepository.save(team);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.RESET_TEAM,
                teamId, null, "newRunNo=" + team.getCurrentRunNo()));

        eventPublisher.publishEvent(TeamControlEvent.teamReset(teamId));
    }

    @Transactional
    public void forceFinalStage(String adminUsername, Long teamId) {
        Team team = getTeamOrThrow(teamId);
        team.forceFinalStage();
        teamRepository.save(team);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.FORCE_FINAL_STAGE,
                teamId, null, null));

        eventPublisher.publishEvent(TeamControlEvent.forceFinalStage(teamId));
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamSummaryResponse toSummary(Team team) {
        return new TeamSummaryResponse(
                team.getId(), team.getName(), team.getInviteToken(), team.getCurrentRunNo(),
                team.isFinalStageUnlocked(), team.isFinalStageForced(),
                quizQueryService.listForTeam(team.getId()));
    }
}
