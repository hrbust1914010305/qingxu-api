package com.qingxu.qingxuapi.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

import java.security.Principal;
import java.util.Map;

/**
 * 将 WebSocket session 中 {@code userId} 属性提升为 STOMP 层的 {@link Principal}，
 * 供 {@code convertAndSendToUser(userKey, ...)} 正确路由消息。
 * <p>
 * 流程：
 * 1. 握手时 {@link WebSocketAuthInterceptor} 将 userId 存入 WebSocket session attributes
 * 2. STOMP CONNECT 到达后，inbound channel 经过此拦截器
 * 3. 从 WebSocket session 取出 userId，构建 Principal 并设入 STOMP accessor
 * 4. 之后 /user/queue/... 的路由即可通过 Principal 找到对应连接
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
                log.debug("STOMP CONNECT session={} userId={}", sessionId, userId);
            } else {
                log.warn("STOMP CONNECT session={} 但未找到 userId attribute，消息将无法路由", sessionId);
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