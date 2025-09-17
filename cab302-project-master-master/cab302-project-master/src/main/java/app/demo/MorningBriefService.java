package app.demo;

import java.util.List;
import java.util.Random;

public final class MorningBriefService {

    public String build(Market market, EventEngine engine) {
        // Prioritize a live event
        List<EventEngine.ActiveEvent> active = new java.util.ArrayList<>(engine.getActiveEvents());
        if (!active.isEmpty()) {
            EventEngine.ActiveEvent hot = active.stream()
                    .max(java.util.Comparator.comparingDouble(ae -> Math.abs(ae.def.shiftPct)))
                    .get();
            String tone = hot.def.shiftPct >= 0 ? "tailwinds" : "headwinds";
            return String.format("Morning. '%s' is live on %s (%.2f%%). Expect %s—trade with care.",
                    hot.def.title, hot.def.stockCode.toUpperCase(), hot.def.shiftPct, tone);
        }

        // Otherwise nudge to a random/cheap ticker
        List<Stock> pool = market.list();                    // <-- use accessor
        Stock pick = pool.isEmpty() ? null : pool.get(new Random().nextInt(pool.size()));
        if (pick != null) {
            return String.format("Morning. Keep an eye on %s around $%.2f; no live events yet—watch the open.",
                    pick.code.toUpperCase(), pick.price);
        }
        return "Morning. Quiet open—no signals yet.";
    }
}
