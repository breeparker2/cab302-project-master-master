package events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class EventBus {
    private final List<Consumer<MarketEvent>> listeners = new ArrayList<>();

    public void subscribe(Consumer<MarketEvent> listener) {
        listeners.add(listener);
    }

    public void publish(MarketEvent event) {
        for (Consumer<MarketEvent> l : listeners) {
            l.accept(event);
        }
    }
}
