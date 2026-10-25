package com.stw.escapelink.admin;

import com.stw.escapelink.admin.dto.CreateQuizRequest;
import com.stw.escapelink.admin.dto.UpdateGameRequest;
import com.stw.escapelink.admin.dto.UpdateQuizRequest;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.admin.service.AdminGameService;
import com.stw.escapelink.admin.service.AdminQuizService;
import com.stw.escapelink.admin.service.AdminTeamService;
import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.QuizType;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.repository.QuizSecretRepository;
import com.stw.escapelink.support.AbstractIntegrationTest;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.domain.TeamSession;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Edit/delete rules for games, teams and quizzes. Each test rolls back its own data. */
@Transactional
class AdminContentManagementServiceTest extends AbstractIntegrationTest {

    private static final String ADMIN = "tester";

    @Autowired
    private AdminGameService gameService;
    @Autowired
    private AdminTeamService teamService;
    @Autowired
    private AdminQuizService quizService;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamSessionRepository teamSessionRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizSecretRepository quizSecretRepository;
    @Autowired
    private QuizProgressRepository quizProgressRepository;
    @Autowired
    private AdminActionLogRepository actionLogRepository;

    private Game game(GameStatus status) {
        Instant now = Instant.now();
        return gameRepository.save(new Game("content-test", status, now, now.plusSeconds(3600)));
    }

    private Long quiz(Game game) {
        return quizService.createQuiz(game.getId(),
                new CreateQuizRequest("q", "content", QuizType.TEXT, 1, "hint", 0, "answer")).id();
    }

    private Long teamWithProgress(Game game, Long quizId, String token) {
        Team team = teamRepository.save(new Team(game.getId(), "team", token));
        quizProgressRepository.insertIfAbsent(team.getId(), quizId, 1);
        return team.getId();
    }

    @Test
    void updateQuizKeepsTheAnswerWhenBlankAndReplacesItWhenGiven() {
        Long quizId = quiz(game(GameStatus.READY));

        var kept = quizService.updateQuiz(ADMIN, quizId, new UpdateQuizRequest("new", "body", 3, null, 5, " "));
        assertThat(kept.title()).isEqualTo("new");
        assertThat(kept.orderNo()).isEqualTo(3);
        assertThat(kept.answer()).isEqualTo("answer");

        var changed = quizService.updateQuiz(ADMIN, quizId, new UpdateQuizRequest("new", "body", 3, null, 5, "other"));
        assertThat(changed.answer()).isEqualTo("other");
        assertThat(quizSecretRepository.findByQuizId(quizId).orElseThrow().getAnswer()).isEqualTo("other");
        assertThat(actionLogRepository.count()).isEqualTo(2);
    }

    @Test
    void runningGameKeepsQuizzesAndTeamsThatHaveProgress() {
        Game game = game(GameStatus.RUNNING);
        Long quizId = quiz(game);
        Long teamId = teamWithProgress(game, quizId, "tok-running-1");

        assertThatThrownBy(() -> quizService.deleteQuiz(ADMIN, quizId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE));
        assertThatThrownBy(() -> teamService.deleteTeam(ADMIN, teamId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE));
        assertThat(quizRepository.existsById(quizId)).isTrue();
        assertThat(teamRepository.existsById(teamId)).isTrue();
    }

    @Test
    void runningGameStillAllowsDeletingItemsWithoutProgress() {
        Game game = game(GameStatus.RUNNING);
        Long quizId = quiz(game);
        Team team = teamRepository.save(new Team(game.getId(), "idle", "tok-running-2"));

        quizService.deleteQuiz(ADMIN, quizId);
        teamService.deleteTeam(ADMIN, team.getId());

        assertThat(quizRepository.existsById(quizId)).isFalse();
        assertThat(quizSecretRepository.findByQuizId(quizId)).isEmpty();
        assertThat(teamRepository.existsById(team.getId())).isFalse();
    }

    @Test
    void finishedGameAllowsDeletingTeamsWithProgressAndTheirSessions() {
        Game game = game(GameStatus.FINISHED);
        Long quizId = quiz(game);
        Long teamId = teamWithProgress(game, quizId, "tok-finished");
        teamSessionRepository.save(new TeamSession(teamId, "session-finished", "dev", Instant.now().plusSeconds(60)));

        teamService.deleteTeam(ADMIN, teamId);

        assertThat(teamRepository.existsById(teamId)).isFalse();
        assertThat(teamSessionRepository.findBySessionToken("session-finished")).isEmpty();
        assertThat(quizProgressRepository.existsByTeamId(teamId)).isFalse();
    }

    @Test
    void regeneratingTheInviteTokenInvalidatesTheOldOne() {
        Game game = game(GameStatus.READY);
        Team team = teamRepository.save(new Team(game.getId(), "t", "tok-old"));

        var updated = teamService.regenerateInviteToken(ADMIN, team.getId());

        assertThat(updated.inviteToken()).isNotEqualTo("tok-old");
        assertThat(teamRepository.findByInviteToken("tok-old")).isEmpty();
        assertThat(teamRepository.findByInviteToken(updated.inviteToken())).isPresent();
    }

    @Test
    void deletingAGameRemovesItsContentButNotWhileRunning() {
        Game running = game(GameStatus.RUNNING);
        assertThatThrownBy(() -> gameService.deleteGame(ADMIN, running.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE));

        Game finished = game(GameStatus.FINISHED);
        Long quizId = quiz(finished);
        Long teamId = teamWithProgress(finished, quizId, "tok-game-delete");

        gameService.deleteGame(ADMIN, finished.getId());

        assertThat(gameRepository.existsById(finished.getId())).isFalse();
        assertThat(quizRepository.existsById(quizId)).isFalse();
        assertThat(teamRepository.existsById(teamId)).isFalse();
    }

    @Test
    void updateGameChangesTitleAndSchedule() {
        Game game = game(GameStatus.READY);
        Instant end = Instant.now().plusSeconds(7200);

        var updated = gameService.updateGame(ADMIN, game.getId(), new UpdateGameRequest("renamed", null, end));

        assertThat(updated.title()).isEqualTo("renamed");
        assertThat(updated.startAt()).isNull();
        assertThat(updated.endAt()).isEqualTo(end);
    }
}
