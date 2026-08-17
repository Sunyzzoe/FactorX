package com.factorx.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReluFactorServiceTest {

    private final ReluFactorService service = new ReluFactorService();

    @Test
    void calculatesDocumentedReluMetrics() {
        List<BigDecimal> prices = List.of(
                new BigDecimal("100"),
                new BigDecimal("101.0050167"),
                new BigDecimal("100.2002001"),
                new BigDecimal("100.5010042"),
                new BigDecimal("101.9174147"),
                new BigDecimal("102.6332737")
        );

        ReluResult result = service.calculate(prices, new BigDecimal("0.004"), 5);

        assertEquals(6, result.momentum().size());
        assertEquals(1.9, result.momentum().get(5).reluMomentum(), 0.01);
        assertEquals(0.0038, result.metrics().reluSlope(), 0.0001);
        assertEquals(0.6, result.metrics().positiveDensity(), 0.0001);
        assertEquals(0.4, result.metrics().plateauRatio(), 0.0001);
        assertEquals(0.001368, result.metrics().momentumPurity(), 0.0001);
    }

    @Test
    void truncatesNonQualifyingReturnsToZero() {
        ReluResult result = service.calculate(
                List.of(new BigDecimal("100"), new BigDecimal("100.2"), new BigDecimal("99")),
                new BigDecimal("0.004"),
                2
        );

        assertEquals(0, result.momentum().get(1).reluReturn());
        assertEquals(0, result.momentum().get(2).reluReturn());
        assertEquals(0, result.momentum().get(2).reluMomentum());
        assertEquals(1, result.metrics().plateauRatio(), 0.0001);
    }

    @Test
    void rejectsInvalidPriceSeries() {
        assertThrows(IllegalArgumentException.class, () -> service.calculate(
                List.of(new BigDecimal("100")),
                new BigDecimal("0.004"),
                1
        ));
    }
}
