package app.demo;

import java.util.Random;

// stock logic all stocks have the same logic for now in terms of increasing %
public final class Stock {
    public final String code;
    public double price;
    public double lastPrice;

    // Base value for random increase
    public final double baseMinPct = -0.1;
    public final double baseMaxPct = +0.3;

    // Current shift - its 0 at the java.demo.start cause no events, this changes when a new event appears
    private double shiftPct = 0.0;

    public Stock(String code, double initialPrice) {
        this.code = code;
        this.price = initialPrice;
        this.lastPrice = initialPrice;
    }

    // every tick pick a random number between the min adn max pct (base is -.01 to 0.3) events effect this number
    public void tick(Random rng) {
        double minPct = baseMinPct + shiftPct;
        double maxPct = baseMaxPct + shiftPct;
        double rPct = minPct + rng.nextDouble() * (maxPct - minPct);
        double r = rPct / 100.0; // convert percentage points to decimal

        lastPrice = price;
        price = price * (1.0 + r);
        if (price < 0.01) price = 0.01; // guardrail
    }

    // apply and shifts in the market
    public void addShift(double deltaPct) {
        shiftPct += deltaPct;
    }
}