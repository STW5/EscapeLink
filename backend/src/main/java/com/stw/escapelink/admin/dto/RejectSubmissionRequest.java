package com.stw.escapelink.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectSubmissionRequest(
        @NotBlank String reason
) {
}
