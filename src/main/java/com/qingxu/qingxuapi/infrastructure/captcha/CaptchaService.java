package com.qingxu.qingxuapi.infrastructure.captcha;

import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    private static final int EXPIRES_IN_SECONDS = 120;
    private final Map<String, CaptchaValue> captchaStore = new ConcurrentHashMap<>();

    public CaptchaChallenge createCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 15);
        String captchaKey = UUID.randomUUID().toString();
        String answer = captcha.getCode();
        captchaStore.put(captchaKey, new CaptchaValue(answer, System.currentTimeMillis() + Duration.ofSeconds(EXPIRES_IN_SECONDS).toMillis()));
        return new CaptchaChallenge(captchaKey, captcha.getImageBase64Data(), EXPIRES_IN_SECONDS, answer);
    }

    public boolean validateAndConsume(String captchaKey, String captcha) {
        CaptchaValue value = captchaStore.get(captchaKey);
        if (value == null || value.expiresAtMillis() < System.currentTimeMillis()) {
            captchaStore.remove(captchaKey);
            return false;
        }
        boolean matched = value.answer().equalsIgnoreCase(captcha);
        captchaStore.remove(captchaKey);
        return matched;
    }

    public void assertCaptcha(String captchaKey, String captcha) {
        if (!validateAndConsume(captchaKey, captcha)) {
            throw new BusinessException(ErrorCode.AUTH_CAPTCHA_INVALID);
        }
    }

    public void invalidateCaptcha(String captchaKey) {
        if (captchaKey != null) {
            captchaStore.remove(captchaKey);
        }
    }

    private record CaptchaValue(String answer, long expiresAtMillis) {
    }
}