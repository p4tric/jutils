package my.jutils.jswing;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * Class to manage the widths of columns in a table.
 * <p>
 * Various properties control how the width of the column is calculated.
 * Another property controls whether column width calculation should be dynamic.
 * Finally, various Actions will be added to the table to allow the user
 * to customize the functionality.
 *
 * This class was designed to be used with tables that use an auto resize mode
 * of AUTO_RESIZE_OFF. With all other modes you are constrained as the width
 * of the columns must fit inside the table. So if you increase one column, one
 * or more of the other columns must decrease. Because of this the resize mode
 * of RESIZE_ALL_COLUMNS will work the best.
 */
public final class TableColumnAdjuster implements PropertyChangeListener, TableModelListener {

    private JTable table;
    private int spacing;
    private boolean isColumnHeaderIncluded;
    private boolean isColumnDataIncluded;
    private boolean isOnlyAdjustLarger;
    private boolean isDynamicAdjustment;
    private final Map<TableColumn, Integer> columnSizes;

    /**
     * Specify the table and use default spacing.
     * 
     * @param table Adjusting JTable
     */
    public TableColumnAdjuster(JTable table) {
        this(table, 6);
    }

    /**
     * Specify the table and spacing.
     * 
     * @param table Adjusting JTable
     * @param spacing Column spacing
     */
    public TableColumnAdjuster(JTable table, int spacing) {
        this.columnSizes = new HashMap<TableColumn, Integer>();
        this.table = table;
        this.spacing = spacing;
        setColumnHeaderIncluded(true);
        setColumnDataIncluded(true);
        setOnlyAdjustLarger(false);
        setDynamicAdjustment(false);
        installActions();
    }

    /**
     * Adjust the widths of all the columns in the table.
     */
    public void adjustColumns() {
        TableColumnModel tcm = table.getColumnModel();

        for (int i = 0; i < tcm.getColumnCount(); i++) {
            adjustColumn(i);
        }
    }

    /**
     * Adjust the width of the specified column in the table.
     * 
     * @param column Column index
     */
    public void adjustColumn(final int column) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (!tableColumn.getResizable()) {
            return;
        }
        int columnHeaderWidth = getColumnHeaderWidth(column);
        int columnDataWidth = getColumnDataWidth(column);
        int preferredWidth = Math.max(columnHeaderWidth, columnDataWidth);

