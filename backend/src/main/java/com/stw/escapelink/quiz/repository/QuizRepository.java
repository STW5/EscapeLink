package com.stw.escapelink.quiz.repository;

import com.stw.escapelink.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByGameIdOrderByOrderNoAsc(Long gameId);
}
