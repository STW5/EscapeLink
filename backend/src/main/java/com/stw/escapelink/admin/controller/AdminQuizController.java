package com.stw.escapelink.admin.controller;

import com.stw.escapelink.admin.dto.CreateQuizRequest;
import com.stw.escapelink.admin.dto.QuizAdminResponse;
import com.stw.escapelink.admin.service.AdminQuizService;
import com.stw.escapelink.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    public AdminQuizController(AdminQuizService adminQuizService) {
        this.adminQuizService = adminQuizService;
    }

    @PostMapping("/api/admin/games/{gameId}/quizzes")
    public ApiResponse<QuizAdminResponse> createQuiz(@PathVariable Long gameId,
                                                       @Valid @RequestBody CreateQuizRequest request) {
        return ApiResponse.ok(adminQuizService.createQuiz(gameId, request));
    }

    @GetMapping("/api/admin/games/{gameId}/quizzes")
    public ApiResponse<List<QuizAdminResponse>> listQuizzes(@PathVariable Long gameId) {
        return ApiResponse.ok(adminQuizService.listQuizzesForGame(gameId));
    }
}
