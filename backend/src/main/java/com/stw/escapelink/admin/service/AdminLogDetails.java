package com.stw.escapelink.admin.service;

/** admin_action_log.detail is varchar(255). */
final class AdminLogDetails {

    private static final int MAX_LENGTH = 255;

    private AdminLogDetails() {
    }

    static String of(String detail) {
        return detail.length() <= MAX_LENGTH ? detail : detail.substring(0, MAX_LENGTH);
    }
}
