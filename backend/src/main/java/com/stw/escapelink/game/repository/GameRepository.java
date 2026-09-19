package com.stw.escapelink.game.repository;

import com.stw.escapelink.game.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
