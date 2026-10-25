package com.stw.escapelink.submission.repository;

import com.stw.escapelink.submission.domain.ImageSubmission;
import com.stw.escapelink.submission.domain.ImageSubmissionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageSubmissionRepository extends JpaRepository<ImageSubmission, Long> {

    Optional<ImageSubmission> findTopByTeamIdAndQuizIdAndRunNoOrderBySubmissionVersionDesc(
            Long teamId, Long quizId, int runNo);

    List<ImageSubmission> findByStatusOrderByCreatedAtAsc(ImageSubmissionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ImageSubmission s where s.id = :submissionId")
    Optional<ImageSubmission> findForUpdate(@Param("submissionId") Long submissionId);

    @Query("select s.teamId from ImageSubmission s where s.id = :submissionId")
    Optional<Long> findTeamIdById(@Param("submissionId") Long submissionId);
}
