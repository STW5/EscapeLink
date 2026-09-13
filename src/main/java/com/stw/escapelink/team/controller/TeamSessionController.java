package com.stw.escapelink.team.controller;

import com.stw.escapelink.global.config.TeamSessionProperties;
import com.stw.escapelink.global.response.ApiResponse;
import com.stw.escapelink.team.dto.JoinRequest;
import com.stw.escapelink.team.dto.JoinResult;
import com.stw.escapelink.team.dto.TeamStateResponse;
import com.stw.escapelink.team.service.TeamSessionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
public class TeamSessionController {

    private final TeamSessionService teamSessionService;
    private final TeamSessionProperties properties;

    public TeamSessionController(TeamSessionService teamSessionService, TeamSessionProperties properties) {
        this.teamSessionService = teamSessionService;
        this.properties = properties;
    }

    @PostMapping("/api/team-sessions/join")
    public ApiResponse<TeamStateResponse> join(@Valid @RequestBody JoinRequest request, HttpServletResponse response) {
        JoinResult result = teamSessionService.join(request.inviteToken(), request.deviceId());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(result.sessionToken(), result.sessionExpiresAt()).toString());
        return ApiResponse.ok(result.state());
    }

    private ResponseCookie buildCookie(String token, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        return ResponseCookie.from(properties.getCookieName(), token)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }
}
