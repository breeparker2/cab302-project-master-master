package core;

import app.demo.Stock;

import java.util.*;

// mainly for the panel, this will change with javafx
public final class Market {
    private final List<Stock> list = new ArrayList<>();
    private final Map<String, Stock> byCode = new HashMap<>();

    public void add(Stock s) { list.add(s); byCode.put(s.code, s); }
    public List<Stock> list() { return list; }
    public Stock get(String code) { return byCode.get(code); }

    public void tickAll(Random rng) {
        for (Stock s : list) s.tick(rng);
    }
}