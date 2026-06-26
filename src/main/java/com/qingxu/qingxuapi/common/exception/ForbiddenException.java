package com.qingxu.qingxuapi.common.exception;

import com.qingxu.qingxuapi.common.response.ErrorCode;

public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(ErrorCode.AUTH_403);
    }
}
