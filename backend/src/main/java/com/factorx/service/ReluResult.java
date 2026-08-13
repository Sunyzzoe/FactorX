package com.factorx.service;

import com.factorx.model.ReluFactor;
import com.factorx.model.ReluMomentumPoint;

import java.util.List;

public record ReluResult(
        List<ReluMomentumPoint> momentum,
        List<ReluFactor> factors,
        double momentumScore
) {}
