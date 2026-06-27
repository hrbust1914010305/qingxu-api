package com.qingxu.qingxuapi.interfaces.user.dto;

import java.util.List;

public record SystemSettings(
        Boolean collapsed,
        Boolean isAccordion,
        Boolean isBreadcrumb,
        Boolean isTabs,
        Boolean isFooter,
        String watermark,
        String watermarkColor,
        Integer watermarkFontSize,
        Integer watermarkRotate,
        List<Integer> watermarkGap
) {
}