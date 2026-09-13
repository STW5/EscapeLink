package com.stw.escapelink.team.dto;

import java.time.Instant;

/** Internal service-layer result; the session token is only ever placed in an HttpOnly cookie. */
public record JoinResult(
        TeamStateResponse state,
        String sessionToken,
        Instant sessionExpiresAt
) {
}
