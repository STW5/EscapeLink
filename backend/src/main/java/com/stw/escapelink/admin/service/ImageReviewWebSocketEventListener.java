package com.stw.escapelink.admin.service;

import com.stw.escapelink.global.websocket.dto.ImageReviewedMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ImageReviewWebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public ImageReviewWebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageReviewed(ImageReviewedEvent event) {
        ImageReviewedMessage message = event.approved()
                ? ImageReviewedMessage.approved(event.quizId())
                : ImageReviewedMessage.rejected(event.quizId(), event.rejectReason());

        messagingTemplate.convertAndSend("/topic/teams/" + event.teamId(), message);
    }
}
