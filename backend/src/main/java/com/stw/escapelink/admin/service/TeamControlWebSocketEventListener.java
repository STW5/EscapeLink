package com.stw.escapelink.admin.service;

import com.stw.escapelink.global.websocket.dto.TeamControlMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TeamControlWebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public TeamControlWebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTeamControl(TeamControlEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/teams/" + event.teamId(),
                TeamControlMessage.of(event.type(), event.quizId()));
    }
}
