package com.stw.escapelink.global.websocket;

import com.stw.escapelink.global.config.TeamSessionProperties;
import com.stw.escapelink.team.domain.TeamSession;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates the STOMP handshake itself using the same TeamSession cookie
 * as the REST API (or the admin login session), so an anonymous socket can never
 * be opened in the first place.
 */
@Component
public class TeamSessionHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TEAM_ID_ATTRIBUTE = "teamId";
    public static final String ADMIN_ATTRIBUTE = "admin";
    private static final String SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    private final TeamSessionRepository teamSessionRepository;
    private final TeamSessionProperties properties;

    public TeamSessionHandshakeInterceptor(TeamSessionRepository teamSessionRepository, TeamSessionProperties properties) {
        this.teamSessionRepository = teamSessionRepository;
        this.properties = properties;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        Optional<TeamSession> session = extractToken(servletRequest)
                .flatMap(teamSessionRepository::findBySessionToken)
                .filter(s -> s.isValid(Instant.now()));

        if (session.isPresent()) {
            attributes.put(TEAM_ID_ATTRIBUTE, session.get().getTeamId());
            return true;
        }
        if (isAdminSession(servletRequest)) {
            attributes.put(ADMIN_ATTRIBUTE, Boolean.TRUE);
            return true;
        }
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    /** The admin console authenticates with the server-side HTTP session set by the admin login. */
    private boolean isAdminSession(ServletServerHttpRequest servletRequest) {
        HttpSession httpSession = servletRequest.getServletRequest().getSession(false);
        if (httpSession == null
                || !(httpSession.getAttribute(SECURITY_CONTEXT_KEY) instanceof SecurityContext context)
                || context.getAuthentication() == null
                || !context.getAuthentication().isAuthenticated()) {
            return false;
        }
        return context.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Optional<String> extractToken(ServletServerHttpRequest servletRequest) {
        List<HttpCookie> cookies = servletRequest.getServletRequest().getCookies() == null
                ? List.of()
                : java.util.Arrays.stream(servletRequest.getServletRequest().getCookies())
                        .map(c -> new HttpCookie(c.getName(), c.getValue()))
                        .toList();
        return cookies.stream()
                .filter(c -> properties.getCookieName().equals(c.getName()))
                .map(HttpCookie::getValue)
                .findFirst();
    }
}
