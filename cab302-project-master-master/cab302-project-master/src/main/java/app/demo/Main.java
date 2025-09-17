package app.demo;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // Create the stocks we can add more by just copy pasting and cahnging values
        Market market = new Market();
        market.add(new Stock("wood", 100.00));
        market.add(new Stock("iron", 100.00));
        market.add(new Stock("coal", 100.00));
        market.add(new Stock("steel", 100.00));
        market.add(new Stock("meat", 100.00));
        market.add(new Stock("paper", 100.00));
        market.add(new Stock("gold", 1850.00));

        // Build UI
        JFrame frame = new JFrame("Simple Stocks (Predefined Events)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 540);

        StocksPanel stocksPanel = new StocksPanel(market);
        EventsPanel eventsPanel = new EventsPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Stocks", stocksPanel.getComponent());
        tabs.addTab("Events", eventsPanel.getComponent());
        frame.add(tabs);
        frame.setVisible(true);

        Random rng = new Random();

        // this refreshes the swng ui panel to display new values every 1 second
        new javax.swing.Timer(1000, e -> {
            market.tickAll(rng);
            stocksPanel.refresh();
        }).start();

        // Event engine startup
        var eventEngine = new EventEngine(
                market,
                EventLibrary.create(),
                rng,
                eventsPanel::log,
                msg -> showToast(frame, msg),
                stocksPanel::refresh
        );
        eventEngine.start();
    }

    // The toast thingy you had setup in previous code for bottom pop up display
    private static void showToast(JFrame frame, String message) {
        JLabel toast = new JLabel(message, SwingConstants.CENTER);
        toast.setOpaque(true);
        toast.setBackground(new Color(255, 255, 200));
        toast.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLayeredPane lp = frame.getRootPane().getLayeredPane();
        int w = frame.getContentPane().getWidth();
        int h = 32;
        toast.setBounds(0, frame.getContentPane().getHeight() - h - 8, w, h);
        lp.add(toast, JLayeredPane.POPUP_LAYER);
        lp.revalidate(); lp.repaint();

        javax.swing.Timer t = new javax.swing.Timer(2000, e -> {
            lp.remove(toast);
            lp.revalidate();
            lp.repaint();
            ((javax.swing.Timer)e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }
}