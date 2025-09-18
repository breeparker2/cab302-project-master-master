package ui;

import javax.swing.*;
import java.awt.*;

// the swing panel for display - we will remove this for the javafx
public final class EventsPanel {
    private final JTextArea area = new JTextArea();
    private final JPanel root = new JPanel(new BorderLayout());

    public EventsPanel() {
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        root.add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public JComponent getComponent() { return root; }

    public void log(String line) {
        area.append(line + System.lineSeparator());
        area.setCaretPosition(area.getDocument().getLength());
    }
}