package com.qingxu.qingxuapi.infrastructure.captcha;

import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.config.QingxuCaptchaProperties;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final QingxuCaptchaProperties properties;
    private final Map<String, CaptchaValue> captchaStore = new ConcurrentHashMap<>();

    public CaptchaChallenge createCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                properties.getWidth(),
                properties.getHeight(),
                properties.getCodeCount(),
                properties.getInterfereCount()
        );
        String captchaKey = UUID.randomUUID().toString();
        String answer = captcha.getCode();
        int expiresInSeconds = properties.getExpiresInSeconds();
        captchaStore.put(captchaKey, new CaptchaValue(answer, System.currentTimeMillis() + Duration.ofSeconds(expiresInSeconds).toMillis()));
        return new CaptchaChallenge(captchaKey, captcha.getImageBase64Data(), expiresInSeconds, answer);
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
