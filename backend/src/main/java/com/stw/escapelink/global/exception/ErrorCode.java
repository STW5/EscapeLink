package com.stw.escapelink.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다."),
    INVALID_INVITE_TOKEN(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 접속해주세요."),
    GAME_NOT_RUNNING(HttpStatus.CONFLICT, "현재 진행 중인 게임이 아닙니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."),
    QUIZ_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 문제입니다."),
    QUIZ_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
    WRONG_ANSWER(HttpStatus.OK, "정답이 아닙니다."),
    INVALID_QUIZ_TYPE(HttpStatus.BAD_REQUEST, "해당 문제 유형에서는 지원하지 않는 요청입니다."),
    INVALID_RUN(HttpStatus.CONFLICT, "이전 회차의 요청입니다."),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 용량이 너무 큽니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 파일입니다."),
    FINAL_STAGE_NOT_UNLOCKED(HttpStatus.CONFLICT, "아직 최종 스테이지에 진입하지 않았습니다."),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "제출물을 찾을 수 없습니다."),
    SUBMISSION_ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 검수 처리된 제출물입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