        updateTableColumn(column, preferredWidth);
    }

    /**
     *  Calculated the width based on the column name.
     * 
     * @param column Column index
     * @return Calculated width
     */
    private int getColumnHeaderWidth(int column) {
        if (!isColumnHeaderIncluded) {
            return 0;
        }

        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        Object value = tableColumn.getHeaderValue();
        TableCellRenderer renderer = tableColumn.getHeaderRenderer();

        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component c = renderer.getTableCellRendererComponent(table, value, false, false, -1, column);
        return c.getPreferredSize().width;
    }
    
    /**
     *  Calculate the width based on the widest cell renderer for the
     *  given column.
     * 
     * @param column Column index
     * @return Widest cell renderer
     */
    private int getColumnDataWidth(int column) {
        if (!isColumnDataIncluded) {
            return 0;
        }
        int preferredWidth = 0;
        int maxWidth = table.getColumnModel().getColumn(column).getMaxWidth();

        for (int row = 0; row < table.getRowCount(); row++) {
            preferredWidth = Math.max(preferredWidth, getCellDataWidth(row, column));
            // We've exceeded the maximum width, no need to check other rows
            if (preferredWidth >= maxWidth) {
                break;
            }
        }
        return preferredWidth;
    }

    /**
     * Get the preferred width for the specified cell.
     * 
     * @param row Table row index
     * @param column Table column index
     * @return Preferred width
     */
    private int getCellDataWidth(int row, int column) {
        // Inovke the renderer for the cell to calculate the preferred width
        TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
        Component c = table.prepareRenderer(cellRenderer, row, column);
        return c.getPreferredSize().width + table.getIntercellSpacing().width;
    }

    /**
     * Update the TableColumn with the newly calculated width.
     * 
     * @param column Column index
     * @param width Column width
     */
    private void updateTableColumn(int column, int width) {
        final TableColumn tableColumn = table.getColumnModel().getColumn(column);

        if (!tableColumn.getResizable()) {
            return;
        }

        width += spacing;

        // Don't shrink the column width
        if (isOnlyAdjustLarger) {
            width = Math.max(width, tableColumn.getPreferredWidth());
        }

        columnSizes.put(tableColumn, tableColumn.getWidth());

        table.getTableHeader().setResizingColumn(tableColumn);
        tableColumn.setWidth(width);
    }

    /**
     * Restore the widths of the columns in the table to its previous width.
     */
    public void restoreColumns() {
        TableColumnModel tcm = table.getColumnModel();

        for (int i = 0; i < tcm.getColumnCount(); i++) {
            restoreColumn(i);
        }
    }

    /**
     * Restore the width of the specified column to its previous width.
     * 
     * @param column Column index
     */
    private void restoreColumn(int column) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        Integer width = columnSizes.get(tableColumn);

        if (width != null) {
            table.getTableHeader().setResizingColumn(tableColumn);
            tableColumn.setWidth(width);
        }
    }

    /**
     * Indicates whether to include the header in the width calculation.
     * 
     * @param isColumnHeaderIncluded Include column header?
     */
    public void setColumnHeaderIncluded(boolean isColumnHeaderIncluded) {
        this.isColumnHeaderIncluded = isColumnHeaderIncluded;
    }

    /**
     * Indicates whether to include the model data in the width calculation.
     * 
     * @param isColumnDataIncluded Include column data?
     */
    public void setColumnDataIncluded(boolean isColumnDataIncluded) {
        this.isColumnDataIncluded = isColumnDataIncluded;
    }

    /**
     * Indicates whether columns can only be increased in size.
     * 
     * @param isOnlyAdjustLarger Adjust larger only?
     */
    public void setOnlyAdjustLarger(boolean isOnlyAdjustLarger) {
        this.isOnlyAdjustLarger = isOnlyAdjustLarger;
    }

    /**
     * Indicate whether changes to the model should cause the width to be
     * dynamically recalculated.
     * 
     * @param isDynamicAdjustment Dynamical adjustment?
     */
    public void setDynamicAdjustment(boolean isDynamicAdjustment) {
        //  May need to add or remove the TableModelListener when changed
        if (this.isDynamicAdjustment != isDynamicAdjustment) {
            if (isDynamicAdjustment) {
                table.addPropertyChangeListener(this);
                table.getModel().addTableModelListener(this);
            } else {
                table.removePropertyChangeListener(this);
                table.getModel().removeTableModelListener(this);
            }
        }

        this.isDynamicAdjustment = isDynamicAdjustment;
    }
    
    /***
     * Implement the PropertyChangeListener
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
	// When the TableModel changes we need to update the listeners
        // and column widths
        if ("model".equals(e.getPropertyName())) {
            TableModel model = (TableModel) e.getOldValue();
            model.removeTableModelListener(this);
            model = (TableModel) e.getNewValue();
            model.addTableModelListener(this);
            adjustColumns();
        }
    }

    /***
     * Implement the TableModelListener
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        if (!isColumnDataIncluded) {
            return;
        }

        if (e.getType() == TableModelEvent.UPDATE) { //  A cell has been updated
            int column = table.convertColumnIndexToView(e.getColumn());
            //  Only need to worry about an increase in width for this cell
            if (isOnlyAdjustLarger) {
                int row = e.getFirstRow();
                TableColumn tableColumn = table.getColumnModel().getColumn(column);

                if (tableColumn.getResizable()) {
                    int width = getCellDataWidth(row, column);
                    updateTableColumn(column, width);
                }
            } else { // Could be an increase of decrease so check all rows
                adjustColumn(column);
            }
        } else { //  The update affected more than one column so adjust all columns
            adjustColumns();
        }
    }

    /**
     * Install Actions to give user control of certain functionality.
     */
    private void installActions() {
        installColumnAction(true, true, "adjustColumn", "control ADD");
        installColumnAction(false, true, "adjustColumns", "control shift ADD");
        installColumnAction(true, false, "restoreColumn", "control SUBTRACT");
        installColumnAction(false, false, "restoreColumns", "control shift SUBTRACT");

        installToggleAction(true, false, "toggleDynamic", "control MULTIPLY");
        installToggleAction(false, true, "toggleLarger", "control DIVIDE");
    }

    /**
     * Update the input and action maps with a new ColumnAction.
     */
    private void installColumnAction(
            boolean isSelectedColumn, boolean isAdjust, String key, String keyStroke) {
        Action action = new ColumnAction(isSelectedColumn, isAdjust);
        KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
        table.getInputMap().put(ks, key);
        table.getActionMap().put(key, action);
    }

    /**
     * Update the input and action maps with new ToggleAction.
     */
    private void installToggleAction(
            boolean isToggleDynamic, boolean isToggleLarger, String key, String keyStroke) {
        Action action = new ToggleAction(isToggleDynamic, isToggleLarger);
        KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
        table.getInputMap().put(ks, key);
        table.getActionMap().put(key, action);
    }

    /**
     * Action to adjust or restore the width of a single column or all columns.
     */
    class ColumnAction extends AbstractAction {

        private final boolean isSelectedColumn;
        private final boolean isAdjust;

        public ColumnAction(boolean isSelectedColumn, boolean isAdjust) {
            this.isSelectedColumn = isSelectedColumn;
            this.isAdjust = isAdjust;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Handle selected column(s) width change actions
            if (isSelectedColumn) {
                int[] columns = table.getSelectedColumns();

                for (int i = 0; i < columns.length; i++) {
                    if (isAdjust) {
                        adjustColumn(columns[i]);
                    } else {
                        restoreColumn(columns[i]);
                    }
                }
            } else {
                if (isAdjust) {
                    adjustColumns();
                } else {
                    restoreColumns();
                }
            }
        }
    }

    /**
     *  Toggle properties of the TableColumnAdjuster so the user can
     *  customize the functionality to their preferences.
     */
    class ToggleAction extends AbstractAction {

        private final boolean isToggleDynamic;
        private final boolean isToggleLarger;

        public ToggleAction(boolean isToggleDynamic, boolean isToggleLarger) {
            this.isToggleDynamic = isToggleDynamic;
            this.isToggleLarger = isToggleLarger;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isToggleDynamic) {
                setDynamicAdjustment(!isDynamicAdjustment);
                return;
            }

            if (isToggleLarger) {
                setOnlyAdjustLarger(!isOnlyAdjustLarger);
                return;
            }
        }
    }
}
