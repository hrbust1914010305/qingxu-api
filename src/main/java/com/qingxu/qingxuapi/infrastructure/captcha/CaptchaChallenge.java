package com.qingxu.qingxuapi.infrastructure.captcha;

public record CaptchaChallenge(
        String captchaKey,
        String imageBase64,
        int expiresIn,
        String answer
) {
}
