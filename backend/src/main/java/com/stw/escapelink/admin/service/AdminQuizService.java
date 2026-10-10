package com.stw.escapelink.admin.service;

import com.stw.escapelink.admin.dto.CreateQuizRequest;
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

    public AdminQuizService(GameRepository gameRepository, QuizRepository quizRepository,
                             QuizSecretRepository quizSecretRepository) {
        this.gameRepository = gameRepository;
        this.quizRepository = quizRepository;
        this.quizSecretRepository = quizSecretRepository;
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

    private QuizAdminResponse toResponse(Quiz quiz, String answer) {
        return new QuizAdminResponse(quiz.getId(), quiz.getGameId(), quiz.getTitle(), quiz.getContent(),
                quiz.getType(), quiz.getOrderNo(), quiz.getHint(), quiz.getHintDelaySeconds(), answer);
    }
}
