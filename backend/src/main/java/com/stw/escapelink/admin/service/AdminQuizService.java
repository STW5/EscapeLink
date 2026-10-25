package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.domain.AdminAction;
import com.stw.escapelink.admin.domain.AdminActionLog;
import com.stw.escapelink.admin.dto.CreateQuizRequest;
import com.stw.escapelink.admin.dto.UpdateQuizRequest;
import com.stw.escapelink.admin.repository.AdminActionLogRepository;
import com.stw.escapelink.game.domain.Game;
import org.springframework.context.ApplicationEventPublisher;
import com.stw.escapelink.admin.dto.QuizAdminResponse;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.quiz.domain.Quiz;
import com.stw.escapelink.quiz.domain.QuizSecret;
import com.stw.escapelink.quiz.domain.QuizType;
import com.stw.escapelink.quiz.repository.QuizRepository;
import com.stw.escapelink.quiz.repository.QuizSecretRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminQuizService {

    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final QuizSecretRepository quizSecretRepository;
    private final AdminContentCleaner contentCleaner;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminQuizService(GameRepository gameRepository, QuizRepository quizRepository,
                             QuizSecretRepository quizSecretRepository, AdminContentCleaner contentCleaner,
                             AdminActionLogRepository adminActionLogRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.quizRepository = quizRepository;
        this.quizSecretRepository = quizSecretRepository;
        this.contentCleaner = contentCleaner;
        this.adminActionLogRepository = adminActionLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public QuizAdminResponse createQuiz(Long gameId, CreateQuizRequest request) {
        gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (request.type() == QuizType.TEXT && (request.answer() == null || request.answer().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "TEXT 문제는 정답이 필요합니다.", Map.of("field", "answer"));
        }

        Quiz quiz = quizRepository.save(new Quiz(gameId, request.title(), request.content(),
                request.type(), request.orderNo(), request.hint(), request.hintDelaySeconds()));

        String answer = null;
        if (request.type() == QuizType.TEXT) {
            quizSecretRepository.save(new QuizSecret(quiz.getId(), request.answer()));
            answer = request.answer();
        }

        return toResponse(quiz, answer);
    }

    @Transactional(readOnly = true)
    public List<QuizAdminResponse> listQuizzesForGame(Long gameId) {
        return quizRepository.findByGameIdOrderByOrderNoAsc(gameId).stream()
                .map(quiz -> toResponse(quiz, quizSecretRepository.findByQuizId(quiz.getId())
                        .map(QuizSecret::getAnswer)
                        .orElse(null)))
                .toList();
    }

    /** Text, order and hint may change at any time (typo fixes mid-event); the quiz type may not. */
    @Transactional
    public QuizAdminResponse updateQuiz(String adminUsername, Long quizId, UpdateQuizRequest request) {
        Quiz quiz = getQuizOrThrow(quizId);
        quiz.update(request.title(), request.content(), request.orderNo(), request.hint(), request.hintDelaySeconds());
        quizRepository.save(quiz);

        String answer = null;
        if (quiz.isText()) {
            QuizSecret secret = quizSecretRepository.findByQuizId(quizId).orElse(null);
            if (request.answer() != null && !request.answer().isBlank()) {
                if (secret == null) {
                    secret = new QuizSecret(quizId, request.answer());
                } else {
                    secret.changeAnswer(request.answer());
                }
                quizSecretRepository.save(secret);
            }
            answer = secret == null ? null : secret.getAnswer();
        }

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.UPDATE_QUIZ,
                null, quizId, null));
        eventPublisher.publishEvent(new AdminDataChangedEvent("QUIZ_UPDATED", null, quizId));
        return toResponse(quiz, answer);
    }

    @Transactional
    public void deleteQuiz(String adminUsername, Long quizId) {
        Quiz quiz = getQuizOrThrow(quizId);
        boolean running = gameRepository.findById(quiz.getGameId()).map(Game::isRunning).orElse(false);
        contentCleaner.requireNoQuizActivityWhileRunning(running, quizId);

        contentCleaner.purgeQuiz(quizId);
        quizRepository.delete(quiz);

        adminActionLogRepository.save(new AdminActionLog(adminUsername, AdminAction.DELETE_QUIZ,
                null, quizId, AdminLogDetails.of("quiz=" + quiz.getTitle())));
        eventPublisher.publishEvent(new AdminDataChangedEvent("QUIZ_DELETED", null, quizId));
    }

    private Quiz getQuizOrThrow(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
    }

    private QuizAdminResponse toResponse(Quiz quiz, String answer) {
        return new QuizAdminResponse(quiz.getId(), quiz.getGameId(), quiz.getTitle(), quiz.getContent(),
                quiz.getType(), quiz.getOrderNo(), quiz.getHint(), quiz.getHintDelaySeconds(), answer);
    }
}
