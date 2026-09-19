package com.stw.escapelink.team.repository;

import com.stw.escapelink.team.domain.TeamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamSessionRepository extends JpaRepository<TeamSession, Long> {

    Optional<TeamSession> findBySessionToken(String sessionToken);
}
