package com.stw.escapelink.team.repository;

import com.stw.escapelink.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByInviteToken(String inviteToken);
}
