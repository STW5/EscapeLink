package com.stw.escapelink.admin.controller;

import com.stw.escapelink.admin.dto.CreateQuizRequest;
import com.stw.escapelink.admin.dto.QuizAdminResponse;
import com.stw.escapelink.admin.dto.UpdateQuizRequest;
import com.stw.escapelink.admin.service.AdminQuizService;
import com.stw.escapelink.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/api/admin/quizzes/{quizId}")
    public ApiResponse<QuizAdminResponse> updateQuiz(Authentication authentication, @PathVariable Long quizId,
                                                       @Valid @RequestBody UpdateQuizRequest request) {
        return ApiResponse.ok(adminQuizService.updateQuiz(authentication.getName(), quizId, request));
    }

    @DeleteMapping("/api/admin/quizzes/{quizId}")
    public ApiResponse<Void> deleteQuiz(Authentication authentication, @PathVariable Long quizId) {
        adminQuizService.deleteQuiz(authentication.getName(), quizId);
        return ApiResponse.ok();
    }

    @GetMapping("/api/admin/games/{gameId}/quizzes")
    public ApiResponse<List<QuizAdminResponse>> listQuizzes(@PathVariable Long gameId) {
        return ApiResponse.ok(adminQuizService.listQuizzesForGame(gameId));
    }
}
