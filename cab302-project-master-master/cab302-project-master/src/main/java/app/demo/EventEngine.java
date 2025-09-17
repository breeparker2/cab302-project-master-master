package app.demo;

import java.util.*;
import java.util.function.Consumer;

/** Drives market events with spam guards and cooldowns. */
public final class EventEngine {
    // ===== knobs you can tweak =====
    public static final int TICK_MS = 6_000;    // engine checks every 6s
    public static final int CHANCE_PCT = 20;       // % chance per tick
    public static final int MAX_EVENTS_PER_DAY = 4;        // hard daily cap
    public static final long MIN_GAP_BETWEEN_TRIGGERS_MS = 90_000;   // ≥1.5 min between ANY two events
    public static final long PER_STOCK_COOLDOWN_MS = 180_000;  // ≥3 min between same stock
    public static final int PER_EVENT_MAX_PER_DAY = 1;        // each (title|stock) at most once/day

    private final Market market;
    private final List<EventDef> eventDefs;            // mutable list is OK
    private final Random rng;
    private final Consumer<String> logFn;
    private final Consumer<String> toastFn;
    private final Runnable refreshFn;

    // use Swing timer explicitly to avoid clash with java.util.Timer
    private final javax.swing.Timer tickTimer;

    // what’s currently active (visible to java.demo.hub bridge)
    public static final class ActiveEvent {
        public final EventDef def;
        public final long endsAtMillis;
        public boolean effectReverted = false;  // ✅ new: mark when stock impact has ended

        public ActiveEvent(EventDef def, long endsAtMillis) {
            this.def = def;
            this.endsAtMillis = endsAtMillis;
        }
    }

    private final List<ActiveEvent> activeEvents = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Set<String> activeKeys = new HashSet<>(); // "stock|title" of active events

    // anti-spam state
    private final Map<String, Long> lastStockTriggerAt = new HashMap<>(); // stock -> millis
    private final Map<String, Integer> perEventCountToday = new HashMap<>(); // "stock|title" -> count today
    private long lastAnyTriggerAt = 0L;
    private int eventsToday = 0;

    public List<ActiveEvent> getActiveEvents() {
        return java.util.Collections.unmodifiableList(activeEvents);
    }

    /**
     * Replace the event set at runtime (optional).
     */
    public void setEventDefs(List<EventDef> defs) {
        this.eventDefs.clear();
        this.eventDefs.addAll(defs);
    }

    public EventEngine(Market market,
                       List<EventDef> eventDefs,
                       Random rng,
                       Consumer<String> logFn,
                       Consumer<String> toastFn,
                       Runnable refreshFn) {
        this.market = market;
        this.eventDefs = new ArrayList<>(eventDefs);
        this.rng = rng;
        this.logFn = logFn;
        this.toastFn = toastFn;
        this.refreshFn = refreshFn;

        // Swing timer: (delay, ActionListener)
        this.tickTimer = new javax.swing.Timer(TICK_MS, e -> tryTrigger());
    }

    public void start() {
        tickTimer.start();
    }

    public void stop() {
        tickTimer.stop();
    }

    /**
     * Call this at the java.demo.start of each new in-game day (your EOD handler already does this).
     */
    /** Call this at the java.demo.start of each new in-game day (EOD rollover). */
    public void resetDaily() {
        // revert all still-active events once, at day change
        for (ActiveEvent ae : new ArrayList<>(activeEvents)) {
            Stock s = market.get(ae.def.stockCode);
            if (s != null) s.addShift(-ae.def.shiftPct);
            logFn.accept("Event ended (EOD): " + ae.def.title + " — " + ae.def.stockCode.toUpperCase());
        }
        activeEvents.clear();
        activeKeys.clear();

        // reset anti-spam / counters for the new day
        eventsToday = 0;
        lastAnyTriggerAt = 0L;
        lastStockTriggerAt.clear();
        perEventCountToday.clear();

        refreshFn.run();
    }


    /* ================= core ================= */

    private void tryTrigger() {
        if (eventDefs.isEmpty()) return;

        // daily cap
        if (eventsToday >= MAX_EVENTS_PER_DAY) return;

        // debounce between any two triggers
        long now = System.currentTimeMillis();
        if (now - lastAnyTriggerAt < MIN_GAP_BETWEEN_TRIGGERS_MS) return;

        // probability gate
        if (rng.nextInt(100) >= CHANCE_PCT) return;

        // pick a candidate that isn’t active and respects per-stock cooldown + per-event/day cap
        List<EventDef> pool = new ArrayList<>(eventDefs);
        Collections.shuffle(pool, rng);

        EventDef chosen = null;
        for (EventDef ev : pool) {
            String stock = ev.stockCode.toLowerCase();
            String key = stock + "|" + ev.title;

            if (activeKeys.contains(key)) continue; // already active

            // per-event daily cap
            int used = perEventCountToday.getOrDefault(key, 0);
            if (used >= PER_EVENT_MAX_PER_DAY) continue;

            // per-stock cooldown
            long lastForStock = lastStockTriggerAt.getOrDefault(stock, 0L);
            if (now - lastForStock < PER_STOCK_COOLDOWN_MS) continue;

            // stock must exist
            if (market.get(ev.stockCode) == null) continue;

            chosen = ev;
            break;
        }

        if (chosen != null) trigger(chosen);
    }

    private void trigger(EventDef ev) {
        Stock s = market.get(ev.stockCode);
        if (s == null) {
            logFn.accept("Skipped event " + ev + " (unknown stock)");
            return;
        }

        // apply effect and keep it until EOD (ignore durationMs here)
        s.addShift(ev.shiftPct);

        // keep an endsAtMillis if you like, but it’s not used to auto-revert
        long endsAt = Long.MAX_VALUE; // marker: persists until EOD
        ActiveEvent active = new ActiveEvent(ev, endsAt);

        String stock = ev.stockCode.toLowerCase();
        String key = stock + "|" + ev.title;

        activeEvents.add(active);
        activeKeys.add(key);

        // book-keeping
        eventsToday++;
        long now = System.currentTimeMillis();
        lastAnyTriggerAt = now;
        lastStockTriggerAt.put(stock, now);
        perEventCountToday.merge(key, 1, Integer::sum);

        // log explicitly that it persists until end of day
        logFn.accept("Event: " + ev.title + " — " + ev.description + " ["
                + ev.stockCode.toUpperCase() + " shift " + ev.shiftPct + "pp; persists until EOD]");
        toastFn.accept(ev.title + " (" + ev.stockCode.toUpperCase() + ")");
        refreshFn.run();

        //  NO revert timer here anymore
    }
}
