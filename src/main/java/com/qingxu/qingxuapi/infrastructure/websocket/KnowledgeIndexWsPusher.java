package com.qingxu.qingxuapi.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeIndexWsPusher {

    private static final String KNOWLEDGE_INDEX_QUEUE = "/queue/knowledge-index";
    private static final String KNOWLEDGE_INDEX_V2_QUEUE = "/queue/v2-knowledge-index";

    private final SimpMessagingTemplate messagingTemplate;

    public void pushToUser(Long userId, KnowledgeIndexProgressPayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), KNOWLEDGE_INDEX_QUEUE, payload);
        } catch (Exception ex) {
            log.warn("[knowledge] WebSocket push failed, userId={}, jobId={}", userId, payload.jobId(), ex);
        }
    }

    public void pushV2ToUser(Long userId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), KNOWLEDGE_INDEX_V2_QUEUE, payload);
        } catch (Exception ex) {
            log.warn("[knowledge-v2] WebSocket push failed, userId={}, payload={}", userId, payload, ex);
        }
    }
}
