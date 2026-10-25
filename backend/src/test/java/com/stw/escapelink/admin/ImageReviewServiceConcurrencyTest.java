package com.stw.escapelink.admin;

import com.stw.escapelink.admin.service.ImageReviewService;
import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.domain.QuizType;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.submission.domain.ImageSubmission;
import com.stw.escapelink.submission.domain.ImageSubmissionStatus;
import com.stw.escapelink.submission.repository.ImageSubmissionRepository;
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

class ImageReviewServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizProgressRepository quizProgressRepository;
    @Autowired
    private ImageSubmissionRepository imageSubmissionRepository;
    @Autowired
    private ImageReviewService imageReviewService;

    @Test
    void concurrentReviewsResolveASubmissionOnlyOnce() throws Exception {
        Instant now = Instant.now();
        Game game = gameRepository.save(
                new Game("review-concurrency", GameStatus.RUNNING, now, now.plusSeconds(3600)));
        Team team = teamRepository.save(
                new Team(game.getId(), "review-team", "review-concurrency-token"));
        Quiz quiz = quizRepository.save(
                new Quiz(game.getId(), "photo", "take a photo", QuizType.IMAGE, 1, null, 0));
        QuizProgress progress = new QuizProgress(team.getId(), quiz.getId(), team.getCurrentRunNo());
        progress.markPendingReview();
        quizProgressRepository.save(progress);
        ImageSubmission submission = imageSubmissionRepository.save(
                new ImageSubmission(team.getId(), quiz.getId(), team.getCurrentRunNo(), 1, "/tmp/review.jpg"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger approved = new AtomicInteger();
        AtomicInteger alreadyReviewed = new AtomicInteger();

        Callable<Void> reviewTask = () -> {
            ready.countDown();
            start.await();
            try {
                imageReviewService.approve("admin", submission.getId());
                approved.incrementAndGet();
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.SUBMISSION_ALREADY_REVIEWED) {
                    alreadyReviewed.incrementAndGet();
                } else {
                    throw e;
                }
            }
            return null;
        };

        List<Future<Void>> futures = List.of(executor.submit(reviewTask), executor.submit(reviewTask));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(approved.get()).isEqualTo(1);
        assertThat(alreadyReviewed.get()).isEqualTo(1);
        assertThat(imageSubmissionRepository.findById(submission.getId()).orElseThrow().getStatus())
                .isEqualTo(ImageSubmissionStatus.APPROVED);
    }
}
