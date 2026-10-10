package com.stw.escapelink.global.websocket.dto;

public record TeamControlMessage(String type, Long quizId) {

    public static TeamControlMessage of(String type, Long quizId) {
        return new TeamControlMessage(type, quizId);
    }
}
