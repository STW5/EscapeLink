package com.stw.escapelink.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameTeamRequest(
        @NotBlank String name
) {
}
