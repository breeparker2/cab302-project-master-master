package app.demo;

import events.EventDef;

import java.util.ArrayList;
import java.util.List;

// Event library so we can easily make new events
public final class EventLibrary {
    public static List<EventDef> create() {
        List<EventDef> events = new ArrayList<>();

        // the shiftPCT is the % per tick, so -1 shiftPCT will make a (-0.1 to 0.3) become (-1.1 to -0.7) for the duration
        // If we want a new event, just copy paste an event and change the inputs
        events.add(new EventDef(
                "Iron demand falls", "Negative sentiment hits iron producers.",
                "iron", -1.0, 30_000));

        events.add(new EventDef(
                "Iron demand surges", "Major contracts boost iron demand.",
                "iron", +1.0, 30_000));

        events.add(new EventDef(
                "Wood harvest slowdown", "Supply constraints reduce output.",
                "wood", -1.0, 30_000));

        events.add(new EventDef(
                "Wood demand spike", "Construction boom increases timber demand.",
                "wood", +1.0, 30_000));

        events.add(new EventDef(
                "Coal regulation tightening", "Stricter rules squeeze margins.",
                "coal", -1.0, 30_000));

        events.add(new EventDef(
                "Steel mega-project", "Infrastructure order book lifts steel.",
                "steel", +1.0, 30_000));

        events.add(new EventDef(
                "Meat health scare", "Temporary demand drop as news spreads.",
                "meat", -1.0, 30_000));

        events.add(new EventDef(
                "Paper contract win", "Large publishing deal secured.",
                "paper", +1.0, 30_000));

        events.add(new EventDef(
                "Gold safe-haven bid", "Risk-off flows lift gold.",
                "gold", +1.0, 30_000));

        // Add more by copy-pasting any line above and adjusting values.

        return events;
    }

    private EventLibrary() {}
}