package com.stw.escapelink.global.security;

/** Authenticated principal for a participant device, resolved from its TeamSession cookie. */
public record CurrentTeam(Long teamId, Long teamSessionId) {
}
