package com.stw.escapelink.submission.service;

import com.stw.escapelink.global.websocket.dto.ImagePendingMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ImageSubmissionWebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public ImageSubmissionWebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageSubmitted(ImageSubmittedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/teams/" + event.teamId(),
                ImagePendingMessage.of(event.quizId(), event.submissionId()));
    }
}
