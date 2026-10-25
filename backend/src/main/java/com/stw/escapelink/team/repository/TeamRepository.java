package com.stw.escapelink.team.repository;

import com.stw.escapelink.team.domain.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByInviteToken(String inviteToken);

    List<Team> findByGameId(Long gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Team t where t.id = :teamId")
    Optional<Team> findForUpdate(@Param("teamId") Long teamId);
}
