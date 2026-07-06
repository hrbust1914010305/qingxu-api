package com.qingxu.qingxuapi.infrastructure.session;

import com.qingxu.qingxuapi.infrastructure.security.QingxuUserPrincipal;
import com.qingxu.qingxuapi.infrastructure.websocket.WebSocketAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketAuthInterceptorTest {

    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor();

    @Test
    void beforeHandshakeAcceptsAuthenticatedSessionAndExposesUserId() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication())
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                null,
                null,
                attributes
        );

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry("userId", "2");
    }

    private Authentication authentication() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("system:user:list"));
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
                new ArrayList<>(authorities)
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
