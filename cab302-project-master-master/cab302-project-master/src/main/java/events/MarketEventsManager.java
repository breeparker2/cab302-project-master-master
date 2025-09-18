package events;

import app.demo.Stock;
import core.Market;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

public final class MarketEventsManager {

    private final EventBus bus = new EventBus();   // pub/sub

    private final Market market = new Market();
    private final EventEngine engine;
    private Timeline bridgeTimer;

    private final Set<String> liveKeys = new HashSet<>();
    private final List<MarketEvent> todaysEvents = new ArrayList<>();

    private final Random rng = new Random();
    private final List<EventDef> pool = new ArrayList<>();

    public MarketEventsManager() {
        // base stocks
        market.add(new Stock("iron", 12.00));
        market.add(new Stock("oil", 75.00));
        market.add(new Stock("bhp", 24.00));

        Consumer<String> logFn = s -> System.out.println("[EVENT] " + s);
        Consumer<String> toastFn = s -> {};
        Runnable refreshFn = () -> {};

        engine = new EventEngine(market, Collections.emptyList(), new Random(), logFn, toastFn, refreshFn);

        // default event pool
        pool.addAll(List.of(
                new EventDef("Trade Dispute",   "Tariffs escalate; demand weak",   "iron", -8.0, 10_000),
                new EventDef("OPEC Guidance",   "Production guidance due",         "oil",  +5.0, 10_000),
                new EventDef("Earnings Beat",   "Strong results pre-market",       "bhp",  +4.0, 10_000),
                new EventDef("Pipeline Snag",   "Throughput cut weighs on supply", "oil",  -3.0, 10_000),
                new EventDef("China Stimulus",  "Infrastructure push lifting steel","iron", +6.0, 10_000),
                new EventDef("Mine Safety Audit","Temporary halt; output risk",     "bhp",  -4.0, 10_000)
        ));
    }

    // subscribe to events
    public void subscribe(Consumer<MarketEvent> listener) { bus.subscribe(listener); }

    // publisher hook
    public Consumer<MarketEvent> publisher() { return bus::publish; }

    // start engine + pick today’s events
    public void start() {
        engine.start();
        rollToday(3);
        startBridge();
    }

    // move to next day
    public void nextDay() {
        engine.resetDaily();
        rollToday(3);
    }

    // read-only list of today’s events
    public List<MarketEvent> getTodaysEvents() {
        return Collections.unmodifiableList(todaysEvents);
    }

    private void startBridge() {
        if (bridgeTimer != null) bridgeTimer.stop();
        bridgeTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> syncFromEngine()));
        bridgeTimer.setCycleCount(Timeline.INDEFINITE);
        bridgeTimer.play();
    }

    private void rollToday(int n) {
        List<EventDef> all = new ArrayList<>(pool);
        Collections.shuffle(all, rng);
        List<EventDef> todaysDefs = all.subList(0, Math.min(n, all.size()));
        engine.setEventDefs(todaysDefs);

        todaysEvents.clear();
        for (int i = 0; i < Math.min(3, todaysDefs.size()); i++) {
            todaysEvents.add(mapToMarketEvent(todaysDefs.get(i)));
        }
        todaysEvents.forEach(bus::publish);
    }

    private void syncFromEngine() {
        Set<String> now = new HashSet<>();
        for (EventEngine.ActiveEvent ae : engine.getActiveEvents()) {
            String key = ae.def.stockCode + "|" + ae.def.title;
            now.add(key);
            if (!liveKeys.contains(key)) {
                MarketEvent evt = mapToMarketEvent(ae.def);
                bus.publish(evt);
                if (todaysEvents.size() < 5) todaysEvents.add(evt);
            }
        }
        liveKeys.clear();
        liveKeys.addAll(now);
    }

    private static MarketEvent mapToMarketEvent(EventDef def) {
        String title = def.title.toLowerCase();
        String type;
        if (title.contains("pipeline") || title.contains("refinery") ||
                title.contains("throughput") || title.contains("outage")) {
            type = "OIL_SUPPLY";
        } else if (title.contains("opec") || def.stockCode.equalsIgnoreCase("oil")) {
            type = "OPEC";
        } else if (title.contains("rate") || title.contains("hike")) {
            type = "RATE_HIKE";
        } else if (title.contains("earning") || title.contains("beat")) {
            type = "EARNINGS";
        } else {
            type = "NEWS";
        }

        String detail = def.title + " (" + def.stockCode.toUpperCase() + ", " +
                (def.shiftPct >= 0 ? "+" : "") + String.format("%.2f", def.shiftPct) + "%)";
        return new MarketEvent(type, "GLOBAL", detail);
    }
}
