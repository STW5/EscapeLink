package com.stw.escapelink.quiz.controller;

import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.global.security.CurrentTeam;
import com.stw.escapelink.quiz.dto.AnswerRequest;
import com.stw.escapelink.quiz.dto.AnswerResponse;
import com.stw.escapelink.quiz.dto.QuizListResponse;
import com.stw.escapelink.quiz.service.QuizAnswerService;
import com.stw.escapelink.quiz.service.QuizQueryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizController {

    private final QuizQueryService quizQueryService;
    private final QuizAnswerService quizAnswerService;

    public QuizController(QuizQueryService quizQueryService, QuizAnswerService quizAnswerService) {
        this.quizQueryService = quizQueryService;
        this.quizAnswerService = quizAnswerService;
    }

    @GetMapping("/api/quizzes")
    public ApiResponse<QuizListResponse> list(@AuthenticationPrincipal CurrentTeam currentTeam) {
        return ApiResponse.ok(quizQueryService.listForTeam(currentTeam.teamId()));
    }

    @PostMapping("/api/quizzes/{quizId}/answer")
    public ApiResponse<AnswerResponse> answer(@AuthenticationPrincipal CurrentTeam currentTeam,
                                               @PathVariable Long quizId,
                                               @Valid @RequestBody AnswerRequest request) {
        AnswerResponse response = quizAnswerService.submit(
                currentTeam.teamId(), quizId, request.requestId(), request.answer(), request.runNo());
        return ApiResponse.ok(response);
    }
}
