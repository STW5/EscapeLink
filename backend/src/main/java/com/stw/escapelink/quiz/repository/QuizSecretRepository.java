package com.stw.escapelink.quiz.repository;

import com.stw.escapelink.quiz.domain.QuizSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizSecretRepository extends JpaRepository<QuizSecret, Long> {

    Optional<QuizSecret> findByQuizId(Long quizId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from QuizSecret s where s.quizId = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);
}
