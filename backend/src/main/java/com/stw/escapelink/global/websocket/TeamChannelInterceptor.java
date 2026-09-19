package com.stw.escapelink.global.websocket;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * A socket authenticated as team A must not be able to subscribe to team B's
 * topic just by knowing its numeric id — the topic teamId is checked against
 * the teamId resolved at handshake time (see TeamSessionHandshakeInterceptor).
 */
@Component
public class TeamChannelInterceptor implements ChannelInterceptor {

    private static final String TEAM_TOPIC_PREFIX = "/topic/teams/";

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Object teamIdAttr = accessor.getSessionAttributes() == null
                    ? null
                    : accessor.getSessionAttributes().get(TeamSessionHandshakeInterceptor.TEAM_ID_ATTRIBUTE);

            if (destination == null || !destination.startsWith(TEAM_TOPIC_PREFIX) || teamIdAttr == null) {
                throw new MessagingException("Subscription rejected");
            }

            String requestedTeamId = destination.substring(TEAM_TOPIC_PREFIX.length());
            if (!requestedTeamId.equals(String.valueOf(teamIdAttr))) {
                throw new MessagingException("Subscription rejected: not your team");
            }
        }

        return message;
    }
}
