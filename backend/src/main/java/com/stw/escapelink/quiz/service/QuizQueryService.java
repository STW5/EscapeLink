package com.stw.escapelink.quiz.service;

import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.domain.QuizProgressStatus;
import com.stw.escapelink.quiz.dto.QuizDetailResponse;
import com.stw.escapelink.quiz.dto.QuizListItemResponse;
import com.stw.escapelink.quiz.dto.QuizListResponse;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizQueryService {

    private final TeamRepository teamRepository;
    private final QuizRepository quizRepository;
    private final QuizProgressRepository quizProgressRepository;

    public QuizQueryService(TeamRepository teamRepository, QuizRepository quizRepository,
                             QuizProgressRepository quizProgressRepository) {
        this.teamRepository = teamRepository;
        this.quizRepository = quizRepository;
        this.quizProgressRepository = quizProgressRepository;
    }

    @Transactional(readOnly = true)
    public QuizListResponse listForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        List<Quiz> quizzes = quizRepository.findByGameIdOrderByOrderNoAsc(team.getGameId());
        Map<Long, QuizProgress> progressByQuizId = quizProgressRepository
                .findByTeamIdAndRunNo(team.getId(), team.getCurrentRunNo())
                .stream()
                .collect(Collectors.toMap(QuizProgress::getQuizId, Function.identity()));

        List<QuizListItemResponse> items = quizzes.stream()
                .map(quiz -> {
                    QuizProgress progress = progressByQuizId.get(quiz.getId());
                    QuizProgressStatus status = progress == null ? QuizProgressStatus.UNSOLVED : progress.getStatus();
                    return new QuizListItemResponse(quiz.getId(), quiz.getTitle(), quiz.getType(), status);
                })
                .toList();

        return new QuizListResponse(team.getGameId(), team.getId(), items);
    }

    /**
     * Viewing a quiz counts as "entering" it: the first team member to open this
     * page starts the hint-delay clock (see Quiz#isHintAvailableAt).
     */
    @Transactional
    public QuizDetailResponse getDetail(Long teamId, Long quizId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        Quiz quiz = quizRepository.findById(quizId)
                .filter(q -> q.getGameId().equals(team.getGameId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        quizProgressRepository.insertIfAbsent(teamId, quizId, team.getCurrentRunNo());
        QuizProgress progress = quizProgressRepository
                .findByTeamIdAndQuizIdAndRunNo(teamId, quizId, team.getCurrentRunNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));

        Instant firstEnteredAt = progress.getFirstEnteredAt();
        boolean hintAvailable = quiz.isHintAvailableAt(firstEnteredAt, Instant.now());
        Instant hintAvailableAt = firstEnteredAt == null
                ? null
                : firstEnteredAt.plusSeconds(quiz.getHintDelaySeconds());

        return new QuizDetailResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getContent(),
                quiz.getType(),
                progress.getStatus(),
                hintAvailable,
                hintAvailableAt,
                hintAvailable ? quiz.getHint() : null
        );
    }
}
