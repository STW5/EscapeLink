package com.stw.escapelink.team.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRequest(
        @NotBlank String inviteToken,
        String deviceId
) {
}
