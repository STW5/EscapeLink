package com.stw.escapelink.global.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamChannelInterceptorTest {

    private final TeamChannelInterceptor interceptor = new TeamChannelInterceptor();

    @Test
    void adminSocketMaySubscribeToAdminTopicOnly() {
        Map<String, Object> attrs = Map.of(TeamSessionHandshakeInterceptor.ADMIN_ATTRIBUTE, true);

        assertThat(interceptor.preSend(subscribe("/topic/admin", attrs), null)).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/teams/1", attrs), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void teamSocketCannotSubscribeToAdminTopic() {
        Map<String, Object> attrs = Map.of(TeamSessionHandshakeInterceptor.TEAM_ID_ATTRIBUTE, 1L);

        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/admin", attrs), null))
                .isInstanceOf(MessagingException.class);
        assertThat(interceptor.preSend(subscribe("/topic/teams/1", attrs), null)).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/teams/2", attrs), null))
                .isInstanceOf(MessagingException.class);
    }

    private Message<?> subscribe(String destination, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(new HashMap<>(sessionAttributes));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
