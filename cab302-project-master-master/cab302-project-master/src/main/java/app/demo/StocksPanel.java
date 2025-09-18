package app.demo;

import core.Market;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;

// stocks panel basically copied the events panel - this changes when we use javafx
public final class StocksPanel {
    private static final String[] COLS = {"CODE", "PRICE", "CHANGE%"};
    private static final DecimalFormat MONEY = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat PCT   = new DecimalFormat("+0.00%;-0.00%");

    private final Market market;
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel root;

    public StocksPanel(Market market) {
        this.market = market;
        this.model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        this.table = new JTable(model);
        this.table.setRowHeight(24);

        for (Stock s : market.list()) {
            model.addRow(new Object[] { s.code, MONEY.format(s.price), PCT.format(0.0) });
        }

        this.root = new JPanel(new BorderLayout());
        this.root.add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public JComponent getComponent() { return root; }

    public void refresh() {
        for (int i = 0; i < market.list().size(); i++) {
            Stock s = market.list().get(i);
            double delta = (s.price - s.lastPrice) / s.lastPrice;
            model.setValueAt(MONEY.format(s.price), i, 1);
            model.setValueAt(PCT.format(delta), i, 2);
        }
    }
}