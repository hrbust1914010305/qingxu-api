package com.qingxu.qingxuapi.infrastructure.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableRedisIndexedHttpSession(
        maxInactiveIntervalInSeconds = 7200,
        redisNamespace = "spring:session:qingxu"
)
public class RedisSessionConfig {

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
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("QINGXU_SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setCookiePath("/");
        serializer.setDomainName(null);
        serializer.setCookieMaxAge(-1);

        boolean isProd = "prod".equalsIgnoreCase(activeProfile);

        if (isProd) {
            serializer.setSameSite("None");
            serializer.setUseSecureCookie(true);
        } else {
            serializer.setSameSite("Lax");
            serializer.setUseSecureCookie(false);
        }

        return serializer;
    }
}
