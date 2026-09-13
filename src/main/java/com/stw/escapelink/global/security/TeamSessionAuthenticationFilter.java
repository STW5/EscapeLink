package com.stw.escapelink.global.security;

import com.stw.escapelink.global.config.TeamSessionProperties;
import com.stw.escapelink.team.domain.TeamSession;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Resolves the acting Team purely from the TeamSession cookie on every request.
 * A client-supplied teamId in the URL or body is never trusted for authorization.
 */
public class TeamSessionAuthenticationFilter extends OncePerRequestFilter {

    private final TeamSessionRepository teamSessionRepository;
    private final TeamSessionProperties properties;

    public TeamSessionAuthenticationFilter(TeamSessionRepository teamSessionRepository, TeamSessionProperties properties) {
        this.teamSessionRepository = teamSessionRepository;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        findCookie(request, properties.getCookieName())
                .flatMap(teamSessionRepository::findBySessionToken)
                .filter(session -> session.isValid(Instant.now()))
                .ifPresent(this::authenticate);

        filterChain.doFilter(request, response);
    }

    private void authenticate(TeamSession session) {
        session.touch();
        teamSessionRepository.save(session);
        CurrentTeam principal = new CurrentTeam(session.getTeamId(), session.getId());
        SecurityContextHolder.getContext().setAuthentication(new TeamSessionAuthenticationToken(principal));
    }

    private Optional<String> findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
