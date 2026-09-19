package com.stw.escapelink.quiz.service;

import com.stw.escapelink.global.websocket.dto.QuizCompletedMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires strictly after the completing transaction commits (AFTER_COMMIT) —
 * WebSocket never announces a state change that didn't actually persist.
 */
@Component
public class QuizWebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public QuizWebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuizCompleted(QuizCompletedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/teams/" + event.teamId(),
                QuizCompletedMessage.of(event.quizId(), event.stateVersion()));
    }
}
