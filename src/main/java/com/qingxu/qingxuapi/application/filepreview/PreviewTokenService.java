package com.qingxu.qingxuapi.application.filepreview;

import com.qingxu.qingxuapi.common.config.QingxuPreviewProperties;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PreviewTokenService {

    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(8);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final QingxuPreviewProperties previewProperties;
    private final Map<String, FilePreviewToken> tokens = new ConcurrentHashMap<>();

    public FilePreviewToken create(Long fileId) {
        OffsetDateTime expiresAt = OffsetDateTime.now(ZONE_OFFSET).plus(previewProperties.getTokenTtl());
        FilePreviewToken token = new FilePreviewToken(randomToken(), fileId, expiresAt);
        tokens.put(token.token(), token);
        return token;
    }

    public FilePreviewToken requireValid(String tokenValue) {
        FilePreviewToken token = tokens.get(tokenValue);
        if (token == null) {
            throw new BusinessException(ErrorCode.AUTH_401);
        }
        if (token.expiresAt().isBefore(OffsetDateTime.now(ZONE_OFFSET))) {
            tokens.remove(tokenValue);
            throw new BusinessException(ErrorCode.AUTH_401);
        }
        return token;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
