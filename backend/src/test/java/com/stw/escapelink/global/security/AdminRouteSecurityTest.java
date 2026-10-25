package com.stw.escapelink.global.security;

import com.stw.escapelink.game.domain.Game;
import com.stw.escapelink.game.domain.GameStatus;
import com.stw.escapelink.game.repository.GameRepository;
import com.stw.escapelink.support.AbstractIntegrationTest;
import com.stw.escapelink.team.domain.Team;
import com.stw.escapelink.team.domain.TeamSession;
import com.stw.escapelink.team.repository.TeamRepository;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class AdminRouteSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamSessionRepository teamSessionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void participantSessionCannotAccessAdminRoutes() throws Exception {
        Instant now = Instant.now();
        Game game = gameRepository.save(
                new Game("admin-security", GameStatus.RUNNING, now, now.plusSeconds(3600)));
        Team team = teamRepository.save(
                new Team(game.getId(), "participant", "admin-security-invite"));
        String token = "participant-admin-route-token";
        teamSessionRepository.save(new TeamSession(team.getId(), token, "test-device", now.plusSeconds(3600)));

        mockMvc.perform(get("/api/admin/teams")
                        .cookie(new Cookie("ESCAPELINK_TEAM_SESSION", token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminKeepsAccessWhenTheSameBrowserAlsoHoldsAParticipantCookie() throws Exception {
        Instant now = Instant.now();
        Game game = gameRepository.save(
                new Game("admin-and-team", GameStatus.RUNNING, now, now.plusSeconds(3600)));
        Team team = teamRepository.save(new Team(game.getId(), "participant", "admin-and-team-invite"));
        String token = "participant-and-admin-token";
        teamSessionRepository.save(new TeamSession(team.getId(), token, "test-device", now.plusSeconds(3600)));

        // An admin who also scanned a team QR on the same device must not be demoted to a participant.
        mockMvc.perform(get("/api/admin/teams")
                        .with(user("admin").roles("ADMIN"))
                        .cookie(new Cookie("ESCAPELINK_TEAM_SESSION", token)))
                .andExpect(status().isOk());
    }
}
