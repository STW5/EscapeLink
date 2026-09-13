package com.stw.escapelink.global.websocket;

import com.stw.escapelink.global.config.TeamSessionProperties;
import com.stw.escapelink.team.domain.TeamSession;
import com.stw.escapelink.team.repository.TeamSessionRepository;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates the STOMP handshake itself using the same TeamSession cookie
 * as the REST API, so an anonymous socket can never be opened in the first place.
 */
@Component
public class TeamSessionHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TEAM_ID_ATTRIBUTE = "teamId";

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

        if (session.isEmpty()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(TEAM_ID_ATTRIBUTE, session.get().getTeamId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
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
