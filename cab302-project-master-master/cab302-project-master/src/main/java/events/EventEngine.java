package events;

import app.demo.Stock;
import core.Market;

import java.util.*;
import java.util.function.Consumer;

/**
 * Runs the random market events and makes sure they don't fire too often.
 */
public final class EventEngine {

    //  simple settings you can tweak
    // How often the engine checks for a new event.
    public static final int TICK_MS = 6_000;                 // every 6 seconds
    // Chance that an event happens on each tick.
    public static final int CHANCE_PCT = 20;                 // 20% chance per tick
    // Daily limit for how many events can happen at all.
    public static final int MAX_EVENTS_PER_DAY = 4;
    // Minimum time gap between any two events.
    public static final long MIN_GAP_BETWEEN_TRIGGERS_MS = 90_000;   // 1.5 minutes
    // Minimum time gap before the same stock can get hit again.
    public static final long PER_STOCK_COOLDOWN_MS = 180_000;        // 3 minutes
    // How many times a specific (title + stock) can appear in one day.
    public static final int PER_EVENT_MAX_PER_DAY = 1;

    private final Market market;
    private final List<EventDef> eventDefs;      // current pool of possible events
    private final Random rng;
    private final Consumer<String> logFn;
    private final Consumer<String> toastFn;
    private final Runnable refreshFn;

    // We use a Swing timer here (not java.util.Timer) so we can pass a lambda easily.
    private final javax.swing.Timer tickTimer;

    // What’s currently active in the world (visible to other parts of the game).
    public static final class ActiveEvent {
        public final EventDef def;
        public final long endsAtMillis;
        public boolean effectReverted = false;   // set true if/when we undo the effect

        public ActiveEvent(EventDef def, long endsAtMillis) {
            this.def = def;
            this.endsAtMillis = endsAtMillis;
        }
    }

    private final List<ActiveEvent> activeEvents = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Set<String> activeKeys = new HashSet<>(); // tracks "stock|title" currently active

    // Cooldown and limit tracking
    private final Map<String, Long> lastStockTriggerAt = new HashMap<>();     // stock -> last time it fired
    private final Map<String, Integer> perEventCountToday = new HashMap<>();  // "stock|title" -> count today
    private long lastAnyTriggerAt = 0L;
    private int eventsToday = 0;

    public List<ActiveEvent> getActiveEvents() {
        return java.util.Collections.unmodifiableList(activeEvents);
    }

    /** Replace the event list while the game is running (optional). */
    public void setEventDefs(List<EventDef> defs) {
        this.eventDefs.clear();
        this.eventDefs.addAll(defs);
    }

    public EventEngine(
            Market market,
            List<EventDef> eventDefs,
            Random rng,
            Consumer<String> logFn,
            Consumer<String> toastFn,
            Runnable refreshFn
    ) {
        this.market = market;
        this.eventDefs = new ArrayList<>(eventDefs);
        this.rng = rng;
        this.logFn = logFn;
        this.toastFn = toastFn;
        this.refreshFn = refreshFn;

        // Check for new events on a fixed schedule.
        this.tickTimer = new javax.swing.Timer(TICK_MS, e -> tryTrigger());
    }

    public void start() {
        tickTimer.start();
    }

    public void stop() {
        tickTimer.stop();
    }

    /**
     * Call at the start of each new in-game day.
     * Cleans up yesterday’s events and resets counters.
     */
    public void resetDaily() {
        // Undo the impact of anything still active from yesterday.
        for (ActiveEvent ae : new ArrayList<>(activeEvents)) {
            Stock s = market.get(ae.def.stockCode);
            if (s != null) s.addShift(-ae.def.shiftPct);
            logFn.accept("Event ended (EOD): " + ae.def.title + " — " + ae.def.stockCode.toUpperCase());
        }
        activeEvents.clear();
        activeKeys.clear();

        // Reset daily counters and cooldown tracking.
        eventsToday = 0;
        lastAnyTriggerAt = 0L;
        lastStockTriggerAt.clear();
        perEventCountToday.clear();

        refreshFn.run();
    }

    // core loop

    private void tryTrigger() {
        if (eventDefs.isEmpty()) return;

        // Stop if we've reached today's limit.
        if (eventsToday >= MAX_EVENTS_PER_DAY) return;

        long now = System.currentTimeMillis();

        // Too soon since the last event? Skip this tick.
        if (now - lastAnyTriggerAt < MIN_GAP_BETWEEN_TRIGGERS_MS) return;

        // Roll the dice.
        if (rng.nextInt(100) >= CHANCE_PCT) return;

        // Pick something that isn't already active and passes all limits.
        List<EventDef> pool = new ArrayList<>(eventDefs);
        Collections.shuffle(pool, rng);

        EventDef chosen = null;
        for (EventDef ev : pool) {
            String stock = ev.stockCode.toLowerCase();
            String key = stock + "|" + ev.title;

            // Already running.
            if (activeKeys.contains(key)) continue;

            // This exact event has already been used too many times today.
            int used = perEventCountToday.getOrDefault(key, 0);
            if (used >= PER_EVENT_MAX_PER_DAY) continue;

            // The same stock fired too recently.
            long lastForStock = lastStockTriggerAt.getOrDefault(stock, 0L);
            if (now - lastForStock < PER_STOCK_COOLDOWN_MS) continue;

            // Ignore events for unknown stocks.
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

        // Apply the effect and keep it until the end of the day.
        s.addShift(ev.shiftPct);

        // We keep a timestamp but set it to "forever" to mark it as lasting until EOD.
        long endsAt = Long.MAX_VALUE;
        ActiveEvent active = new ActiveEvent(ev, endsAt);

        String stock = ev.stockCode.toLowerCase();
        String key = stock + "|" + ev.title;

        activeEvents.add(active);
        activeKeys.add(key);

        // Update counters and cooldowns.
        eventsToday++;
        long now = System.currentTimeMillis();
        lastAnyTriggerAt = now;
        lastStockTriggerAt.put(stock, now);
        perEventCountToday.merge(key, 1, Integer::sum);

        // Basic logging/notification.
        logFn.accept("Event: " + ev.title + " — " + ev.description + " [" +
                ev.stockCode.toUpperCase() + " shift " + ev.shiftPct + "pp; stays until EOD]");
        toastFn.accept(ev.title + " (" + ev.stockCode.toUpperCase() + ")");
        refreshFn.run();
    }
}
