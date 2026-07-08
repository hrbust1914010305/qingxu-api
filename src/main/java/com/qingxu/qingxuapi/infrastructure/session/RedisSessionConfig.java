package com.qingxu.qingxuapi.infrastructure.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qingxu.qingxuapi.common.config.QingxuSessionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableRedisIndexedHttpSession
@RequiredArgsConstructor
public class RedisSessionConfig {

    private final QingxuSessionProperties sessionProperties;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer(ObjectMapper objectMapper) {
        ObjectMapper sessionObjectMapper = objectMapper.copy();
        sessionObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        sessionObjectMapper.registerModules(SecurityJackson2Modules.getModules(RedisSessionConfig.class.getClassLoader()));

        return GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(sessionObjectMapper)
                .defaultTyping(true)
                .build();
    }

    @Bean
    public SessionRepositoryCustomizer<RedisIndexedSessionRepository> redisSessionRepositoryCustomizer() {
        return repository -> {
            repository.setDefaultMaxInactiveInterval(sessionProperties.getTimeout());
            repository.setRedisKeyNamespace(sessionProperties.getRedisNamespace());
        };
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        QingxuSessionProperties.Cookie cookie = sessionProperties.getCookie();
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookie.getName());
        serializer.setUseHttpOnlyCookie(cookie.isHttpOnly());
        serializer.setCookiePath(cookie.getPath());
        serializer.setDomainName(cookie.getDomain());
        serializer.setCookieMaxAge(cookie.getMaxAge());

        boolean isProd = "prod".equalsIgnoreCase(activeProfile);

        if (isProd) {
            serializer.setSameSite(cookie.getSameSite().getProd());
            serializer.setUseSecureCookie(cookie.getSecure().isProd());
        } else {
            serializer.setSameSite(cookie.getSameSite().getDev());
            serializer.setUseSecureCookie(cookie.getSecure().isDev());
        }

        return serializer;
    }
}
