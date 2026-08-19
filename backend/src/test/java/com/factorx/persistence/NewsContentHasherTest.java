package com.factorx.persistence;

import com.factorx.model.AnalysisRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NewsContentHasherTest {

    private final NewsContentHasher hasher = new NewsContentHasher();

    @Test
    void normalizesWhitespaceBeforeHashing() {
        String first = hasher.hash(new AnalysisRequest(
                "Tesla update",
                " Reuters ",
                "Tesla   wins\n a contract"
        ));
        String second = hasher.hash(new AnalysisRequest(
                " Tesla update ",
                "Reuters",
                "Tesla wins a contract"
        ));

        assertEquals(first, second);
    }

    @Test
    void changesHashWhenNewsContentChanges() {
        String first = hasher.hash(new AnalysisRequest("Tesla update", "Reuters", "Contract A"));
        String second = hasher.hash(new AnalysisRequest("Tesla update", "Reuters", "Contract B"));

        assertNotEquals(first, second);
    }
}
