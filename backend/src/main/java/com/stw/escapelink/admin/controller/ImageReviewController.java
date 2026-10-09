package com.stw.escapelink.admin.controller;

import com.stw.escapelink.admin.dto.PendingSubmissionResponse;
import com.stw.escapelink.admin.dto.RejectSubmissionRequest;
import com.stw.escapelink.admin.service.ImageReviewService;
import com.stw.escapelink.global.exception.BusinessException;
import com.stw.escapelink.global.exception.ErrorCode;
import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.submission.domain.ImageSubmission;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Locale;

@RestController
public class ImageReviewController {

    private final ImageReviewService imageReviewService;

    public ImageReviewController(ImageReviewService imageReviewService) {
        this.imageReviewService = imageReviewService;
    }

    @GetMapping("/api/admin/submissions")
    public ApiResponse<List<PendingSubmissionResponse>> listPending() {
        return ApiResponse.ok(imageReviewService.listPending());
    }

    @GetMapping("/api/admin/submissions/{submissionId}/image")
    public ResponseEntity<Resource> image(@PathVariable Long submissionId) {
        ImageSubmission submission = imageReviewService.getSubmissionOrThrow(submissionId);
        File file = new File(submission.getFilePath());
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND);
        }

        return ResponseEntity.ok()
                .contentType(contentTypeOf(submission.getFilePath()))
                .body(new FileSystemResource(file));
    }

    @PostMapping("/api/admin/submissions/{submissionId}/approve")
    public ApiResponse<Void> approve(Authentication authentication, @PathVariable Long submissionId) {
        imageReviewService.approve(authentication.getName(), submissionId);
        return ApiResponse.ok();
    }

    @PostMapping("/api/admin/submissions/{submissionId}/reject")
    public ApiResponse<Void> reject(Authentication authentication, @PathVariable Long submissionId,
                                     @Valid @RequestBody RejectSubmissionRequest request) {
        imageReviewService.reject(authentication.getName(), submissionId, request.reason());
        return ApiResponse.ok();
    }

    private MediaType contentTypeOf(String filePath) {
        String lower = filePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
