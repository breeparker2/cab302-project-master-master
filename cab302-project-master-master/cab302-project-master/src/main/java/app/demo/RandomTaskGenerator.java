package app.demo;

import java.util.*;

/** Creates simple ✔/• java.demo.Task lines for your tablet. */
public final class RandomTaskGenerator {
    private final Random rng = new Random();

    /** Build a fresh set of tasks for the new day. */
    public List<Task> generate(Market market, EventEngine engine) {
        List<Task> out = new ArrayList<>();

        out.add(new Task("Read the market bulletin on the corkboard"));

        List<EventEngine.ActiveEvent> active = new ArrayList<>(engine.getActiveEvents());
        if (!active.isEmpty()) {
            EventEngine.ActiveEvent hot = active.stream()
                    .max(Comparator.comparingDouble(ae -> Math.abs(ae.def.shiftPct)))
                    .get();
            String dir = hot.def.shiftPct >= 0 ? "accumulate" : "hedge/explore short";
            out.add(new Task(String.format("%s exposure in %s while '%s' is live",
                    capitalize(dir), hot.def.stockCode.toUpperCase(), hot.def.title)));
        }

        // Use accessor instead of private field
        List<Stock> pool = new ArrayList<>(market.list());
        if (pool.size() >= 2) {
            Collections.shuffle(pool, rng);
            Stock a = pool.get(0);
            Stock b = pool.get(1);

            out.add(makeThresholdTask(a, "Buy", -0.01, -0.03));
            out.add(makeThresholdTask(b, "Take profit", +0.02, +0.05));
        }

        out.add(new Task("Close at least 1 losing position before noon"));
        while (out.size() > 5) out.remove(out.size() - 1);
        return out;
    }

    private Task makeThresholdTask(Stock s, String verb, double minPct, double maxPct) {
        double pct = lerp(minPct, maxPct, rng.nextDouble());
        double target = s.price * (1.0 + pct);
        String arrow = pct >= 0 ? "↑" : "↓";
        return new Task(String.format("%s %s near %s (%.2f%% %s from now)",
                verb, s.code.toUpperCase(), money(target), Math.abs(pct * 100), arrow));
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static String money(double v) { return String.format("$%,.2f", v); }
    private static String capitalize(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}
