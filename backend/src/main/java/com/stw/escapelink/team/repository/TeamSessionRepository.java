package com.stw.escapelink.team.repository;

import com.stw.escapelink.team.domain.TeamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamSessionRepository extends JpaRepository<TeamSession, Long> {

    Optional<TeamSession> findBySessionToken(String sessionToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TeamSession s where s.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);
}
