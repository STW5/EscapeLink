package com.stw.escapelink.quiz.repository;

import com.stw.escapelink.quiz.domain.QuizProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizProgressRepository extends JpaRepository<QuizProgress, Long> {

    Optional<QuizProgress> findByTeamIdAndQuizIdAndRunNo(Long teamId, Long quizId, int runNo);

    List<QuizProgress> findByTeamIdAndRunNo(Long teamId, int runNo);

    /**
     * Row-locks the progress record for the duration of the answer-submission
     * transaction so two devices on the same team racing to submit the same
     * quiz serialize on this row instead of both flipping it to COMPLETED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from QuizProgress p where p.teamId = :teamId and p.quizId = :quizId and p.runNo = :runNo")
    Optional<QuizProgress> findForUpdate(@Param("teamId") Long teamId, @Param("quizId") Long quizId, @Param("runNo") int runNo);

    /**
     * ON CONFLICT DO NOTHING instead of catch-and-retry: a plain unique-constraint
     * violation would abort the surrounding Postgres transaction, so first-entry
     * races between two devices are resolved with an atomic upsert-style insert
     * instead of exception-driven control flow.
     */
    @Modifying
    @Query(value = """
            insert into quiz_progress (team_id, quiz_id, run_no, status, first_entered_at, version)
            values (:teamId, :quizId, :runNo, 'UNSOLVED', now(), 0)
            on conflict (team_id, quiz_id, run_no) do nothing
            """, nativeQuery = true)
    void insertIfAbsent(@Param("teamId") Long teamId, @Param("quizId") Long quizId, @Param("runNo") int runNo);
}
