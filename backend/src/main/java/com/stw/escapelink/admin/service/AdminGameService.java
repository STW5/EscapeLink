package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.dto.CreateGameRequest;
import com.stw.escapelink.admin.dto.GameResponse;
import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminGameService {

    private final GameRepository gameRepository;

    public AdminGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional
    public GameResponse createGame(CreateGameRequest request) {
        Game game = gameRepository.save(
                new Game(request.title(), GameStatus.READY, request.startAt(), request.endAt()));
        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> listGames() {
        return gameRepository.findAll().stream().map(this::toResponse).toList();
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

    private Game getGameOrThrow(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(game.getId(), game.getTitle(), game.getStatus(), game.getStartAt(), game.getEndAt());
    }
}
