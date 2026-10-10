package com.stw.escapelink.leaderboard.service;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.leaderboard.dto.LeaderboardEntry;
import com.stw.escapelink.leaderboard.dto.LeaderboardResponse;
import com.stw.escapelink.quiz.domain.QuizProgressStatus;
import com.stw.escapelink.quiz.dto.QuizListResponse;
import com.stw.escapelink.quiz.service.QuizQueryService;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class LeaderboardService {

    private static final Comparator<LeaderboardEntry> RANKING = (a, b) -> {
        boolean aCleared = a.clearTime() != null;
        boolean bCleared = b.clearTime() != null;
        if (aCleared != bCleared) {
            return aCleared ? -1 : 1;
        }
        if (aCleared) {
            return a.clearTime().compareTo(b.clearTime());
        }
        int byCompleted = Integer.compare(b.completedQuizCount(), a.completedQuizCount());
        if (byCompleted != 0) {
            return byCompleted;
        }
        return a.teamName().compareTo(b.teamName());
    };

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final QuizQueryService quizQueryService;

    public LeaderboardService(GameRepository gameRepository, TeamRepository teamRepository,
                               QuizQueryService quizQueryService) {
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.quizQueryService = quizQueryService;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(Long gameId) {
        Game game = resolveGame(gameId);
        if (game == null) {
            return new LeaderboardResponse(null, null, List.of());
        }

        List<LeaderboardEntry> entries = teamRepository.findByGameId(game.getId()).stream()
                .map(this::toEntry)
                .sorted(RANKING)
                .toList();

        return new LeaderboardResponse(game.getId(), game.getTitle(), entries);
    }

    private Game resolveGame(Long gameId) {
        if (gameId != null) {
            return gameRepository.findById(gameId).orElse(null);
        }
        // Multiple games can be RUNNING at once if an admin forgets to finish an
        // old one before starting the next — prefer the most recently *started*
        // one, not just whichever happens to come first in repository order.
        List<Game> games = gameRepository.findAll();
        return games.stream()
                .filter(Game::isRunning)
                .max(Comparator.comparing(Game::getUpdatedAt))
                .orElseGet(() -> games.stream().max(Comparator.comparing(Game::getCreatedAt)).orElse(null));
    }

    private LeaderboardEntry toEntry(Team team) {
        QuizListResponse quizzes = quizQueryService.listForTeam(team.getId());
        int total = quizzes.quizzes().size();
        int completed = (int) quizzes.quizzes().stream()
                .filter(q -> q.status() == QuizProgressStatus.COMPLETED)
                .count();

        return new LeaderboardEntry(
                team.getName(), completed, total, team.isFinalStageUnlocked(), team.getFinalStageClearedAt());
    }
}
