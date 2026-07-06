package com.qingxu.qingxuapi.common.permissionchange;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionChangeDispatcherTest {

    @Test
    void fireRoleChangePublishesEveryTimeForSameRole() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AffectedUserResolver affectedUserResolver = mock(AffectedUserResolver.class);

        when(affectedUserResolver.resolveByRole(10L)).thenReturn(Set.of(2L));
        when(affectedUserResolver.isAdminRole(10L)).thenReturn(false);

        PermissionChangeDispatcher dispatcher = new PermissionChangeDispatcher(
                eventPublisher,
                affectedUserResolver
        );

        dispatcher.fireRoleChange(10L, 1L, "admin", "update ui role");
        dispatcher.fireRoleChange(10L, 1L, "admin", "update ui role again");

        verify(eventPublisher, times(2)).publishEvent(any(PermissionChangeEvent.class));
    }
}
