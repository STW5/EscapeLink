package com.stw.escapelink.team.service;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.config.TeamSessionProperties;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.global.security.SecureTokenGenerator;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.domain.TeamSession;
import com.stw.escapelink.team.dto.JoinResult;
import com.stw.escapelink.team.dto.TeamStateResponse;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TeamSessionService {

    private static final int SESSION_TOKEN_BYTE_LENGTH = 32;

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final TeamSessionRepository teamSessionRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final TeamSessionProperties properties;

    public TeamSessionService(TeamRepository teamRepository,
                               GameRepository gameRepository,
                               TeamSessionRepository teamSessionRepository,
                               SecureTokenGenerator tokenGenerator,
                               TeamSessionProperties properties) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.teamSessionRepository = teamSessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
    }

    @Transactional
    public JoinResult join(String inviteToken, String deviceId) {
        Team team = teamRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_TOKEN));
        Game game = getGameOrThrow(team.getGameId());

        if (game.getStatus() == GameStatus.FINISHED) {
            throw new BusinessException(ErrorCode.GAME_NOT_RUNNING);
        }

        String token = tokenGenerator.generate(SESSION_TOKEN_BYTE_LENGTH);
        Instant expiresAt = Instant.now().plus(properties.getTtlHours(), ChronoUnit.HOURS);
        TeamSession session = new TeamSession(team.getId(), token, deviceId, expiresAt);
        teamSessionRepository.save(session);

        return new JoinResult(toState(team, game), token, expiresAt);
    }

    @Transactional(readOnly = true)
    public TeamStateResponse getState(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        Game game = getGameOrThrow(team.getGameId());
        return toState(team, game);
    }

    private Game getGameOrThrow(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamStateResponse toState(Team team, Game game) {
        return new TeamStateResponse(
                team.getId(),
                team.getName(),
                team.getCurrentRunNo(),
                game.getId(),
                game.getStatus(),
                game.getStartAt(),
                game.getEndAt(),
                team.isFinalStageUnlocked(),
                team.getFinalStageClearedAt()
        );
    }
}
