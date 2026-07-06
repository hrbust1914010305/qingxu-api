package com.qingxu.qingxuapi.interfaces.notification;

import com.qingxu.qingxuapi.application.notification.NotificationApplicationService;
import com.qingxu.qingxuapi.application.notification.NotificationQuery;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.infrastructure.security.QingxuUserPrincipal;
import com.qingxu.qingxuapi.interfaces.notification.dto.NotificationListItemResponse;
import com.qingxu.qingxuapi.interfaces.notification.dto.UnreadCountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @Test
    void unreadCountAndListReturnCurrentUserNotifications() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        NotificationController controller = new NotificationController(notificationService, new ResponseFactory());
        NotificationListItemResponse item = new NotificationListItemResponse();
        item.setId(1001L);
        item.setNotificationId(501L);
        item.setType("SYSTEM");
        item.setLevel("INFO");
        item.setTitle("系统维护");
        item.setContent("今晚 23:00 进行系统维护。");
        item.setReadStatus("UNREAD");
        item.setCreatedAt(LocalDateTime.parse("2026-07-06T10:20:30"));
        when(notificationService.getUnreadCount(2L)).thenReturn(1L);
        when(notificationService.pageNotifications(eq(2L), any(NotificationQuery.class)))
                .thenReturn(new PageResponse<>(List.of(item), 1, 1, 10));

        ApiResponse<UnreadCountResponse> count = controller.getUnreadNotificationCount(principal(2L));
        ApiResponse<PageResponse<NotificationListItemResponse>> list = controller.getNotificationList(
                principal(2L),
                new NotificationQuery(1, 10, null, null)
        );

        assertThat(count.data().count()).isEqualTo(1L);
        assertThat(list.data().records()).hasSize(1);
        assertThat(list.data().records().getFirst().getTitle()).isEqualTo("系统维护");
    }

    @Test
    void writeOperationsPassCurrentUserIdToService() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        NotificationController controller = new NotificationController(notificationService, new ResponseFactory());
        QingxuUserPrincipal principal = principal(2L);

        controller.putReadNotification(principal, 1001L);
        controller.putReadAllNotifications(principal);
        controller.deleteNotification(principal, 1001L);

        verify(notificationService).markAsRead(2L, 1001L);
        verify(notificationService).markAllAsRead(2L);
        verify(notificationService).deleteNotification(2L, 1001L);
    }

    private QingxuUserPrincipal principal(Long id) {
        return new QingxuUserPrincipal(
                id,
                "default",
                "user" + id,
                "password",
                "User " + id,
                "user" + id,
                null,
                null,
                "USER",
                "ACTIVE",
                0,
                null,
                List.of("USER"),
                List.of(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
