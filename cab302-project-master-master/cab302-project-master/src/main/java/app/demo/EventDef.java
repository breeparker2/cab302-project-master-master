package app.demo;

// event predetermined structure for easy editing
public final class EventDef {
    public final String title;
    public final String description;
    public final String stockCode;
    public final double shiftPct;
    public final int durationMs;

    public EventDef(String title, String description,
                    String stockCode, double shiftPct, int durationMs) {
        this.title = title;
        this.description = description;
        this.stockCode = stockCode;
        this.shiftPct = shiftPct;
        this.durationMs = durationMs;
    }

    // 👇 new convenience constructor
    public EventDef(String title, String stockCode, double shiftPct, int durationMs) {
        this(title, "", stockCode, shiftPct, durationMs);
    }

    @Override public String toString() {
        String pol = shiftPct >= 0 ? "positive" : "negative";
        return title + " — " + stockCode.toUpperCase() + " (" + pol + ")";
    }
}
