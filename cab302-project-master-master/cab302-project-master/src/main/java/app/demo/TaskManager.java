package app.demo;

import events.MarketEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.Set;

public final class TaskManager {
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Set<String> dayKeys = new HashSet<>();  // Prevent duplication

    public ObservableList<Task> getTasks() { return tasks; }
    public void resetForNewDay() { tasks.clear(); dayKeys.clear(); }
    public void markAllDone() { tasks.forEach(Task::markDone); tasks.setAll(FXCollections.observableArrayList(tasks)); }
    public void clear() { tasks.clear(); dayKeys.clear(); }

    public void onEvent(MarketEvent evt) {
        String key = evt.type + "|" + evt.detail;
        if (dayKeys.contains(key)) return;
        String label = makePlayerAction(evt);
        tasks.add(new Task(label));
        dayKeys.add(key);
        if (tasks.size() > 20) tasks.remove(0);
    }

    private String makePlayerAction(MarketEvent evt) {
        return switch (evt.type) {
            case "OPEC"      -> "Energy: adjust OIL positioning — " + evt.detail;
            case "EARNINGS"  -> "Decide BHP move on earnings — " + evt.detail;
            case "RATE_HIKE" -> "Rebalance rate-sensitive names — " + evt.detail;
            case "NEWS"      -> "Review positions affected — " + evt.detail;
            default          -> "Respond: " + evt.detail;
        };
    }
}
