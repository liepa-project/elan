package mpi.eudico.client.annotator.gui;

import mpi.eudico.client.annotator.Constants;
import mpi.eudico.util.BasicControlledVocabulary;
import mpi.eudico.util.CVEntry;
import mpi.eudico.util.ControlledVocabulary;
import mpi.eudico.util.ExternalCV;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static mpi.eudico.client.annotator.util.ResourceUtil.getImageIconFrom;

/**
 * A panel to edit the entries of a Controlled Vocabulary.
 */
@SuppressWarnings("serial")
public class EditCVPanel extends JPanel implements ActionListener,
                                                   ListSelectionListener {
    /**
     * Empty string to fill UI elements when values/description are empty.
     */
    protected static final String EMPTY = "";
    private static final int MOVE_BUTTON_SIZE = 24;
    private static final int MINIMAL_ENTRY_PANEL_WIDTH = 240;
    public static final String RESOURCE_TOP_16_GIF = "/mpi/eudico/client/annotator/resources/Top16.gif";
    public static final String RESOURCE_BOTTOM_16_GIF = "/mpi/eudico/client/annotator/resources/Bottom16.gif";
    public static final String TOOLBAR_BUTTON_GRAPHICS_NAVIGATION_UP_16_GIF = "/toolbarButtonGraphics/navigation/Up16.gif";
    public static final String TOOLBAR_BUTTON_GRAPHICS_NAVIGATION_DOWN_16_GIF =
        "/toolbarButtonGraphics/navigation/Down16.gif";
    public static final String TOOLBAR_BUTTON_GRAPHICS_GENERAL_REDO_16_GIF = "/toolbarButtonGraphics/general/Redo16.gif";
    public static final String TOOLBAR_BUTTON_GRAPHICS_GENERAL_UNDO_16_GIF = "/toolbarButtonGraphics/general/Undo16.gif";
    public static final String RESOURCE_REMOVE_GIF = "/mpi/eudico/client/annotator/resources/Remove.gif";
    /**
     * controlled vocabulary identifier
     */
    protected BasicControlledVocabulary cv;

    private boolean ascending = false;
    private boolean descending = true;

    // internal caching fields
    /**
     * current entry identifier
     */
    protected CVEntry currentEntry;
    /**
     * add entry button
     */
    protected JButton addEntryButton;
    /**
     * change entry button
     */
    protected JButton changeEntryButton;
    /**
     * delete entry button
     */
    protected JButton deleteEntryButton;
    /**
     * move down button
     */
    protected JButton moveDownButton;
    /**
     * move to bottom button
     */
    protected JButton moveToBottomButton;
    /**
     * move to top button
     */
    protected JButton moveToTopButton;
    /**
     * move up button
     */
    protected JButton moveUpButton;
    /**
     * redo  button
     */
    protected JButton redoButton;
    /**
     * undo  button
     */
    protected JButton undoButton;
    /**
     * ascending button
     */
    protected JButton ascendingButton;
    /**
     * descending button
     */
    protected JButton descendingButton;
    /**
     * entry desc label
     */
    protected JLabel entryDescLabel;
    /**
     * entry value label
     */
    protected JLabel entryValueLabel;
    /**
     * title  label
     */
    protected JLabel titleLabel;
    /**
     * entry table
     */
    protected JTable entryTable;
    /**
     * entry desc text field
     */
    protected JTextField entryDescTextField;
    /**
     * entry value text field
     */
    protected JTextField entryValueTextField;
    /**
     * invalid value message
     */
    protected String invalidValueMessage = "Invalid value";
    /**
     * value exists message
     */
    protected String valueExistsMessage = "Value exists";
    private CVTableModel entryTableModel;
    // dcr
    /**
     * dcr panel
     */
    protected JPanel dcrPanel;
    /**
     * dcr label
     */
    protected JLabel dcrLabel;
    /**
     * dcr text field
     */
    protected JTextField dcrField;
    /**
     * dcr id field
     */
    protected JTextField dcIdField;
    /**
     * dcr remove button
     */
    protected JButton dcrRemoveButton;
    /**
     * dcr button
     */
    protected JButton dcrButton;
    // more options
    boolean enableMoreOptions = false;
    /**
     * more options button
     */
    protected JButton moreOptionsButton;

    // ui elements
    private final UndoManager undoManager;
    private CVEntry selectedTableEntry;
    private int selectedTableColumn;

    /**
     * Creates a panel without an initial CV.
     */
    public EditCVPanel() {
        this(null);
    }

    /**
     * Creates a panel without an initial CV and adds a button for additional options.
     *
     * @param enableMoreOptions if {@code true}, enables more options for entries
     */
    public EditCVPanel(boolean enableMoreOptions) {
        this(null, enableMoreOptions);
    }

    /**
     * Creates a panel with the specified CV.
     *
     * @param cv the Controlled Vocabulary to edit
     */
    public EditCVPanel(BasicControlledVocabulary cv) {
        this(cv, false);
    }

    /**
     * Creates a panel with a CV and with more options (for preferences.)
     *
     * @param cv the Controlled Vocabulary to edit
     * @param enableMoreOptions if {@code true}, enables more options for entries
     */
    public EditCVPanel(BasicControlledVocabulary cv, boolean enableMoreOptions) {
        undoManager = new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                super.undoableEditHappened(e);
                updateUndoRedoButtons();
            }
        };
        this.enableMoreOptions = enableMoreOptions;
        makeLayout();
        setSelectionListener();
        setControlledVocabulary(cv);
    }

    /**
     * Sets the selection listener
     */
    protected void setSelectionListener() {
        entryTable.getSelectionModel().addListSelectionListener(this);
        entryTable.getColumnModel().getSelectionModel().addListSelectionListener(this);
    }

    /**
     * Clears the selection listener
     */
    protected void clearSelectionListener() {
        entryTable.getSelectionModel().removeListSelectionListener(this);
        entryTable.getColumnModel().getSelectionModel().removeListSelectionListener(this);
    }

    /**
     * Sets the (new) CV to edit, resets the UI and clears the edit history.
     *
     * @param cv the Controlled Vocabulary
     */
    public void setControlledVocabulary(BasicControlledVocabulary cv) {
        this.cv = cv;
        entryTableModel.setControlledVocabulary(cv);
        undoManager.discardAllEdits();
        updateLabels();
        resetViewer();
        entryValueTextField.setEnabled(cv != null);
        entryDescTextField.setEnabled(cv != null);

        if (cv instanceof ControlledVocabulary) {
            ((ControlledVocabulary) cv).addUndoableEditListener(undoManager);
            undoButton.setVisible(true);
            redoButton.setVisible(true);
            dcrButton.setEnabled(true);
            for (int i = 0; i < entryTable.getColumnCount(); i++) {
                TableColumn c = entryTable.getColumnModel().getColumn(i);
                c.setMinWidth(50);
                c.setPreferredWidth(150);
            }

            if (cv instanceof ExternalCV) {
                addEntryButton.setEnabled(false);
                changeEntryButton.setEnabled(false);
                deleteEntryButton.setEnabled(false);
                moveDownButton.setEnabled(false);
                moveToBottomButton.setEnabled(false);
                moveToTopButton.setEnabled(false);
                moveUpButton.setEnabled(false);
                redoButton.setEnabled(false);
                undoButton.setEnabled(false);
                entryDescTextField.setEnabled(false);
                entryValueTextField.setEnabled(false);
                dcrButton.setEnabled(false);
                moreOptionsButton.setEnabled(true);
                ascendingButton.setEnabled(false);
                descendingButton.setEnabled(false);
            }
        } else {
            undoButton.setVisible(false);
            redoButton.setVisible(false);
        }
    }

    /**
     * The button actions.
     *
     * @param actionEvent the actionEvent
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();

        // check source equality
        if (source == entryValueTextField) {
            entryDescTextField.requestFocus();
        } else if ((source == addEntryButton) || (source == entryDescTextField)) {
            addEntry();
        } else if (source == changeEntryButton) {
            changeEntry();
        } else if (source == deleteEntryButton) {
            deleteEntries();
        } else if (source == moveToTopButton) {
            moveEntries(BasicControlledVocabulary.MOVE_TO_TOP);
        } else if (source == moveUpButton) {
            moveEntries(BasicControlledVocabulary.MOVE_UP);
        } else if (source == moveDownButton) {
            moveEntries(BasicControlledVocabulary.MOVE_DOWN);
        } else if (source == moveToBottomButton) {
            moveEntries(BasicControlledVocabulary.MOVE_TO_BOTTOM);
        } else if (source == undoButton) {
            undo();
        } else if (source == redoButton) {
            redo();
        } else if (source == ascendingButton) {
            ascending = true;
            sortEntries();
        } else if (source == descendingButton) {
            descending = true;
            sortEntries();
        }
    }

    /**
     * For test purposes. Opens a frame with this panel and a test controlled vocabulary.
     *
     * @param args no arguments
     */
    public static void main(String[] args) {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        BasicControlledVocabulary cv = new BasicControlledVocabulary("Name", "Description 1");
        cv.addEntry(new CVEntry(cv, 0, "Entry 1", "Entry description 1"));
        cv.addEntry(new CVEntry(cv, 0, "Entry 2", "Entry description 2"));
        cv.addEntry(new CVEntry(cv, 0, "Entry 3", "Entry description 3"));

        JPanel p = new EditCVPanel(cv);
        frame.getContentPane().add(p);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Handles a change in the selection in the entry table. For a new selection there may be 1 or 2 events, from either
     * SelectionModel, depending on whether the row and/or column of the selection changed.
     *
     * @param lse the list selection event
     */
    @Override
    public void valueChanged(ListSelectionEvent lse) {
        if (lse.getValueIsAdjusting()) {
            return;
        }
        if (lse.getSource() == entryTable.getSelectionModel() || lse.getSource() == entryTable.getColumnModel()
                                                                                              .getSelectionModel()) {
            // Register the current selection at this time,
            // so that it can't change behind our backs.
            int row = getSelectedEntryIndex();
            if (row >= 0 && row < entryTableModel.getRowCount()) {
                selectedTableEntry = entryTableModel.getEntry(row);
                int col = entryTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
                selectedTableColumn = entryTable.convertColumnIndexToModel(col);
            } else {
                selectedTableEntry = null;
                selectedTableColumn = 0;
            }

            updateEntryButtons();
            updateTextFields();
            updateSortButtons();
        }

    }

    /**
     * Returns the selected entry index
     *
     * @return the index
     */
    protected int getSelectedEntryIndex() {
        int row = entryTable.getSelectionModel().getLeadSelectionIndex();
        return entryTable.convertRowIndexToModel(row);
    }

    //    public void setSelectedEntryIndex(int index) {
    //        index = entryTable.convertRowIndexToView(index);
    //        entryTable.getSelectionModel().setSelectionInterval(index, index);
    //    }

    /**
     * Adds an entry to the current CV. When checking the uniqueness of the entry value, values are compared case sensitive.
     */
    protected void addEntry() {
        if (cv == null) {
            return;
        }

        String entry = entryValueTextField.getText();

        entry = entry.trim();

        if (entry.length() == 0) {
            showWarningDialog(invalidValueMessage);

            return;
        }

        int language = getSelectedColumn();

        if (cv.containsValue(language, entry)) {
            showWarningDialog(valueExistsMessage);
        } else {
            String desc = entryDescTextField.getText();

            if (desc != null) {
                desc = desc.trim();
            }

            CVEntry newEntry = new CVEntry(cv);
            newEntry.setValue(language, entry);
            newEntry.setDescription(language, desc);
            cv.addEntry(newEntry);
            updateList();

            //make text fields free for next input!
            setSelectedEntry(null);
        }
    }

    /**
     * Changes the value and/or description of an existing entry. Checks whether  the specified value is unique within the
     * current ControlledVocabulary.
     */
    protected void changeEntry() {
        if (cv == null) {
            return;
        }

        String newValue = entryValueTextField.getText().trim();
        int language = getSelectedColumn();

        // only object if ALL language values are empty
        if (newValue.isEmpty()) {
            boolean ok = false;
            for (int i = 0; i < cv.getNumberOfLanguages(); i++) {
                if (i != language && !currentEntry.getValue(i).isEmpty()) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                showWarningDialog(invalidValueMessage);
                entryValueTextField.setText((currentEntry != null) ? currentEntry.getValue(language) : "");

                return;
            }
        }

        String newDescription = entryDescTextField.getText().trim();

        if (newValue.equals(currentEntry.getValue(language))) {
            if (newDescription != null && !newDescription.equals(currentEntry.getDescription(language))) {

                CVEntry newEntry = new CVEntry(currentEntry, language, newValue, newDescription);
                cv.replaceEntry(currentEntry, newEntry);
                updateList();
                setSelectedEntry(newEntry);
            }

            return;
        }

        // entry value has changed...
        if (cv.containsValue(language, newValue)) {
            showWarningDialog(valueExistsMessage);
        } else {
            CVEntry newEntry = new CVEntry(currentEntry, language, newValue, newDescription);
            cv.replaceEntry(currentEntry, newEntry);
            updateList();
            setSelectedEntry(newEntry);
        }
    }

    /**
     * Deletes the selected entry/entries from the current ControlledVocabulary.
     */
    protected void deleteEntries() {
        int[] selEntries = entryTable.getSelectedRows();

        if (selEntries.length == 0) {
            return;
        }

        CVEntry[] entries = new CVEntry[selEntries.length];

        for (int i = 0; i < entries.length; i++) {
            int index = entryTable.convertRowIndexToModel(selEntries[i]);
            entries[i] = entryTableModel.getEntry(index);
        }

        cv.removeEntries(entries);
        updateList();
        setSelectedEntry(null);
    }

    /**
     * This method is called from within the constructor to initialize the dialog's components.
     */
    protected void makeLayout() {
        JPanel moveEntriesPanel;
        JPanel sortEntriesPanel;

        GridBagConstraints gridBagConstraints;

        ImageIcon topIcon = getImageIconFrom(RESOURCE_TOP_16_GIF).get();
        ImageIcon bottomIcon = getImageIconFrom(RESOURCE_BOTTOM_16_GIF).get();
        ImageIcon upIcon = getImageIconFrom(TOOLBAR_BUTTON_GRAPHICS_NAVIGATION_UP_16_GIF).get();
        ImageIcon downIcon = getImageIconFrom(TOOLBAR_BUTTON_GRAPHICS_NAVIGATION_DOWN_16_GIF).get();
        ImageIcon redoIcon = getImageIconFrom(TOOLBAR_BUTTON_GRAPHICS_GENERAL_REDO_16_GIF).get();
        ImageIcon undoIcon = getImageIconFrom(TOOLBAR_BUTTON_GRAPHICS_GENERAL_UNDO_16_GIF).get();
        ImageIcon removeRefIcon = getImageIconFrom(RESOURCE_REMOVE_GIF).get();

        entryTableModel = new CVTableModel();
        entryTable = new ScrollFriendlyTable(entryTableModel);
        entryTable.setCellSelectionEnabled(true);
        entryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        entryValueLabel = new JLabel();
        entryValueTextField = new JTextField();
        addEntryButton = new JButton();
        addEntryButton.setEnabled(false);
        changeEntryButton = new JButton();
        changeEntryButton.setEnabled(false);
        deleteEntryButton = new JButton();
        deleteEntryButton.setEnabled(false);

        titleLabel = new JLabel();
        entryDescLabel = new JLabel();
        entryDescTextField = new JTextField();

        dcrPanel = new JPanel(new GridBagLayout());
        dcrLabel = new JLabel();
        dcrField = new JTextField();
        dcIdField = new JTextField();
        dcIdField.setEditable(false);
        dcrField.setEditable(false);
        //dcrField.setEnabled(false);
        dcrRemoveButton = new JButton(removeRefIcon);
        dcrButton = new JButton();

        moveEntriesPanel = new JPanel();
        moveUpButton = new JButton(upIcon);
        moveUpButton.setEnabled(false);
        moveToTopButton = new JButton(topIcon);
        moveToTopButton.setEnabled(false);
        moveDownButton = new JButton(downIcon);
        moveDownButton.setEnabled(false);
        moveToBottomButton = new JButton(bottomIcon);
        moveToBottomButton.setEnabled(false);
        undoButton = new JButton(undoIcon);
        undoButton.setEnabled(false);
        redoButton = new JButton(redoIcon);
        redoButton.setEnabled(false);


        Dimension prefDim = new Dimension(MINIMAL_ENTRY_PANEL_WIDTH, MOVE_BUTTON_SIZE);
        Dimension buttonDimension = new Dimension(MOVE_BUTTON_SIZE, MOVE_BUTTON_SIZE);

        Insets insets = new Insets(2, 6, 2, 6);

        // entry sorting buttons
        moveEntriesPanel.setLayout(new GridBagLayout());

        moveToTopButton.addActionListener(this);
        moveToTopButton.setPreferredSize(buttonDimension);
        moveToTopButton.setMaximumSize(buttonDimension);
        moveToTopButton.setMinimumSize(buttonDimension);
        moveUpButton.addActionListener(this);
        moveUpButton.setPreferredSize(buttonDimension);
        moveUpButton.setMaximumSize(buttonDimension);
        moveUpButton.setMinimumSize(buttonDimension);
        moveDownButton.addActionListener(this);
        moveDownButton.setPreferredSize(buttonDimension);
        moveDownButton.setMaximumSize(buttonDimension);
        moveDownButton.setMinimumSize(buttonDimension);
        moveToBottomButton.addActionListener(this);
        moveToBottomButton.setPreferredSize(buttonDimension);
        moveToBottomButton.setMaximumSize(buttonDimension);
        moveToBottomButton.setMinimumSize(buttonDimension);
        undoButton.addActionListener(this);
        undoButton.setPreferredSize(buttonDimension);
        undoButton.setMaximumSize(buttonDimension);
        undoButton.setMinimumSize(buttonDimension);
        redoButton.addActionListener(this);
        redoButton.setPreferredSize(buttonDimension);
        redoButton.setMaximumSize(buttonDimension);
        redoButton.setMinimumSize(buttonDimension);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.insets = insets;
        moveEntriesPanel.add(moveToTopButton, gridBagConstraints);
        moveEntriesPanel.add(moveUpButton, gridBagConstraints);
        moveEntriesPanel.add(moveDownButton, gridBagConstraints);
        moveEntriesPanel.add(moveToBottomButton, gridBagConstraints);
        moveEntriesPanel.add(undoButton, gridBagConstraints);
        moveEntriesPanel.add(redoButton, gridBagConstraints);

        //sort entries button

        sortEntriesPanel = new JPanel();
        sortEntriesPanel.setLayout(new GridBagLayout());

        ascendingButton = new JButton("Sort A-Z");
        ascendingButton.setEnabled(false);
        ascendingButton.addActionListener(this);
        descendingButton = new JButton("Sort Z-A");
        descendingButton.setEnabled(false);
        descendingButton.addActionListener(this);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.insets = insets;
        sortEntriesPanel.add(ascendingButton, gridBagConstraints);
        sortEntriesPanel.add(descendingButton, gridBagConstraints);

        // dcr
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        dcrPanel.add(dcrLabel, gridBagConstraints);
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(4, 0, 0, 6);
        dcrPanel.add(dcrField, gridBagConstraints);
        // add the id field?
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.5;
        dcrPanel.add(dcIdField, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(0, 6, 0, 0);
        dcrPanel.add(dcrRemoveButton, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        dcrPanel.add(dcrButton, gridBagConstraints);

        //other subcomponents
        JScrollPane entryPane = new JScrollPane(entryTable);

        entryValueTextField.setPreferredSize(prefDim);
        entryValueTextField.setMinimumSize(prefDim);

        entryDescTextField.setPreferredSize(prefDim);
        entryDescTextField.setMinimumSize(prefDim);

        //main layout
        setLayout(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 10;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = insets;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 10.0;
        add(entryPane, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = insets;
        add(sortEntriesPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = insets;
        add(entryValueLabel, gridBagConstraints);

        gridBagConstraints.gridy = GridBagConstraints.RELATIVE;
        add(entryValueTextField, gridBagConstraints);
        add(entryDescLabel, gridBagConstraints);
        add(entryDescTextField, gridBagConstraints);
        add(dcrPanel, gridBagConstraints);
        add(addEntryButton, gridBagConstraints);
        add(changeEntryButton, gridBagConstraints);
        add(deleteEntryButton, gridBagConstraints);
        if (enableMoreOptions) {
            moreOptionsButton = new JButton();
            add(moreOptionsButton, gridBagConstraints);
        }
        add(moveEntriesPanel, gridBagConstraints);

        //add(sortEntriesPanel, gridBagConstraints);

        undoButton.setToolTipText(undoManager.getUndoPresentationName());

        entryValueTextField.addActionListener(this);
        entryDescTextField.addActionListener(this);
        addEntryButton.addActionListener(this);
        changeEntryButton.addActionListener(this);
        deleteEntryButton.addActionListener(this);
        dcrRemoveButton.addActionListener(this);
        dcrRemoveButton.setPreferredSize(buttonDimension);
        dcrRemoveButton.setMaximumSize(buttonDimension);
        dcrRemoveButton.setMinimumSize(buttonDimension);
        dcrButton.addActionListener(this);
        dcrPanel.setVisible(false); // default
        if (enableMoreOptions) {
            moreOptionsButton.addActionListener(this);
        }
        // set default font
        entryDescTextField.setFont(Constants.DEFAULTFONT);
        entryValueTextField.setFont(Constants.DEFAULTFONT);
        entryTable.setFont(Constants.DEFAULTFONT);
    }

    /**
     * If a subclass wants to set a new table cell renderer, it may also want to replace the table data model (so that the
     * model can return objects of the type that the renderer likes). It should do so right after calling makeLayout(), or at
     * least before setting a CV.
     *
     * @param newModel the new table model for the entry table
     */
    protected void setTableModel(CVTableModel newModel) {
        entryTableModel = newModel;
        entryTable.setModel(entryTableModel);
    }

    /**
     * Updates some UI fields after a change in the selected CV.
     */
    protected void resetViewer() {
        // reset some fields

        if (cv != null) {
            CVEntry[] entries = cv.getEntries();
            currentEntry = null;

            entryTableModel.setEntries(entries);
            addEntryButton.setEnabled(true);
        } else {
            cv = null;
            addEntryButton.setEnabled(false);
        }

        updateEntryButtons();
        updateTextFields();
        updateSortButtons();
    }

    /**
     * Since this dialog is meant to be modal a Locale change while this dialog is open  is not supposed to happen. This will
     * set the labels etc. using the current locale  strings.
     */
    protected void updateLabels() {
        moveToTopButton.setToolTipText("Top");
        moveUpButton.setToolTipText("Up");
        moveDownButton.setToolTipText("Down");
        moveToBottomButton.setToolTipText("Bottom");
        deleteEntryButton.setText("Delete");
        changeEntryButton.setText("Change");
        addEntryButton.setText("Add");
        entryDescLabel.setText("Description");
        entryValueLabel.setText("Value");
        dcrLabel.setText("ISO Data Category");
        dcrRemoveButton.setToolTipText("Remove Ref");
        dcrButton.setText("Browse...");
        setBorder(new TitledBorder("Entries"));
        undoButton.setToolTipText(undoManager.getUndoPresentationName());
        redoButton.setToolTipText(undoManager.getRedoPresentationName());
        ascendingButton.setToolTipText("Ascending order");
        descendingButton.setToolTipText("Descending order");
        if (enableMoreOptions) {
            moreOptionsButton.setText("More Options...");
        }
    }

    /**
     * Re-extracts the entries from the current CV after an add, change or delete entry operation on the CV.
     */
    protected void updateList() {
        if (cv != null) {
            clearSelectionListener();

            CVEntry[] entries = cv.getEntries();

            entryTableModel.setEntries(entries);
            setSelectionListener();
        }
    }

    /**
     * Sorts the entries from the current CV in alphabetical order, ascending or descending. The order is changed in the CV,
     * not only in the list or table.
     */
    protected void sortEntries() {
        if (cv != null) {
            CVEntry[] entries = null;
            int language = getSelectedColumn();

            if (ascending) {
                entries = cv.getEntriesSortedByAlphabet(language);
                ascending = false;
            } else if (descending) {
                entries = cv.getEntriesSortedByReverseAlphabetOrder(language);
                descending = false;
            }
            if (entries != null) {
                entryTableModel.setEntries(entries);
                entryTable.changeSelection(0, entryTable.convertColumnIndexToView(language), false, false);
            }
        }
    }

    /**
     * Changes the selected entries in the entry table.
     *
     * @param entries the entries to select
     */
    protected void setSelectedEntries(CVEntry[] entries) {
        currentEntry = null;

        if ((entries != null) && (entries.length > 0)) {
            clearSelectionListener();
            int column = entryTable.convertColumnIndexToView(getSelectedColumn());
            boolean extend = false;

            for (int i = 0; i < entryTableModel.getRowCount(); i++) {
                for (int j = 0; j < entries.length; j++) {
                    //if (entryTableModel.getEntry(i).equals(entries[j])) {
                    if (entryTableModel.getEntry(i) == entries[j]) {
                        entryTable.changeSelection(i, column, false, extend);
                        selectedTableEntry = entries[j];
                        extend = true;
                    }
                }
            }

            setSelectionListener();
        } else {
            selectedTableEntry = null;
        }

        updateEntryButtons();
        updateTextFields();
    }

    /**
     * Selects a single entry in the table.
     *
     * @param entry the entry to select
     */
    protected void setSelectedEntry(CVEntry entry) {
        if (entry != null) {
            setSelectedEntries(new CVEntry[] {entry});
        } else {
            setSelectedEntries(null);
        }
    }

    /**
     * Moves the selected entries to the bottom of the entry list.
     *
     * @param the type of the move (up, down, etc.) as defined in BasicControlledVocabulary
     */
    private void moveEntries(int moveType) {
        if (cv == null) {
            return;
        }

        int[] selEntries = entryTable.getSelectedRows();

        if (selEntries.length == 0) {
            return;
        }

        CVEntry[] entriesToBeMoved = new CVEntry[selEntries.length];

        for (int i = 0; i < selEntries.length; i++) {
            int index = entryTable.convertRowIndexToModel(selEntries[i]);
            entriesToBeMoved[i] = entryTableModel.getEntry(index);
        }
        entryTable.changeSelection(0, getSelectedColumn(), false, false);

        cv.moveEntries(entriesToBeMoved, moveType);
        updateList();
        setSelectedEntries(entriesToBeMoved);
    }

    /**
     * Invokes the redo method of the <code>UndoManager</code>.
     */
    private void redo() {
        try {
            undoManager.redo();

            updateList();
            setSelectedEntry(null);
        } catch (CannotRedoException cre) {
            //LOG.warning(LogUtil.formatStackTrace(cre));
        }

        updateUndoRedoButtons();
    }

    /**
     * Shows a warning/error dialog with the specified message string.
     *
     * @param message the message to display
     */
    protected void showWarningDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Invokes the undo method of the <code>UndoManager</code>.
     */
    private void undo() {
        try {
            undoManager.undo();

            updateList();
            setSelectedEntry(null);
        } catch (CannotUndoException cue) {
            // LOG.warning(LogUtil.formatStackTrace(cue));
        }

        updateUndoRedoButtons();
    }

    /**
     * Enables or disables buttons depending on the selected entries.
     */
    private void updateEntryButtons() {
        if ((entryTable == null) || (entryTable.getSelectedRowCount() == 0)) {
            changeEntryButton.setEnabled(false);
            deleteEntryButton.setEnabled(false);
            moveToTopButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            moveToBottomButton.setEnabled(false);
            currentEntry = null;
            if (moreOptionsButton != null) {
                moreOptionsButton.setEnabled(false);
            }
        } else if (cv instanceof ExternalCV) {
            changeEntryButton.setEnabled(false);
            deleteEntryButton.setEnabled(false);
            moveToTopButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            moveToBottomButton.setEnabled(false);
            if (moreOptionsButton != null) {
                moreOptionsButton.setEnabled((entryTable != null) && (entryTable.getSelectedRowCount() != 0));
            }
            currentEntry = getSelectedEntry();
        } else {
            // TODO: apply table.convertRowIndexToModel() - but that makes it discontinuous,
            // if at some point we'll use a RowSorter.
            int firstIndex = entryTable.getSelectedRows()[0];
            int numSelected = entryTable.getSelectedRows().length;
            int lastIndex = entryTable.getSelectedRows()[numSelected - 1];
            changeEntryButton.setEnabled(numSelected == 1);
            deleteEntryButton.setEnabled(true);
            if (moreOptionsButton != null) {
                moreOptionsButton.setEnabled(true);
            }

            if (firstIndex > 0) {
                moveToTopButton.setEnabled(true);
                moveUpButton.setEnabled(true);
            } else {
                moveToTopButton.setEnabled(false);
                moveUpButton.setEnabled(false);
            }

            if (lastIndex < (entryTableModel.getRowCount() - 1)) {
                moveDownButton.setEnabled(true);
                moveToBottomButton.setEnabled(true);
            } else {
                moveDownButton.setEnabled(false);
                moveToBottomButton.setEnabled(false);
            }

            currentEntry = getSelectedEntry();
        }
    }

    /**
     * Enables or disables buttons depending on the selected entries.
     */
    private void updateSortButtons() {
        if (entryTable != null && entryTableModel.getRowCount() > 1 && !(cv instanceof ExternalCV)) {
            ascendingButton.setEnabled(true);
            descendingButton.setEnabled(true);
        }
    }

    /**
     * Updates the contents of the entry text fields based on the selected entry.
     */
    protected void updateTextFields() {
        CVEntry selEntry = getSelectedEntry();
        if (selEntry == null || selectedTableColumn < 0) {
            entryValueTextField.setText(EMPTY);
            entryDescTextField.setText(EMPTY);
        } else {
            //put the first selected entry into text fields
            entryValueTextField.setText(selEntry.getValue(selectedTableColumn));
            entryDescTextField.setText(selEntry.getDescription(selectedTableColumn));
        }

        if (entryValueTextField.isEnabled()) {
            entryValueTextField.requestFocus();
        }
    }

    /**
     * Returns the selected entry.
     *
     * @return the current selected entry or {@code null}
     */
    protected CVEntry getSelectedEntry() {
        return selectedTableEntry;
    }

    /**
     * Returns the index of the selected column.
     *
     * @return the index of the selected column
     */
    protected int getSelectedColumn() {
        return selectedTableColumn;
    }

    /**
     * Enables or disables the undo/redo buttons.
     */
    private void updateUndoRedoButtons() {
        undoButton.setEnabled(undoManager.canUndo());
        redoButton.setEnabled(undoManager.canRedo());
    }

    /**
     * A table model for the entries of a, possibly multilingual, CV. Cells are not editable in the table.
     */
    protected static class CVTableModel extends AbstractTableModel {
        /**
         * array of cv entries
         */
        protected CVEntry[] entries;
        /**
         * controlled vocabulary
         */
        protected BasicControlledVocabulary cv;

        /**
         * Creates a new table model instance.
         */
        public CVTableModel() {
            super();
        }

        /**
         * Setter method for cv entries
         *
         * @param entries the array of cv entries
         */
        public void setEntries(CVEntry[] entries) {
            this.entries = entries;
            // All rows changed
            fireTableDataChanged();
        }

        /**
         * Setter method for controlled vocabulary
         *
         * @param cv the controlled vocabulary
         */
        public void setControlledVocabulary(BasicControlledVocabulary cv) {
            this.cv = cv;
            // All columns changed
            fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));

        }

        /**
         * Returns the entry at the index.
         *
         * @param i the index
         *
         * @return the cv entry
         */
        public CVEntry getEntry(int i) {
            return entries[i];
        }

        @Override
        public String getColumnName(int col) {
            return cv.getLanguageId(col);
        }

        @Override
        public int getRowCount() {
            if (entries == null) {
                return 0;
            }
            return entries.length;
        }

        @Override
        public int getColumnCount() {
            if (cv == null) {
                return 0;
            }
            return cv.getNumberOfLanguages();
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (entries == null) {
                return "";
            }
            return entries[row].getValue(col);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        /*
         * If the table can find a TableCellRenderer for this class,
         * it won't use toString() on the values it gets from getValueAt()
         * when it renders them.
         */
        @Override
        public Class<?> getColumnClass(int col) {
            return CVEntry.class;
        }
    }
}
