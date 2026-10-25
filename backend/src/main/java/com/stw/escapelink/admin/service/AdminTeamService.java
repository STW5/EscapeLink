package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.domain.AdminAction;
import com.stw.escapelink.admin.domain.AdminActionLog;
import com.stw.escapelink.admin.dto.TeamSummaryResponse;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.global.security.SecureTokenGenerator;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.service.QuizQueryService;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.service.FinalStageService;
import com.stw.escapelink.team.service.TeamControlEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminTeamService {

    private static final int INVITE_TOKEN_BYTE_LENGTH = 9;

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final QuizProgressRepository quizProgressRepository;
    private final QuizQueryService quizQueryService;
    private final AdminActionLogRepository adminActionLogRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final FinalStageService finalStageService;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminContentCleaner contentCleaner;

    public AdminTeamService(TeamRepository teamRepository, GameRepository gameRepository,
                             QuizRepository quizRepository, QuizProgressRepository quizProgressRepository,
                             QuizQueryService quizQueryService, AdminActionLogRepository adminActionLogRepository,
                             SecureTokenGenerator secureTokenGenerator, FinalStageService finalStageService,
                             ApplicationEventPublisher eventPublisher, AdminContentCleaner contentCleaner) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.quizRepository = quizRepository;
        this.quizProgressRepository = quizProgressRepository;
        this.quizQueryService = quizQueryService;
        this.adminActionLogRepository = adminActionLogRepository;
        this.secureTokenGenerator = secureTokenGenerator;
        this.finalStageService = finalStageService;
        this.eventPublisher = eventPublisher;
        this.contentCleaner = contentCleaner;
    }

    @Transactional
    public TeamSummaryResponse createTeam(Long gameId, String name) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        String inviteToken = secureTokenGenerator.generate(INVITE_TOKEN_BYTE_LENGTH);
        Team team = teamRepository.save(new Team(game.getId(), name, inviteToken));
        return toSummary(team);
    }

    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams() {
        return teamRepository.findAll(Sort.by("id")).stream().map(this::toSummary).toList();
    }

    @Transactional
    public void forceCompleteQuiz(String adminUsername, Long teamId, Long quizId) {
        Team team = getTeamOrThrow(teamId);
        Quiz quiz = quizRepository.findById(quizId)
                .filter(q -> q.getGameId().equals(team.getGameId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        int runNo = team.getCurrentRunNo();
        quizProgressRepository.insertIfAbsent(teamId, quiz.getId(), runNo);
        QuizProgress progress = quizProgressRepository.findForUpdate(teamId, quiz.getId(), runNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        progress.complete();
        quizProgressRepository.save(progress);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.FORCE_COMPLETE,
                teamId, quiz.getId(), null));

        eventPublisher.publishEvent(TeamControlEvent.quizForceCompleted(teamId, quiz.getId()));
        finalStageService.checkAndUnlockIfAllCompleted(teamId);
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

    @Transactional
    public TeamSummaryResponse renameTeam(String adminUsername, Long teamId, String name) {
        Team team = getTeamOrThrow(teamId);
        String previous = team.getName();
        team.rename(name);
        teamRepository.save(team);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.UPDATE_TEAM,
                teamId, null, AdminLogDetails.of("name=" + previous + " -> " + name)));
        eventPublisher.publishEvent(new AdminDataChangedEvent("TEAM_UPDATED", teamId, null));
        return toSummary(team);
    }

    @Transactional
    public TeamSummaryResponse regenerateInviteToken(String adminUsername, Long teamId) {
        Team team = getTeamOrThrow(teamId);
        team.regenerateInviteToken(secureTokenGenerator.generate(INVITE_TOKEN_BYTE_LENGTH));
        teamRepository.save(team);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.REGENERATE_INVITE,
                teamId, null, null));
        eventPublisher.publishEvent(new AdminDataChangedEvent("TEAM_UPDATED", teamId, null));
        return toSummary(team);
    }

    /** While the game is running, a team that already has progress must be kept (reset it instead). */
    @Transactional
    public void deleteTeam(String adminUsername, Long teamId) {
        Team team = getTeamOrThrow(teamId);
        boolean running = gameRepository.findById(team.getGameId()).map(Game::isRunning).orElse(false);
        contentCleaner.requireNoTeamActivityWhileRunning(running, teamId);

        contentCleaner.purgeTeam(teamId);
        teamRepository.delete(team);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.DELETE_TEAM,
                teamId, null, AdminLogDetails.of("team=" + team.getName())));
        eventPublisher.publishEvent(new AdminDataChangedEvent("TEAM_DELETED", teamId, null));
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findForUpdate(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamSummaryResponse toSummary(Team team) {
        return new TeamSummaryResponse(
                team.getId(), team.getGameId(), team.getName(), team.getInviteToken(), team.getCurrentRunNo(),
                team.isFinalStageUnlocked(), team.isFinalStageForced(),
                quizQueryService.listForTeam(team.getId()));
    }
}
