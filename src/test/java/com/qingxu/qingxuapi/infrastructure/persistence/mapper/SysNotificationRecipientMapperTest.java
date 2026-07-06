package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SysNotificationRecipientMapperTest {

    @Test
    void selectNotificationPageOmitsNullFiltersFromSql() throws NoSuchMethodException {
        Method method = SysNotificationRecipientMapper.class.getMethod(
                "selectNotificationPage",
                Page.class,
                Long.class,
                String.class,
                String.class
        );

        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("<script>");
        assertThat(sql).contains("<if test=\"readStatus != null\">");
        assertThat(sql).contains("<if test=\"type != null\">");
        assertThat(sql).doesNotContain("#{readStatus} is null");
        assertThat(sql).doesNotContain("#{type} is null");
    }
}
