package events;

import java.time.LocalDateTime;

public final class MarketEvent {
    public final String type;
    public final String country;
    public final String detail;
    public final LocalDateTime when;

    public MarketEvent(String type, String country, String detail) {
        this.type = type;
        this.country = country;
        this.detail = detail;
        this.when = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return type + " [" + country + "]: " + detail;
    }
}

