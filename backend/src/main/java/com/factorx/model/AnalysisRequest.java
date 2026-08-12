package com.factorx.model;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank String headline,
        String source,
        String body
) {}
