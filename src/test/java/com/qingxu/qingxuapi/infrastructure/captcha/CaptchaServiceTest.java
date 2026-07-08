package com.qingxu.qingxuapi.infrastructure.captcha;

import com.qingxu.qingxuapi.common.config.QingxuCaptchaProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaServiceTest {

    @Test
    void captchaCanBeCreatedAndValidatedOnce() {
        CaptchaService captchaService = new CaptchaService(new QingxuCaptchaProperties());

        CaptchaChallenge challenge = captchaService.createCaptcha();

        assertThat(challenge.captchaKey()).isNotBlank();
        assertThat(challenge.imageBase64()).startsWith("data:image/png;base64,");
        assertThat(challenge.expiresIn()).isEqualTo(120);
        assertThat(challenge.answer()).isNotBlank();
        assertThat(captchaService.validateAndConsume(challenge.captchaKey(), challenge.answer())).isTrue();
        assertThat(captchaService.validateAndConsume(challenge.captchaKey(), challenge.answer())).isFalse();
    }
}
