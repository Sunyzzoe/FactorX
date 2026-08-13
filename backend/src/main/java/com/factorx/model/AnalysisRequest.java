package com.factorx.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
        @NotBlank(message = "headline 不能为空")
        @Size(max = 300, message = "headline 不能超过 300 个字符")
        String headline,
        @Size(max = 80, message = "source 不能超过 80 个字符")
        String source,
        @Size(max = 10000, message = "body 不能超过 10000 个字符")
        String body
) {}
