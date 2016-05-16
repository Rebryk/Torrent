package ru.spbau.mit.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Created by rebryk on 15/05/16.
 */
public class ProgressRender extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row2, int column) {
        JProgressBar bar = new JProgressBar();
        bar.setForeground(Color.red);
        bar.setValue((Integer) table.getModel().getValueAt(row2, column));
        bar.setString(Integer.toString(bar.getValue()));
        return bar;
    }
}
