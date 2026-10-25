package com.stw.escapelink.global.websocket.dto;

/** Notify-only hint for the admin console: something changed, refetch the snapshot. */
public record AdminMonitorMessage(String type, Long teamId, Long quizId) {
}
