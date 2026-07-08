package com.qingxu.qingxuapi.infrastructure.websocket;

import com.qingxu.qingxuapi.common.config.QingxuSecurityProperties;
import com.qingxu.qingxuapi.common.config.QingxuWebSocketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final QingxuSecurityProperties securityProperties;
    private final QingxuWebSocketProperties webSocketProperties;

    @Bean
    public ThreadPoolTaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(webSocketProperties.getHeartbeatPoolSize());
        taskScheduler.setThreadNamePrefix("websocket-heartbeat-");
        return taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler taskScheduler = heartBeatScheduler();
        config.enableSimpleBroker(webSocketProperties.getBrokerPrefixes().toArray(String[]::new))
              .setHeartbeatValue(webSocketProperties.getHeartbeat())
              .setTaskScheduler(taskScheduler);
        config.setApplicationDestinationPrefixes(webSocketProperties.getApplicationDestinationPrefix());
        config.setUserDestinationPrefix(webSocketProperties.getUserDestinationPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(webSocketProperties.getEndpoint())
                .setAllowedOriginPatterns(resolveAllowedOriginPatterns())
                .addInterceptors(new WebSocketAuthInterceptor())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketChannelInterceptor());
    }

    private String[] resolveAllowedOriginPatterns() {
        if (!webSocketProperties.getAllowedOriginPatterns().isEmpty()) {
            return webSocketProperties.getAllowedOriginPatterns().toArray(String[]::new);
        }
        return securityProperties.getCors().getAllowedOrigins().toArray(String[]::new);
    }
}
