package com.stw.escapelink.admin.service;

import com.stw.escapelink.global.websocket.dto.AdminMonitorMessage;
import com.stw.escapelink.quiz.service.QuizCompletedEvent;
import com.stw.escapelink.submission.service.ImageSubmittedEvent;
import com.stw.escapelink.team.service.TeamControlEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Mirrors every committed game-state change onto the admin-only topic. */
@Component
public class AdminMonitorWebSocketEventListener {

    public static final String ADMIN_TOPIC = "/topic/admin";

    private final SimpMessagingTemplate messagingTemplate;

    public AdminMonitorWebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuizCompleted(QuizCompletedEvent event) {
        send("QUIZ_COMPLETED", event.teamId(), event.quizId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageSubmitted(ImageSubmittedEvent event) {
        send("IMAGE_PENDING", event.teamId(), event.quizId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageReviewed(ImageReviewedEvent event) {
        send(event.approved() ? "IMAGE_APPROVED" : "IMAGE_REJECTED", event.teamId(), event.quizId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTeamControl(TeamControlEvent event) {
        send(event.type(), event.teamId(), event.quizId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDataChanged(AdminDataChangedEvent event) {
        send(event.type(), event.teamId(), event.quizId());
    }

    private void send(String type, Long teamId, Long quizId) {
        messagingTemplate.convertAndSend(ADMIN_TOPIC, new AdminMonitorMessage(type, teamId, quizId));
    }
}
