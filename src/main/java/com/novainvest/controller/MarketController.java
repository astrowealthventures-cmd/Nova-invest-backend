package com.novainvest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private record TickerSeed(String symbol, double basePrice, int spread, boolean fixed) {}

    private final List<TickerSeed> seeds = List.of(
            new TickerSeed("BTC", 68420, 800, false),
            new TickerSeed("ETH", 3540, 80, false),
            new TickerSeed("SOL", 172, 8, false),
            new TickerSeed("USDT", 1.00, 0, true),
            new TickerSeed("BNB", 612, 15, false),
            new TickerSeed("XRP", 0.58, 0, true),
            new TickerSeed("ADA", 0.44, 0, true),
            new TickerSeed("DOGE", 0.16, 0, true),
            new TickerSeed("AVAX", 34, 2, false),
            new TickerSeed("MATIC", 0.72, 0, true)
    );

    @GetMapping("/ticker")
    public List<Map<String, Object>> ticker() {
        // seeded so the ticker only changes every 30 seconds, same behavior as the FastAPI version
        long seed = System.currentTimeMillis() / 1000 / 30;
        Random random = new Random(seed);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TickerSeed s : seeds) {
            double price = s.fixed() ? s.basePrice() : s.basePrice() + (random.nextInt(2 * s.spread() + 1) - s.spread());
            double change = Math.round((random.nextDouble() * 12 - 6) * 100.0) / 100.0;

            Map<String, Object> t = new LinkedHashMap<>();
            t.put("symbol", s.symbol());
            t.put("price", price);
            t.put("change_24h", change);
            result.add(t);
        }
        return result;
    }
}
