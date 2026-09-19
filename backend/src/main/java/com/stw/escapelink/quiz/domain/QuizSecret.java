package com.stw.escapelink.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * Server-only table. Never mapped into a participant-facing DTO or joined
 * eagerly with {@link Quiz} — kept as a separate aggregate on purpose.
 */
@Entity
@Getter
@Table(name = "quiz_secret", uniqueConstraints = @UniqueConstraint(columnNames = "quiz_id"))
public class QuizSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false, updatable = false)
    private Long quizId;

    @Column(nullable = false)
    private String answer;

    protected QuizSecret() {
    }

    public QuizSecret(Long quizId, String answer) {
        this.quizId = quizId;
        this.answer = answer;
    }

    public boolean matches(String normalizedInput) {
        return normalize(answer).equals(normalizedInput);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }
}
