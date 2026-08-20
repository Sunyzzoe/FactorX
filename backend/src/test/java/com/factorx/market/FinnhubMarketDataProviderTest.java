package com.factorx.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factorx.market.model.StockPrice;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinnhubMarketDataProviderTest {
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/stock/candle", exchange -> respond(exchange,
                "{\"s\":\"ok\",\"c\":[100.0,103.0],\"v\":[1000,2500],\"t\":[1721001600,1721088000]}"));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsDailyCandlesToUnifiedPrices() {
        FinnhubMarketDataProvider provider = new FinnhubMarketDataProvider(
                new ObjectMapper(), "http://localhost:" + server.getAddress().getPort(), "test-key", 3000);

        List<StockPrice> prices = provider.getHistory("TSLA", LocalDate.of(2024, 7, 15), LocalDate.of(2024, 7, 16));

        assertEquals(2, prices.size());
        assertEquals("TSLA", prices.get(0).symbol());
        assertEquals(100.0, prices.get(0).closePrice().doubleValue());
        assertEquals(2500L, prices.get(1).volume());
        assertTrue(prices.get(1).tradeDate().isAfter(prices.get(0).tradeDate()));
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
