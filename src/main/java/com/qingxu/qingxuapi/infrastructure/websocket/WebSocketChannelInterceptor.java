package com.qingxu.qingxuapi.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.Map;

/**
 * 将 WebSocket session 中的 userId 提升为 STOMP 层的 Principal，
 * 让 convertAndSendToUser(userKey, ...) 能正确路由到用户连接。
 */
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            Map<String, Object> attrs = accessor.getSessionAttributes();
            Object userId = attrs != null ? attrs.get("userId") : null;
            if (userId != null) {
                Principal principal = new StompUserPrincipal(userId.toString());
                accessor.setUser(principal);
            } else {
                log.warn("STOMP CONNECT session={} 未找到 userId attribute，消息将无法路由", sessionId);
            }
        }
        return message;
    }

    private record StompUserPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
