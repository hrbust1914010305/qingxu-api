package com.qingxu.qingxuapi.interfaces.notification;

import com.qingxu.qingxuapi.application.notification.NotificationApplicationService;
import com.qingxu.qingxuapi.application.notification.NotificationQuery;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.infrastructure.security.QingxuUserPrincipal;
import com.qingxu.qingxuapi.interfaces.notification.dto.NotificationListItemResponse;
import com.qingxu.qingxuapi.interfaces.notification.dto.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService notificationService;
    private final ResponseFactory responseFactory;

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadNotificationCount(
            @AuthenticationPrincipal QingxuUserPrincipal principal
    ) {
        return responseFactory.success(new UnreadCountResponse(
                notificationService.getUnreadCount(principal.getId())
        ));
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationListItemResponse>> getNotificationList(
            @AuthenticationPrincipal QingxuUserPrincipal principal,
            NotificationQuery query
    ) {
        return responseFactory.success(notificationService.pageNotifications(principal.getId(), query));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> putReadNotification(
            @AuthenticationPrincipal QingxuUserPrincipal principal,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(principal.getId(), id);
        return responseFactory.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> putReadAllNotifications(
            @AuthenticationPrincipal QingxuUserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal.getId());
        return responseFactory.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal QingxuUserPrincipal principal,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(principal.getId(), id);
        return responseFactory.success();
    }
}
