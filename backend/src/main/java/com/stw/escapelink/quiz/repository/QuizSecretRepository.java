package com.stw.escapelink.quiz.repository;

import com.stw.escapelink.quiz.domain.QuizSecret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSecretRepository extends JpaRepository<QuizSecret, Long> {

    Optional<QuizSecret> findByQuizId(Long quizId);
}
