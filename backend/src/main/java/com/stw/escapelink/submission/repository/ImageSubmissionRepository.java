package com.stw.escapelink.submission.repository;

import com.stw.escapelink.submission.domain.ImageSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageSubmissionRepository extends JpaRepository<ImageSubmission, Long> {

    Optional<ImageSubmission> findTopByTeamIdAndQuizIdAndRunNoOrderBySubmissionVersionDesc(
            Long teamId, Long quizId, int runNo);
}
