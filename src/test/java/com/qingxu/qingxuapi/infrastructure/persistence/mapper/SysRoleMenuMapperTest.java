package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SysRoleMenuMapperTest {

    @Test
    void selectMenuIdsByRoleIdReturnsOnlyExistingMenus() throws NoSuchMethodException {
        Method method = SysRoleMenuMapper.class.getMethod("selectMenuIdsByRoleId", Long.class);

        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(sql).containsIgnoringCase("join sys_menu");
        assertThat(sql).containsIgnoringCase("m.id = rm.menu_id");
    }
}
