package com.qingxu.qingxuapi.application.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserPreferenceEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserPreferenceMapper;
import com.qingxu.qingxuapi.interfaces.user.dto.ChangePasswordRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.SavePreferenceRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.SystemSettings;
import com.qingxu.qingxuapi.interfaces.user.dto.SystemSettingsRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.ThemeConfig;
import com.qingxu.qingxuapi.interfaces.user.dto.ThemeConfigRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.UpdateProfileRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.UserPreferenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final SysUserPreferenceMapper preferenceMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserPreferenceResponse getUserPreference(Long userId) {
        SysUserPreferenceEntity preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            return getDefaultPreference(userId);
        }
        return convertToResponse(preference);
    }

    public void saveUserPreference(Long userId, SavePreferenceRequest request) {
        SysUserPreferenceEntity preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            preference = new SysUserPreferenceEntity();
            preference.setUserId(userId);
            copySystemSettings(request.systemSettings(), preference);
            copyThemeConfig(request.themeConfig(), preference);
            preference.setCreatedAt(LocalDateTime.now());
            preference.setUpdatedAt(LocalDateTime.now());
            preferenceMapper.insert(preference);
        } else {
            copySystemSettings(request.systemSettings(), preference);
            copyThemeConfig(request.themeConfig(), preference);
            preference.setUpdatedAt(LocalDateTime.now());
            preferenceMapper.updateById(preference);
        }
    }

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setNickname(request.nickname());
        if (request.realname() != null) {
            user.setRealname(request.realname());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        } else {
            user.setEmail(null);
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        } else {
            user.setPhone(null);
        }
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "两次输入的新密码不一致");
        }

        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_INCORRECT, "旧密码错误");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(encodedPassword);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private UserPreferenceResponse getDefaultPreference(Long userId) {
        return new UserPreferenceResponse(
                userId,
                getDefaultSystemSettings(),
                getDefaultThemeConfig()
        );
    }

    private SystemSettings getDefaultSystemSettings() {
        return new SystemSettings(
                false,
                true,
                true,
                true,
                true,
                "",
                "rgba(0, 0, 0, 0.15)",
                14,
                -20,
                List.of(100, 100)
        );
    }

    private ThemeConfig getDefaultThemeConfig() {
        return new ThemeConfig(
                "layoutDefaults",
                "#165DFF",
                false,
                false,
                false,
                false,
                "fadeInOut"
        );
    }

    private UserPreferenceResponse convertToResponse(SysUserPreferenceEntity entity) {
        return new UserPreferenceResponse(
                entity.getUserId(),
                convertToSystemSettings(entity),
                convertToThemeConfig(entity)
        );
    }

    private SystemSettings convertToSystemSettings(SysUserPreferenceEntity entity) {
        List<Integer> watermarkGapList = parseWatermarkGap(entity.getWatermarkGap());
        
        return new SystemSettings(
                entity.getCollapsed(),
                entity.getIsAccordion(),
                entity.getIsBreadcrumb(),
                entity.getIsTabs(),
                entity.getIsFooter(),
                entity.getWatermark(),
                entity.getWatermarkColor(),
                entity.getWatermarkFontSize(),
                entity.getWatermarkRotate(),
                watermarkGapList
        );
    }

    private ThemeConfig convertToThemeConfig(SysUserPreferenceEntity entity) {
        return new ThemeConfig(
                entity.getLayoutType(),
                entity.getThemeColor(),
                entity.getColorWeakMode(),
                entity.getGrayMode(),
                entity.getAsideDark(),
                entity.getDarkMode(),
                entity.getTransitionPage()
        );
    }

    private void copySystemSettings(SystemSettingsRequest request, SysUserPreferenceEntity entity) {
        if (request.collapsed() != null) {
            entity.setCollapsed(request.collapsed());
        }
        if (request.isAccordion() != null) {
            entity.setIsAccordion(request.isAccordion());
        }
        if (request.isBreadcrumb() != null) {
            entity.setIsBreadcrumb(request.isBreadcrumb());
        }
        if (request.isTabs() != null) {
            entity.setIsTabs(request.isTabs());
        }
        if (request.isFooter() != null) {
            entity.setIsFooter(request.isFooter());
        }
        if (request.watermark() != null) {
            entity.setWatermark(request.watermark());
        }
        if (request.watermarkColor() != null) {
            entity.setWatermarkColor(request.watermarkColor());
        }
        if (request.watermarkFontSize() != null) {
            entity.setWatermarkFontSize(request.watermarkFontSize());
        }
        if (request.watermarkRotate() != null) {
            entity.setWatermarkRotate(request.watermarkRotate());
        }
        if (request.watermarkGap() != null) {
            try {
                entity.setWatermarkGap(objectMapper.writeValueAsString(request.watermarkGap()));
            } catch (Exception e) {
                entity.setWatermarkGap("[100, 100]");
            }
        }
    }

    private void copyThemeConfig(ThemeConfigRequest request, SysUserPreferenceEntity entity) {
        if (request.layoutType() != null) {
            entity.setLayoutType(request.layoutType());
        }
        if (request.themeColor() != null) {
            entity.setThemeColor(request.themeColor());
        }
        if (request.colorWeakMode() != null) {
            entity.setColorWeakMode(request.colorWeakMode());
        }
        if (request.grayMode() != null) {
            entity.setGrayMode(request.grayMode());
        }
        if (request.asideDark() != null) {
            entity.setAsideDark(request.asideDark());
        }
        if (request.darkMode() != null) {
            entity.setDarkMode(request.darkMode());
        }
        if (request.transitionPage() != null) {
            entity.setTransitionPage(request.transitionPage());
        }
    }

    private List<Integer> parseWatermarkGap(String watermarkGapStr) {
        if (watermarkGapStr == null || watermarkGapStr.isBlank()) {
            return List.of(100, 100);
        }
        try {
            return objectMapper.readValue(watermarkGapStr, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return List.of(100, 100);
        }
    }
}