package com.factorx.service;

import com.factorx.model.ReluFactor;
import com.factorx.model.ReluMomentumPoint;
import com.factorx.model.ReluMetrics;

import java.util.List;

public record ReluResult(
        List<ReluMomentumPoint> momentum,
        ReluMetrics metrics,
        List<ReluFactor> factors,
        double momentumScore
) {}
