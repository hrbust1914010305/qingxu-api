package com.qingxu.qingxuapi.common.exception;

import com.qingxu.qingxuapi.common.response.ErrorCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.AUTH_401);
    }
}
