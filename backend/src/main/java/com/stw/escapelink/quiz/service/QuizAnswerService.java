package com.stw.escapelink.quiz.service;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.config.QuizProperties;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizProgress;
import com.stw.escapelink.quiz.domain.QuizSecret;
import com.stw.escapelink.quiz.dto.AnswerResponse;
import com.stw.escapelink.quiz.repository.QuizProgressRepository;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.repository.QuizSecretRepository;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.service.FinalStageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class QuizAnswerService {

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final QuizSecretRepository quizSecretRepository;
    private final QuizProgressRepository quizProgressRepository;
    private final QuizProperties quizProperties;
    private final FinalStageService finalStageService;
    private final ApplicationEventPublisher eventPublisher;

    public QuizAnswerService(TeamRepository teamRepository, GameRepository gameRepository,
                              QuizRepository quizRepository, QuizSecretRepository quizSecretRepository,
                              QuizProgressRepository quizProgressRepository, QuizProperties quizProperties,
                              FinalStageService finalStageService, ApplicationEventPublisher eventPublisher) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.quizRepository = quizRepository;
        this.quizSecretRepository = quizSecretRepository;
        this.quizProgressRepository = quizProgressRepository;
        this.quizProperties = quizProperties;
        this.finalStageService = finalStageService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * noRollbackFor: every BusinessException thrown below (cooldown, already-completed,
     * wrong answer, ...) is a legitimate outcome, not a failure — the row created by
     * getOrCreateLocked and any cooldown/dedup bookkeeping must still be persisted.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AnswerResponse submit(Long teamId, Long quizId, String requestId, String rawAnswer, int runNo) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!team.isCurrentRun(runNo)) {
            throw new BusinessException(ErrorCode.INVALID_RUN);
        }

        Game game = gameRepository.findById(team.getGameId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!game.isRunning()) {
            throw new BusinessException(ErrorCode.GAME_NOT_RUNNING);
        }

        Quiz quiz = quizRepository.findById(quizId)
                .filter(q -> q.getGameId().equals(game.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        if (!quiz.isText()) {
            throw new BusinessException(ErrorCode.INVALID_QUIZ_TYPE);
        }

        QuizProgress progress = getOrCreateLocked(teamId, quizId, runNo);

        if (progress.isReplayOf(requestId)) {
            return new AnswerResponse(progress.lastRequestWasCorrect(), progress.getStatus(), progress.getSolvedAt());
        }

        if (progress.isCompleted()) {
            throw new BusinessException(ErrorCode.QUIZ_ALREADY_COMPLETED);
        }

        Instant now = Instant.now();
        int cooldownSeconds = quizProperties.getAnswerCooldownSeconds();
        if (progress.isCooldownActive(now, cooldownSeconds)) {
            long retryAfterSeconds = cooldownSeconds
                    - java.time.Duration.between(progress.getLastWrongAnswerAt(), now).getSeconds();
            throw new BusinessException(ErrorCode.QUIZ_COOLDOWN, Map.of("retryAfterSeconds", Math.max(retryAfterSeconds, 1)));
        }

        QuizSecret secret = quizSecretRepository.findByQuizId(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        boolean correct = secret.matches(QuizSecret.normalize(rawAnswer));

        if (correct) {
            progress.complete();
        } else {
            progress.recordWrongAnswer();
        }
        progress.recordRequest(requestId, correct);
        // flush now, not just save: the WebSocket event below reads getVersion(),
        // which Hibernate only bumps in-memory once the UPDATE is actually flushed.
        quizProgressRepository.saveAndFlush(progress);

        if (correct) {
            eventPublisher.publishEvent(new QuizCompletedEvent(teamId, quizId, progress.getVersion()));
            finalStageService.checkAndUnlockIfAllCompleted(teamId);
            return new AnswerResponse(true, progress.getStatus(), progress.getSolvedAt());
        }

        throw new BusinessException(ErrorCode.WRONG_ANSWER);
    }

    private QuizProgress getOrCreateLocked(Long teamId, Long quizId, int runNo) {
        quizProgressRepository.insertIfAbsent(teamId, quizId, runNo);
        return quizProgressRepository.findForUpdate(teamId, quizId, runNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
    }
}
