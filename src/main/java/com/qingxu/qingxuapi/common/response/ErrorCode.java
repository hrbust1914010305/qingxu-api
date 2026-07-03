package com.qingxu.qingxuapi.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SUCCESS("0", "操作成功", HttpStatus.OK),
    AUTH_401("AUTH_401", "未登录或登录已过期，请重新登录", HttpStatus.UNAUTHORIZED),
    AUTH_403("AUTH_403", "没有权限访问该资源", HttpStatus.FORBIDDEN),
    AUTH_CAPTCHA_INVALID("AUTH_CAPTCHA_INVALID", "验证码错误或已过期，请刷新验证码", HttpStatus.BAD_REQUEST),
    AUTH_BAD_CREDENTIALS("AUTH_BAD_CREDENTIALS", "用户名、密码或验证码错误", HttpStatus.BAD_REQUEST),
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "账号已被锁定，请10分钟后重试或联系管理员", HttpStatus.LOCKED),
    AUTH_ACCOUNT_INACTIVE("AUTH_ACCOUNT_INACTIVE", "账号未激活，请联系管理员审核激活", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_DISABLED("AUTH_ACCOUNT_DISABLED", "账号已被禁用，如需帮助请联系管理员", HttpStatus.FORBIDDEN),
    VALIDATION_ERROR("VALIDATION_ERROR", "参数校验失败", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "请求的接口不存在，请检查URL地址是否正确", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "请求方法不允许，请检查请求方式（GET/POST/PUT/DELETE）是否正确", HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED("MEDIA_TYPE_NOT_SUPPORTED", "不支持的请求格式，请使用JSON格式提交数据", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    REQUEST_BODY_INVALID("REQUEST_BODY_INVALID", "请求数据格式错误，请检查JSON格式是否正确", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("MISSING_PARAMETER", "缺少必填参数，请检查请求参数是否完整", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND),
    OLD_PASSWORD_INCORRECT("OLD_PASSWORD_INCORRECT", "旧密码错误，请重新输入", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_WEAK("PASSWORD_TOO_WEAK", "密码强度不足，必须包含大写字母、小写字母、数字和特殊字符（如!@#$%等）", HttpStatus.BAD_REQUEST),
    DEPT_NOT_FOUND("DEPT_NOT_FOUND", "部门不存在", HttpStatus.NOT_FOUND),
    DEPT_NAME_DUPLICATE("DEPT_NAME_DUPLICATE", "同级部门名称已存在", HttpStatus.BAD_REQUEST),
    DEPT_HAS_CHILDREN("DEPT_HAS_CHILDREN", "存在子部门无法删除", HttpStatus.BAD_REQUEST),
    DEPT_HAS_USERS("DEPT_HAS_USERS", "有关联用户无法直接删除", HttpStatus.BAD_REQUEST),
    DEPT_DELETE_ALL_FAILED("DEPT_DELETE_ALL_FAILED", "批量删除全部失败", HttpStatus.BAD_REQUEST),
    DEPT_ROOT_TEMP_CANNOT_MODIFY("DEPT_ROOT_TEMP_CANNOT_MODIFY", "根目录临时部门不允许修改", HttpStatus.BAD_REQUEST),
    DEPT_ROOT_TEMP_CANNOT_RENAME("DEPT_ROOT_TEMP_CANNOT_RENAME", "根目录临时部门不允许修改名称", HttpStatus.BAD_REQUEST),
    DEPT_ROOT_TEMP_CANNOT_CHANGE_STATUS("DEPT_ROOT_TEMP_CANNOT_CHANGE_STATUS", "根目录临时部门不允许修改状态", HttpStatus.BAD_REQUEST),
    DEPT_ROOT_TEMP_CANNOT_DELETE("DEPT_ROOT_TEMP_CANNOT_DELETE", "根目录临时部门不允许删除", HttpStatus.BAD_REQUEST),
    DEPT_CATEGORY_NOT_FOUND("DEPT_CATEGORY_NOT_FOUND", "部门分类不存在", HttpStatus.NOT_FOUND),
    DEPT_CATEGORY_HAS_DEPTS("DEPT_CATEGORY_HAS_DEPTS", "该分类下有部门无法删除", HttpStatus.BAD_REQUEST),
    DEPT_CATEGORY_CODE_DUPLICATE("DEPT_CATEGORY_CODE_DUPLICATE", "部门分类编码已存在", HttpStatus.BAD_REQUEST),
    PARENT_DEPT_NOT_FOUND("PARENT_DEPT_NOT_FOUND", "上级部门不存在", HttpStatus.NOT_FOUND),
    USERNAME_DUPLICATE("USERNAME_DUPLICATE", "用户名已存在", HttpStatus.BAD_REQUEST),
    PHONE_DUPLICATE("PHONE_DUPLICATE", "手机号已存在", HttpStatus.BAD_REQUEST),
    EMAIL_DUPLICATE("EMAIL_DUPLICATE", "邮箱已存在", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SELF("CANNOT_DELETE_SELF", "不能删除当前登录用户", HttpStatus.BAD_REQUEST),
    CANNOT_DISABLE_SELF("CANNOT_DISABLE_SELF", "不能禁用当前登录用户", HttpStatus.BAD_REQUEST),
    DEPT_NOT_SELECTED("DEPT_NOT_SELECTED", "请至少选择一个部门", HttpStatus.BAD_REQUEST),
    MULTI_TEMP_DEPT("MULTI_TEMP_DEPT", "只能属于一个临时部门", HttpStatus.BAD_REQUEST),
    DEPT_IS_DIRECTORY("DEPT_IS_DIRECTORY", "不能选择目录类型部门", HttpStatus.BAD_REQUEST),
    MENU_NOT_FOUND("MENU_NOT_FOUND", "菜单不存在", HttpStatus.NOT_FOUND),
    MENU_NAME_DUPLICATE("MENU_NAME_DUPLICATE", "菜单名称已存在", HttpStatus.BAD_REQUEST),
    MENU_PARENT_NOT_FOUND("MENU_PARENT_NOT_FOUND", "父菜单不存在", HttpStatus.NOT_FOUND),
    MENU_PARENT_TYPE_ERROR("MENU_PARENT_TYPE_ERROR", "父菜单类型错误", HttpStatus.BAD_REQUEST),
    MENU_HAS_CHILDREN("MENU_HAS_CHILDREN", "存在子菜单不可删除", HttpStatus.BAD_REQUEST),
    MENU_HOME_CANNOT_DELETE("MENU_HOME_CANNOT_DELETE", "Home菜单不可删除", HttpStatus.BAD_REQUEST),
    MENU_HOME_CANNOT_MODIFY_STATUS("MENU_HOME_CANNOT_MODIFY_STATUS", "Home菜单不可修改状态", HttpStatus.BAD_REQUEST),
    MENU_COMPONENT_REQUIRED("MENU_COMPONENT_REQUIRED", "MENU类型必须指定组件路径", HttpStatus.BAD_REQUEST),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}