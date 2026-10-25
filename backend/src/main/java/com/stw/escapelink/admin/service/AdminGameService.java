package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.domain.AdminAction;
import com.stw.escapelink.admin.domain.AdminActionLog;
import com.stw.escapelink.admin.dto.CreateGameRequest;
import com.stw.escapelink.admin.dto.UpdateGameRequest;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.stw.escapelink.admin.dto.GameResponse;
import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminGameService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final QuizRepository quizRepository;
    private final AdminContentCleaner contentCleaner;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminGameService(GameRepository gameRepository, TeamRepository teamRepository,
                             QuizRepository quizRepository, AdminContentCleaner contentCleaner,
                             AdminActionLogRepository adminActionLogRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.quizRepository = quizRepository;
        this.contentCleaner = contentCleaner;
        this.adminActionLogRepository = adminActionLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public GameResponse createGame(CreateGameRequest request) {
        Game game = gameRepository.save(
                new Game(request.title(), GameStatus.READY, request.startAt(), request.endAt()));
        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> listGames() {
        return gameRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GameResponse startGame(Long gameId) {
        Game game = getGameOrThrow(gameId);
        game.start();
        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse finishGame(Long gameId) {
        Game game = getGameOrThrow(gameId);
        game.finish();
        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse updateGame(String adminUsername, Long gameId, UpdateGameRequest request) {
        Game game = getGameOrThrow(gameId);
        game.update(request.title(), request.startAt(), request.endAt());
        gameRepository.save(game);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.UPDATE_GAME,
                null, null, AdminLogDetails.of("game=" + game.getTitle())));
        eventPublisher.publishEvent(new AdminDataChangedEvent("GAME_UPDATED", null, null));
        return toResponse(game);
    }

    /** A running game cannot be deleted; otherwise teams, quizzes and all their records go with it. */
    @Transactional
    public void deleteGame(String adminUsername, Long gameId) {
        Game game = getGameOrThrow(gameId);
        if (game.isRunning()) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE);
        }

        teamRepository.findByGameId(gameId).forEach(team -> {
            contentCleaner.purgeTeam(team.getId());
            teamRepository.delete(team);
        });
        quizRepository.findByGameIdOrderByOrderNoAsc(gameId).forEach(quiz -> {
            contentCleaner.purgeQuiz(quiz.getId());
            quizRepository.delete(quiz);
        });
        gameRepository.delete(game);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.DELETE_GAME,
                null, null, AdminLogDetails.of("game=" + game.getTitle())));
        eventPublisher.publishEvent(new AdminDataChangedEvent("GAME_DELETED", null, null));
    }

    private Game getGameOrThrow(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(game.getId(), game.getTitle(), game.getStatus(), game.getStartAt(), game.getEndAt());
    }
}
