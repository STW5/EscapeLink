package com.stw.escapelink.global.config;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizSecret;
import com.stw.escapelink.quiz.domain.QuizType;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.repository.QuizSecretRepository;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds one Game/Team/Quiz so the join -> answer -> WebSocket vertical slice
 * can be exercised locally without the (not-yet-built) admin console.
 */
@Configuration
@Profile("dev")
public class DevDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String DEMO_INVITE_TOKEN = "demo-team-1";
    private static final String DEMO_ANSWER = "escape";

    @Bean
    public CommandLineRunner seedDemoData(GameRepository gameRepository,
                                           TeamRepository teamRepository,
                                           QuizRepository quizRepository,
                                           QuizSecretRepository quizSecretRepository) {
        return args -> {
            if (gameRepository.count() > 0) {
                return;
            }

            Instant now = Instant.now();
            Game game = gameRepository.save(
                    new Game("데모 게임", GameStatus.RUNNING, now, now.plus(2, ChronoUnit.HOURS)));

            Team team = teamRepository.save(new Team(game.getId(), "1팀", DEMO_INVITE_TOKEN));

            Quiz quiz = quizRepository.save(new Quiz(
                    game.getId(), "문제 1", "첫 번째 문제입니다. 정답을 입력하세요.",
                    QuizType.TEXT, 1, "정답은 영어 단어입니다.", 0));
            quizSecretRepository.save(new QuizSecret(quiz.getId(), DEMO_ANSWER));

            log.info("Dev demo data seeded: gameId={}, inviteToken={}, quizId={}, answer='{}'",
                    game.getId(), team.getInviteToken(), quiz.getId(), DEMO_ANSWER);
        };
    }
}
