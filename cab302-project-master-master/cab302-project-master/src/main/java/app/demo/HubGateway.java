package app.demo;

import events.MarketEvent;
import javafx.application.Platform;

public final class HubGateway {
    private static java.util.function.Consumer<MarketEvent> sink;

    // Hub calls this once to register its java.demo.EventBus
    public static void register(java.util.function.Consumer<MarketEvent> s) {
        sink = s;
    }

    // Anywhere else can call this to deliver an event to the java.demo.hub
    public static void publish(MarketEvent evt) {
        if (sink != null) {
            Platform.runLater(() -> sink.accept(evt));
        }
    }
}
