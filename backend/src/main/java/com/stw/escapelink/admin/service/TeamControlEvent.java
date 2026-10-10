package com.stw.escapelink.admin.service;

/** Raised only after the controlling transaction commits. quizId is null when not applicable. */
public record TeamControlEvent(Long teamId, String type, Long quizId) {

    public static TeamControlEvent quizForceCompleted(Long teamId, Long quizId) {
        return new TeamControlEvent(teamId, "QUIZ_FORCE_COMPLETED", quizId);
    }

    public static TeamControlEvent teamReset(Long teamId) {
        return new TeamControlEvent(teamId, "TEAM_RESET", null);
    }

    public static TeamControlEvent forceFinalStage(Long teamId) {
        return new TeamControlEvent(teamId, "FORCE_FINAL_STAGE", null);
    }
}
