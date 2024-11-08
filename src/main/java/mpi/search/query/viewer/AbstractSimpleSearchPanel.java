/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package mpi.search.query.viewer;

import mpi.search.SearchLocale;

import mpi.search.model.SearchListener;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Base class for a simple search panel.
 * 
 * @author klasal
 */
@SuppressWarnings("serial")
public abstract class AbstractSimpleSearchPanel extends AbstractSearchPanel
    implements SearchListener {
    /** Action to close window */
    protected Action closeAction;

    /** Action to export results  */
    protected Action exportAction;

    /** Action to start search */
    protected Action startAction;

    /** Action to cancel search */
    protected Action stopAction;

    /** Panel to contain start/stop-button */
    protected StartStopPanel startStopPanel;

    /**
     * Creates a new AbstractSimpleSearchFrame object.
     *
     */
    public AbstractSimpleSearchPanel() {
        makeActions();
    }

    /**
     * Returns the close action
     * @return the close action
     */
    public Action getCloseAction() {
        return closeAction;
    }

    /**
     * Returns the export action
     * @return the export action
     */
    public Action getExportAction() {
        return exportAction;
    }

    /**
     * Returns the start action
     * @return the start action
     */
    public Action getStartAction() {
        return startAction;
    }

    /**
     * Updating gui after search has started
     */
    @Override
	public void executionStarted() {
        super.executionStarted();
        startStopPanel.showStopButton();
        updateActions();
    }

    /**
     * Updating gui after search has finished
     */
    @Override
	public void executionStopped() {
        startStopPanel.showStartButton();
        updateActions();
    }

    /**
     * All exceptions should be dealt with here.
     *
     * @param e Exception to be handled
     */
    @Override
	public void handleException(Exception e) {
        e.printStackTrace();

        String message = ((e.getMessage() == null) ||
            "".equals(e.getMessage())) ? e.toString() : e.getMessage();
        JOptionPane.showMessageDialog(this, message,
            SearchLocale.getString("Search.Exception"),
            JOptionPane.ERROR_MESSAGE);
        searchEngine.stopExecution();
    }

    /**
     * Export of results
     */
    protected abstract void export();

    /**
     * update buttons, etc.
     */
    protected void updateActions() {
        boolean executing = (searchEngine != null) &&
            searchEngine.isExecuting();
        startAction.setEnabled(!executing);
        stopAction.setEnabled(executing);
        exportAction.setEnabled(!(executing || (searchEngine == null) ||
            (searchEngine.getResult() == null) ||
            (searchEngine.getResult().getRealSize() == 0)));
    }

    private void makeActions() {
        startAction = new AbstractAction(SearchLocale.getString("Action.Search")) {
                    @Override
					public void actionPerformed(ActionEvent e) {
                        startSearch();
                    }
                };
        startAction.putValue(Action.SHORT_DESCRIPTION,
            SearchLocale.getString("Action.Tooltip.Search"));

        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                ActionEvent.SHIFT_MASK);
        startAction.putValue(Action.ACCELERATOR_KEY, ks);

        stopAction = new AbstractAction(SearchLocale.getString("Action.Cancel")) {
                    @Override
					public void actionPerformed(ActionEvent e) {
                        stopSearch();
                    }
                };
        stopAction.putValue(Action.SHORT_DESCRIPTION,
            SearchLocale.getString("Action.Tooltip.Cancel"));
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK);
        stopAction.putValue(Action.ACCELERATOR_KEY, ks);

        exportAction = new AbstractAction(SearchLocale.getString(
                    "Action.Export")) {
                    @Override
					public void actionPerformed(ActionEvent e) {
                        export();
                    }
                };
        exportAction.putValue(Action.SHORT_DESCRIPTION,
            SearchLocale.getString("Action.Tooltip.Export"));
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK);
        exportAction.putValue(Action.ACCELERATOR_KEY, ks);
    }
}
