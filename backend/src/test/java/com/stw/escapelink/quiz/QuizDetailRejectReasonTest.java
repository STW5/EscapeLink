package com.stw.escapelink.quiz;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizType;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.service.QuizQueryService;
import com.stw.escapelink.submission.domain.ImageSubmission;
import com.stw.escapelink.submission.repository.ImageSubmissionRepository;
import com.stw.escapelink.support.AbstractIntegrationTest;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class QuizDetailRejectReasonTest extends AbstractIntegrationTest {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private ImageSubmissionRepository imageSubmissionRepository;
    @Autowired
    private QuizQueryService quizQueryService;

    @Test
    void latestRejectionReasonIsShownUntilTheTeamSubmitsAgain() {
        Instant now = Instant.now();
        Game game = gameRepository.save(new Game("reject-reason", GameStatus.RUNNING, now, now.plusSeconds(3600)));
        Team team = teamRepository.save(new Team(game.getId(), "t", "tok-reject-reason"));
        Quiz quiz = quizRepository.save(new Quiz(game.getId(), "photo", "take a photo", QuizType.IMAGE, 1, null, 0));

        assertThat(quizQueryService.getDetail(team.getId(), quiz.getId()).rejectReason()).isNull();

        ImageSubmission first = imageSubmissionRepository.save(
                new ImageSubmission(team.getId(), quiz.getId(), team.getCurrentRunNo(), 1, "a.jpg"));
        first.reject("admin", "초점 흐림");
        imageSubmissionRepository.save(first);

        assertThat(quizQueryService.getDetail(team.getId(), quiz.getId()).rejectReason()).isEqualTo("초점 흐림");

        imageSubmissionRepository.save(new ImageSubmission(team.getId(), quiz.getId(), team.getCurrentRunNo(), 2, "b.jpg"));

        assertThat(quizQueryService.getDetail(team.getId(), quiz.getId()).rejectReason()).isNull();
    }
}
