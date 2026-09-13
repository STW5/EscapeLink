package com.stw.escapelink.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "escapelink.quiz")
public class QuizProperties {

    private int answerCooldownSeconds = 10;

    public int getAnswerCooldownSeconds() {
        return answerCooldownSeconds;
    }

    public void setAnswerCooldownSeconds(int answerCooldownSeconds) {
        this.answerCooldownSeconds = answerCooldownSeconds;
    }
}
