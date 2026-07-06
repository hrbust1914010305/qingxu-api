package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;
import com.qingxu.qingxuapi.interfaces.notification.dto.NotificationListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysNotificationRecipientMapper extends BaseMapper<SysNotificationRecipientEntity> {

    @Select("""
            select count(1)
            from sys_notification_recipient
            where user_id = #{userId}
              and read_status = 'UNREAD'
              and deleted = false
            """)
    long countUnreadByUserId(Long userId);

    @Select("""
            <script>
            select
                nr.id as id,
                n.id as notificationId,
                n.type as type,
                n.level as level,
                n.title as title,
                n.content as content,
                n.target_url as targetUrl,
                nr.read_status as readStatus,
                nr.read_at as readAt,
                n.created_at as createdAt,
                n.trace_id as traceId
            from sys_notification_recipient nr
            inner join sys_notification n on n.id = nr.notification_id
            where nr.user_id = #{userId}
              and nr.deleted = false
            <if test="readStatus != null">
              and nr.read_status = #{readStatus}
            </if>
            <if test="type != null">
              and n.type = #{type}
            </if>
            order by n.created_at desc, nr.id desc
            </script>
            """)
    List<NotificationListItemResponse> selectNotificationPage(
            Page<NotificationListItemResponse> page,
            @Param("userId") Long userId,
            @Param("readStatus") String readStatus,
            @Param("type") String type
    );

    @Update("""
            update sys_notification_recipient
            set read_status = 'READ',
                read_at = #{readAt}
            where id = #{id}
              and read_status = 'UNREAD'
              and deleted = false
            """)
    int markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    @Update("""
            update sys_notification_recipient
            set read_status = 'READ',
                read_at = #{readAt}
            where user_id = #{userId}
              and read_status = 'UNREAD'
              and deleted = false
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Update("""
            update sys_notification_recipient
            set deleted = true
            where id = #{id}
              and user_id = #{userId}
              and deleted = false
            """)
    int softDelete(@Param("userId") Long userId, @Param("id") Long id);
}
