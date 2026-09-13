package com.stw.escapelink.quiz;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.domain.QuizSecret;
import com.stw.escapelink.quiz.domain.QuizType;
import com.stw.escapelink.quiz.dto.AnswerResponse;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.repository.QuizSecretRepository;
import com.stw.escapelink.quiz.service.QuizAnswerService;
import com.stw.escapelink.support.AbstractIntegrationTest;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizAnswerServiceConcurrencyTest extends AbstractIntegrationTest {

    private static final String CORRECT_ANSWER = "escape";

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizSecretRepository quizSecretRepository;
    @Autowired
    private QuizProgressRepository quizProgressRepository;
    @Autowired
    private QuizAnswerService quizAnswerService;

    private Game newRunningGame() {
        Instant now = Instant.now();
        return gameRepository.save(new Game("test-game", GameStatus.RUNNING, now, now.plusSeconds(3600)));
    }

    private Team newTeam(Game game, String inviteTokenSuffix) {
        return teamRepository.save(new Team(game.getId(), "team-" + inviteTokenSuffix, "invite-" + inviteTokenSuffix));
    }

    private Quiz newTextQuiz(Game game) {
        Quiz quiz = quizRepository.save(new Quiz(game.getId(), "q", "content", QuizType.TEXT, 1, "hint", 0));
        quizSecretRepository.save(new QuizSecret(quiz.getId(), CORRECT_ANSWER));
        return quiz;
    }

    @Test
    void concurrentCorrectSubmissionsCompleteExactlyOnce() throws Exception {
        Game game = newRunningGame();
        Team team = newTeam(game, "concurrent");
        Quiz quiz = newTextQuiz(game);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger alreadyCompletedCount = new AtomicInteger();

        List<Callable<Void>> tasks = List.of(
                submitTask(team.getId(), quiz.getId(), "req-A", startLatch, successCount, alreadyCompletedCount),
                submitTask(team.getId(), quiz.getId(), "req-B", startLatch, successCount, alreadyCompletedCount)
        );

        // submit() (not invokeAll, which blocks until every task finishes) so both
        // callables are parked on startLatch.await() before we release them together.
        List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
        startLatch.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "exactly one submission should transition to COMPLETED");
        assertEquals(1, alreadyCompletedCount.get(), "the other submission should observe QUIZ_ALREADY_COMPLETED");

        QuizProgress progress = quizProgressRepository.findByTeamIdAndQuizIdAndRunNo(team.getId(), quiz.getId(), 1)
                .orElseThrow();
        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.getSolvedAt()).isNotNull();
    }

    private Callable<Void> submitTask(Long teamId, Long quizId, String requestId, CountDownLatch startLatch,
                                       AtomicInteger successCount, AtomicInteger alreadyCompletedCount) {
        return () -> {
            startLatch.await();
            try {
                AnswerResponse response = quizAnswerService.submit(teamId, quizId, requestId, CORRECT_ANSWER, 1);
                if (response.correct()) {
                    successCount.incrementAndGet();
                }
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.QUIZ_ALREADY_COMPLETED) {
                    alreadyCompletedCount.incrementAndGet();
                } else {
                    throw e;
                }
            }
            return null;
        };
    }

    @Test
    void wrongAnswerTriggersCooldownForSubsequentSubmission() {
        Game game = newRunningGame();
        Team team = newTeam(game, "cooldown");
        Quiz quiz = newTextQuiz(game);

        assertThatThrownBy(() -> quizAnswerService.submit(team.getId(), quiz.getId(), "req-1", "wrong-answer", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertEquals(ErrorCode.WRONG_ANSWER, e.getErrorCode()));

        assertThatThrownBy(() -> quizAnswerService.submit(team.getId(), quiz.getId(), "req-2", "wrong-answer", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertEquals(ErrorCode.QUIZ_COOLDOWN, e.getErrorCode()));
    }

    @Test
    void duplicateRequestIdReplaysWithoutReprocessing() {
        Game game = newRunningGame();
        Team team = newTeam(game, "dedup");
        Quiz quiz = newTextQuiz(game);

        assertThatThrownBy(() -> quizAnswerService.submit(team.getId(), quiz.getId(), "same-request", "wrong-answer", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertEquals(ErrorCode.WRONG_ANSWER, e.getErrorCode()));

        // Same requestId replayed immediately: must not throw QUIZ_COOLDOWN, since it is
        // recognized as the same request rather than being reprocessed.
        AnswerResponse replay = quizAnswerService.submit(team.getId(), quiz.getId(), "same-request", "wrong-answer", 1);
        assertThat(replay.correct()).isFalse();
    }

    @Test
    void staleRunNoAfterResetIsRejected() {
        Game game = newRunningGame();
        Team team = newTeam(game, "reset");
        Quiz quiz = newTextQuiz(game);

        team.resetToNewRun();
        teamRepository.save(team);

        assertThatThrownBy(() -> quizAnswerService.submit(team.getId(), quiz.getId(), "req-stale", CORRECT_ANSWER, 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertEquals(ErrorCode.INVALID_RUN, e.getErrorCode()));
    }
}
