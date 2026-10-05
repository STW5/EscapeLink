package com.stw.escapelink.submission.controller;

import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.global.security.CurrentTeam;
import com.stw.escapelink.submission.dto.ImageSubmissionResponse;
import com.stw.escapelink.submission.service.ImageSubmissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageSubmissionController {

    private final ImageSubmissionService imageSubmissionService;

    public ImageSubmissionController(ImageSubmissionService imageSubmissionService) {
        this.imageSubmissionService = imageSubmissionService;
    }

    @PostMapping(value = "/api/quizzes/{quizId}/image", consumes = "multipart/form-data")
    public ApiResponse<ImageSubmissionResponse> submit(@AuthenticationPrincipal CurrentTeam currentTeam,
                                                         @PathVariable Long quizId,
                                                         @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(imageSubmissionService.submit(currentTeam.teamId(), quizId, file));
    }
}
