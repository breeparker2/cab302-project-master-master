package app.demo;

public class Task {
    private final String text;
    private boolean done;

    public Task(String text) {
        this.text = text;
        this.done = false;
    }

    public String getText() {
        return text;
    }

    public boolean isDone() {
        return done;
    }

    public void markDone() {
        this.done = true;
    }

    @Override
    public String toString() {
        return (done ? "✔ " : "• ") + text;
    }
}