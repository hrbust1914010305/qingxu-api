package com.qingxu.qingxuapi.infrastructure.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.config.QingxuSessionProperties;
import com.qingxu.qingxuapi.infrastructure.security.QingxuUserPrincipal;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSessionConfigTest {

    private final RedisSerializer<Object> serializer =
            new RedisSessionConfig(new QingxuSessionProperties()).springSessionDefaultRedisSerializer(new ObjectMapper().findAndRegisterModules());

    @Test
    void sessionRepositoryCustomizerAppliesConfiguredTimeoutAndNamespace() {
        QingxuSessionProperties properties = new QingxuSessionProperties();
        properties.setTimeout(Duration.ofMinutes(45));
        properties.setRedisNamespace("spring:session:test");
        RedisSessionConfig config = new RedisSessionConfig(properties);
        RedisIndexedSessionRepository repository = mock(RedisIndexedSessionRepository.class);

        config.redisSessionRepositoryCustomizer().customize(repository);

        verify(repository).setDefaultMaxInactiveInterval(Duration.ofMinutes(45));
        verify(repository).setRedisKeyNamespace("spring:session:test");
    }

    @Test
    void sessionSerializerRestoresCurrentUserResponseType() {
        CurrentUserResponse currentUser = new CurrentUserResponse(
                2L,
                "admin2",
                null,
                "admin",
                null,
                "12@12.com",
                "15478451288",
                "default",
                "INTERNAL",
                "ACTIVE",
                false,
                List.of("admin"),
                List.of("system:user:list")
        );

        Object restored = serializer.deserialize(serializer.serialize(currentUser));

        assertThat(restored).isInstanceOf(CurrentUserResponse.class);
        assertThat((CurrentUserResponse) restored).isEqualTo(currentUser);
    }

    @Test
    void sessionSerializerRestoresSecurityContextType() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("system:user:list"));
        QingxuUserPrincipal principal = new QingxuUserPrincipal(
                2L,
                "default",
                "admin2",
                "{noop}password",
                null,
                "admin",
                "12@12.com",
                "15478451288",
                "INTERNAL",
                "ACTIVE",
                0,
                null,
                List.of("admin"),
                List.of("system:user:list"),
                authorities
        );
        SecurityContext securityContext = new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)
        );

        Object restored = serializer.deserialize(serializer.serialize(securityContext));

        assertThat(restored).isInstanceOf(SecurityContext.class);
        SecurityContext restoredContext = (SecurityContext) restored;
        assertThat(restoredContext.getAuthentication()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(restoredContext.getAuthentication().getPrincipal()).isInstanceOf(QingxuUserPrincipal.class);
        assertThat(restoredContext.getAuthentication().getName()).isEqualTo("admin2");
    }
}
