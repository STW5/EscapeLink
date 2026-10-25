package com.stw.escapelink.team;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.support.AbstractIntegrationTest;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.service.FinalStageService;
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

import static org.assertj.core.api.Assertions.assertThat;

class FinalStageServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private FinalStageService finalStageService;

    @Test
    void concurrentClearRequestsKeepTheFirstTimestamp() throws Exception {
        Instant now = Instant.now();
        Game game = gameRepository.save(
                new Game("final-concurrency", GameStatus.RUNNING, now, now.plusSeconds(3600)));
        Team team = new Team(game.getId(), "final-team", "final-concurrency-token");
        team.forceFinalStage();
        team = teamRepository.save(team);

        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        Long teamId = team.getId();

        Callable<Instant> clearTask = () -> {
            ready.countDown();
            start.await();
            return finalStageService.clearFinalStage(teamId);
        };

        List<Future<Instant>> futures = java.util.stream.IntStream.range(0, requestCount)
                .mapToObj(ignored -> executor.submit(clearTask))
                .toList();
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Instant> results = futures.stream().map(future -> {
            try {
                return future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }).toList();
        executor.shutdown();

        assertThat(results).containsOnly(results.getFirst());
        assertThat(teamRepository.findById(teamId).orElseThrow().getFinalStageClearedAt())
                .isEqualTo(results.getFirst());
    }
}
