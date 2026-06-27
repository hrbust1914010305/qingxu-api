package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SystemSettingsRequest(
        Boolean collapsed,
        Boolean isAccordion,
        Boolean isBreadcrumb,
        Boolean isTabs,
        Boolean isFooter,
        String watermark,
        @Size(max = 32, message = "水印颜色长度不能超过32个字符")
        String watermarkColor,
        @Min(value = 8, message = "水印字号不能小于8")
        @Max(value = 72, message = "水印字号不能大于72")
        Integer watermarkFontSize,
        @Min(value = -360, message = "水印旋转角度不能小于-360度")
        @Max(value = 360, message = "水印旋转角度不能大于360度")
        Integer watermarkRotate,
        List<Integer> watermarkGap
) {
}