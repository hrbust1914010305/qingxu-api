package com.qingxu.qingxuapi.interfaces.auth.dto;

public record CaptchaResponse(
        String captchaKey,
        String imageBase64,
        int expiresIn
) {
}
