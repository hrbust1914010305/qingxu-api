package com.qingxu.qingxuapi.common.audit;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    REGISTER_SUCCESS,
    REGISTER_FAILURE,
    ACCOUNT_LOCKED,
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    USER_STATUS_CHANGE,
    USER_PASSWORD_CHANGE,
    USER_PASSWORD_RESET
}
