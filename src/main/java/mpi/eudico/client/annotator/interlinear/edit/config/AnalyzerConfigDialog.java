package mpi.eudico.client.annotator.interlinear.edit.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.Preferences;
import mpi.eudico.client.annotator.gui.ClosableDialog;
import mpi.eudico.client.annotator.interlinear.edit.PotentialTiers;
import mpi.eudico.client.annotator.interlinear.edit.TierTypeMapCreator;
import mpi.eudico.client.annotator.layout.InterlinearizationManager;
import mpi.eudico.server.corpora.clom.Tier;
import mpi.eudico.server.corpora.clomimpl.type.LinguisticType;
import nl.mpi.lexan.analyzers.helpers.Information;
import nl.mpi.lexan.analyzers.helpers.TierNodeType;
import nl.mpi.lexan.analyzers.helpers.parameters.Parameter;
import nl.mpi.lexan.analyzers.helpers.parameters.TierTypeParameter;


/**
 * A dialog to add configurations based on tier or type.
 * Invoked by the "Edit configurations..." button in the AnalyzerConfigPanel/Dialog.
 * <pre>
 * +----------------------------------------------------------+
 * |                   Analyzer configuration                 |
 * +----------------------------------------------------------+
 * |                                                          |
 * |  +--Select source and target as:----------------------+  |
 * |  |   [o] tiers    [o] linguistic types                |  |
 * |  +----------------------------------------------------+  |
 * |                                                          |
 * |  | No || Analyzer  || Source  || Target1  || Target2  |  |
 * |  -----++-----------++---------++----------++-----------  |
 * |  | 1  || Parse a...|| test    || sub1     || sub2     |  |
 * |  | 2  || Whitesp...|| bla     || blasub   ||XXXXXXXXXX|  |
 * |  | 3  || [select]  ||         ||          ||          |  |
 * |                                                          |
 * |                                                          |
 * |  [v] Overwrite existing configurations                   |
 * |                                                          |
 * |  [ Delete ]                       [ Apply ]  [ Cancel ]  |
 * |                                                          |
 * +----------------------------------------------------------+
 *
 * </pre>
 *
 * HS: Sept 2016 removed the option to configure per tier (for now).
 * The linking of tier type to lexical entry field could too easily
 * conflict with configurations here.
 *
 * @author aarsom
 *
 */
@SuppressWarnings("serial")
public class AnalyzerConfigDialog extends ClosableDialog implements
	ActionListener, ChangeListener {

	private static final String SELECT = ElanLocale.getString("InterlinearAnalyzerConfigDlg.ComboBoxDefaultString");
	private static final String NO_TIERS = ElanLocale.getString("InterlinearAnalyzerConfigDlg.NoSource");
	private static final String COLUMN_NO = ElanLocale.getString("TranscriptionTable.Column.No");

	private static final int TIER_MODE = 0;
	private static final int TYPE_MODE = 1;

	private InterlinearizationManager manager;

	private JRadioButton tierRB;
	private JRadioButton typeRB;
	//private JLabel infoLabel;

	private JTable configsTable;
	private DefaultTableModel configsModel;

	private JComboBox<String> annotSelCB;
	private JComboBox<String> sourceCB;
	private JComboBox<String> destinationCB;
	private JCheckBox listAllTypesChkB;

	private JButton deleteButton;
	private JButton applyButton;
    private JButton cancelButton;

    /** SourceName =&gt; targetList: map types to their direct child types,
     * meaning there is a pair of (parent, child) tiers that have these types. */
	private Map<String, List<String>> typesChildMap;

	/** SourceName =&gt; targetList: map tiers to their direct child tiers */
	private Map<String, List<String>> tiersChildMap;
	/** Maps type names to the tiers of that type */
	private Map<String, List<String>> typesToTiersMap;

	private int currentMode = -1;
	private boolean isApplied = false;

	private List<AnalyzerConfig> typeConfigMap;
	private List<AnalyzerConfig> tierConfigMap;

	/** Information about tiers names given as parameters, given an analyzer */
	private Map<String, PotentialTiers> potentialTiersMap;
	/** The potentialTiersMap is now always used in Type mode, this map is
	 * created and used for basic tier-level checks for producing
	 * information and or warning messages */
	private Map<String, PotentialTiers> tierCheckerMap;
	int maxTargetTiers = 1;
	/** For the current row */
	private PotentialTiers potentialTiers;
	private ArrayList<String> usedSources;

	private final static int NUMBER_INDEX = 0;
	private final static int ANALYZER_INDEX = 1;
	private final static int SOURCE_INDEX = 2;
	private final static int TARGET1_INDEX = 3;

	/**
	 * Constructor
	 *
	 * @param frame, the parent frame
	 * @param manager, the interlinearization manager, cannot be null
	 */
	public AnalyzerConfigDialog(Frame frame,
			InterlinearizationManager manager) {
        super(frame, ElanLocale.getString("InterlinearAnalyzerConfigDlg.Title"), true);

        this.manager = manager;
        currentMode = TYPE_MODE;

        initMaps();
        initComponents();
		postInit();
    }

	/**
	 * Constructor
	 *
	 * @param parent, the parent dialog
	 * @param manager, the interlinearization manager, cannot be null
	 */
	public AnalyzerConfigDialog(Dialog parent,
			InterlinearizationManager manager) {
        super(parent, ElanLocale.getString("InterlinearAnalyzerConfigDlg.Title"), true);

        this.manager = manager;
        currentMode = TYPE_MODE;

        initMaps();
        initComponents();
		postInit();
    }

	/**
	 * Creates some maps.
	 */
	private void initMaps() {
		typesChildMap   = new HashMap<String, List<String>>();
		tiersChildMap   = new HashMap<String, List<String>>();
		typesToTiersMap = new HashMap<String, List<String>>();

        typeConfigMap = new ArrayList<AnalyzerConfig>();
        tierConfigMap = new ArrayList<AnalyzerConfig>();

        potentialTiersMap = new HashMap<String, PotentialTiers>();
        tierCheckerMap    = new HashMap<String, PotentialTiers>();
	}

    /**
     * Pack, size and set location.
     */
    private void postInit() {
    	pack();
        setResizable(true);
        setLocationRelativeTo(getParent());
    }

    /**
     * Initializes the components for ui.
     */
    private void initComponents() {
		EmptyBorder marginBorder = new EmptyBorder(4, 6, 4, 6);
		TitledBorder titledBorder = new TitledBorder(ElanLocale.getString("InterlinearAnalyzerConfigDlg.Info"));
		((JComponent) getContentPane()).setBorder(new CompoundBorder(marginBorder, titledBorder));
    	//initialize the components
		tierRB = new JRadioButton(ElanLocale.getString("InterlinearAnalyzerConfigDlg.Tiers"));
		tierRB.addActionListener(this);

		typeRB = new JRadioButton(ElanLocale.getString("InterlinearAnalyzerConfigDlg.Types"));
		typeRB.addActionListener(this);

		sourceCB = new JComboBox<String>();
		destinationCB = new JComboBox<String>();
		annotSelCB = new JComboBox<String>();

		listAllTypesChkB = new JCheckBox("List all available types in the drop-down boxes, without filtering", false);

		//infoLabel = new JLabel(ElanLocale.getString("InterlinearAnalyzerConfigDlg.Info2"));
		JTextArea infoText = new JTextArea(ElanLocale.getString("InterlinearAnalyzerConfigDlg.Info2"));
		infoText.setEditable(false);
		infoText.setEnabled(false);
		infoText.setLineWrap(true);
		infoText.setWrapStyleWord(true);
		infoText.setOpaque(false);
		infoText.setFont(listAllTypesChkB.getFont());

		ButtonGroup group = new ButtonGroup();
		group.add(tierRB);
		group.add(typeRB);

		deleteButton = new JButton(ElanLocale.getString("Button.Delete"));
		deleteButton.addActionListener(this);

        applyButton = new JButton(ElanLocale.getString("Button.Apply"));
        applyButton.addActionListener(this);

        cancelButton = new JButton(ElanLocale.getString("Button.Cancel"));
        cancelButton.addActionListener(this);

        // get information for all analyzers
		if (manager != null) {
			List<Information> analyzersList = manager.getTextAnalyzerContext().listTextAnalyzersInfo();

			if (analyzersList != null && analyzersList.size() > 0) {
				for (Information i : analyzersList) {
					int nrTargetTiers = 0;
					String idOrName = i.getName();

					if (idOrName == null || idOrName.length() == 0) {
						// Log about the missing name
					} else {
						annotSelCB.addItem(idOrName);
						List<Parameter> parameters = i.getParameters();

						// Count how many target tiers this analyzer wants
						final PotentialTiers potentialTiers = new PotentialTiers(parameters);
						nrTargetTiers = potentialTiers.getNumberOfTargetTiers();

						maxTargetTiers = Math.max(maxTargetTiers, nrTargetTiers);

						potentialTiersMap.put(idOrName, potentialTiers);
						tierCheckerMap.put(idOrName, new PotentialTiers(parameters));
					}
				}
			} else {
				//display a warning message...  no analyzers are found
				JOptionPane.showMessageDialog(this, ElanLocale.getString("InterlinearAnalyzerConfigDlg.NoInfo"),
						ElanLocale.getString("Message.Warning"), JOptionPane.WARNING_MESSAGE);
				//disable components
			}
		}

		String[] columnIdentifiers = new String[TARGET1_INDEX + maxTargetTiers];
		columnIdentifiers[NUMBER_INDEX]   = COLUMN_NO;
		columnIdentifiers[ANALYZER_INDEX] = ElanLocale.getString("InterlinearAnalyzerConfigPanel.Analyzer");
		columnIdentifiers[SOURCE_INDEX]   = ElanLocale.getString("InterlinearAnalyzerConfigPanel.SourceTier");

		for (int i = 0; i < maxTargetTiers; i++) {
			columnIdentifiers[TARGET1_INDEX + i] = ElanLocale.getString("InterlinearAnalyzerConfigPanel.TargetTier") + (i+1);
		}

        // DefaultTableModel
		configsModel = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				if (getColumnName(column).equals(COLUMN_NO)) {
	 				return false;
	 			}

				Object value = getValueAt(row, column);

				if (value == null || value.equals(NO_TIERS)) {
					return false;
				} else {
					return true;
				}
			}
		};
		configsModel.setColumnIdentifiers(columnIdentifiers);
		configsModel.addTableModelListener(new TableModelChecker());

		configsTable = new JTable(configsModel) {
			@Override
			public void setEnabled(boolean enabled) {
				super.setEnabled(enabled);

				if (enabled) {
       	 			configsTable.setGridColor(Color.BLACK);
       	 		} else {
       	 			configsTable.setGridColor(Color.GRAY);
       	 		}
			}
		};

		//DefaultTableCellRenderer
        DefaultTableCellRenderer render = new DefaultTableCellRenderer() {
        	private Font italic = new Font(configsTable.getFont().getFontName(), Font.ITALIC, configsTable.getFont().getSize());
        	private Font plain = new Font(configsTable.getFont().getFontName(), Font.PLAIN, configsTable.getFont().getSize());

        	@Override
			public Component getTableCellRendererComponent(JTable table,
       			 Object value, boolean isSelected, boolean hasFocus, int viewRow,
       			 	int viewColumn) {

       	 		Component cell = super.getTableCellRendererComponent(table, value,
       				 isSelected, hasFocus, viewRow, viewColumn);

       	 		if (value == null) {
       	 			int modelColumn = table.convertColumnIndexToModel(viewColumn);

       	 			if (modelColumn > SOURCE_INDEX) {
           	 			int modelRow = table.convertRowIndexToModel(viewRow);
           	 			String analyzer = (String) configsModel.getValueAt(modelRow, ANALYZER_INDEX);

       	 				if (!analyzer.equals(SELECT)) {
       	 					//int targetTiers = numTargetTiersMap.get(analyzer);
       	 					int targetTiers = potentialTiersMap.get(analyzer).getNumberOfTargetTiers();

       	 					if (modelColumn > SOURCE_INDEX+targetTiers) {
       	 						cell.setBackground(Color.GRAY);

       	 						return cell;
       	 					}
       	 				}
       	 			}
       	 			cell.setBackground(Color.WHITE);

       	 			return cell;
       	 		}

       	 		cell.setBackground(Color.WHITE);

       	 		if (table.isEnabled()) {
       	 			cell.setForeground(Color.BLACK);
       	 		} else {
       	 			cell.setForeground(Color.GRAY);
       	 		}

       	 		if (value.equals(SELECT)) {
       	 			cell.setFont(italic);
       	 		} else {
       	 			cell.setFont(plain);
       	 		}

       	 		return cell;
       	 	}
        };

        configsTable.setCellSelectionEnabled(true);
        configsTable.setDefaultEditor(Object.class, new TableCellEditor());
        configsTable.setDefaultRenderer(Object.class, render);
        configsTable.setShowGrid(true);
        configsTable.setSelectionBackground(Color.WHITE);
        configsTable.setRowHeight(configsTable.getRowHeight()+ 5);
		configsTable.getColumnModel().getColumn(0).setMinWidth(50);
		configsTable.getColumnModel().getColumn(0).setMaxWidth(50);
		configsTable.setEnabled(false);

        setLayout(new GridBagLayout());
		Insets insets = new Insets (4, 6, 4, 6);

		GridBagConstraints gbc;

		// config Panel
		JPanel sourceTypePanel = new JPanel(new GridBagLayout());
		sourceTypePanel.setBorder(new TitledBorder(ElanLocale.getString("InterlinearAnalyzerConfigDlg.Mode")));
		gbc = new GridBagConstraints();
		gbc.insets = insets;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		sourceTypePanel.add(tierRB, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		sourceTypePanel.add(typeRB, gbc);

		//button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
	    gbc.gridx = 0;
	    gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
	    gbc.insets = insets;
	    gbc.weightx = 1.0;
	    buttonPanel.add(deleteButton, gbc);

	    gbc.gridx = 2;
	    gbc.weightx = 0.0;
	    gbc.anchor = GridBagConstraints.SOUTHEAST;
	    buttonPanel.add(applyButton, gbc);

	    gbc.gridx = 3;
	    buttonPanel.add(cancelButton, gbc);

		gbc = new GridBagConstraints();

		gbc.insets = new Insets (6, 6, 10, 6);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
//		getContentPane().add(sourceTypePanel, gbc);
		getContentPane().add(infoText, gbc);

		gbc.gridy = 1;
		gbc.insets = insets;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		gbc.weightx = 1.0;
		getContentPane().add(new JScrollPane(configsTable), gbc);

		gbc.gridy = 2;
		gbc.insets = insets;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0.0;
		gbc.weightx = 1.0;
		getContentPane().add(listAllTypesChkB, gbc);

		gbc.gridy = 3;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(buttonPanel, gbc);

 	    loadTierAndTypeMap();

		if (tiersChildMap.size() < 1) {
			tierRB.setEnabled(false);
			typeRB.setEnabled(false);
			// display a warning message and close the dialog...
			JOptionPane.showMessageDialog(this,
					ElanLocale.getString("InterlinearAnalyzerConfigDlg.NoTier"),
					ElanLocale.getString("Message.Warning"), JOptionPane.WARNING_MESSAGE);
			//disable components
		} else {
			List<AnalyzerConfig> list =
					manager.getTextAnalyzerContext().getConfigurations();

			for (AnalyzerConfig analyzerConfig : list) {
//				AConfig mappings = new AConfig(analyzerConfig.annotId.getName(),
//											   analyzerConfig.getSource(),
//											   analyzerConfig.getDest());
				// copy the AnalyzerConfig object?
				if (analyzerConfig.isTypeMode()) {
					typeConfigMap.add(analyzerConfig);
				} else {
					tierConfigMap.add(analyzerConfig);
				}
			}

			readPreferences();
		}
		listAllTypesChkB.addChangeListener(this);
    }

    /**
     * Stores the user preferences
     */
    private void storePreferences() {
    	//Preferences.set("AnnotationConfigurationDialog.TypeMode", typeRB.isSelected(), manager.getTranscription());
    	Preferences.set("AnalyzerConfigurationDialog.ListAllTierTypesMode", listAllTypesChkB.isSelected(), null);
    }

    /**
     * Reads and loads the user preferences
     */
    private void readPreferences() {
    	/*
    	Boolean boolPref = Preferences.getBool("AnnotationConfigurationDialog.TypeMode", manager.getTranscription());

    	if (boolPref != null && boolPref.booleanValue()) {
    		typeRB.setSelected(true);
            updateTableForMode(TYPE_MODE,false);
        } else {
           	tierRB.setSelected(true);
           	updateTableForMode(TIER_MODE, false);
    	}
    	*/
    	Boolean boolPref = Preferences.getBool("AnalyzerConfigurationDialog.ListAllTierTypesMode", null);

    	if (boolPref != null && boolPref.booleanValue()) {
    		listAllTypesChkB.setSelected(true);// default is false
    	}

    	updateTableForMode(TYPE_MODE,false);
    }

    @Override
	public void actionPerformed(ActionEvent e) {
    	Object source = e.getSource();

    	if (source == tierRB) {
    		if (currentMode != TIER_MODE) {
    			updateTableForMode(TIER_MODE, true);
    		}
    	} else if (source == typeRB) {
    		if (currentMode != TYPE_MODE) {
    			addConfigurations();
    			updateTableForMode(TYPE_MODE, true);
    		}
    	} else if (source == cancelButton) {
    		tierConfigMap.clear();
    		typeConfigMap.clear();
    		storePreferences();
    		doClose();
    	} else if (source == applyButton) {
    		isApplied = true;
    		addConfigurations();
    		storePreferences();
    		doClose();
    	} else if (source == deleteButton) {
    		int row = configsTable.getSelectedRow();
    		int rowcount = configsTable.getRowCount();

    		Object obj[] = new Object[TARGET1_INDEX + maxTargetTiers];
    		obj[NUMBER_INDEX]   = 0;
    		obj[ANALYZER_INDEX] = SELECT;
    		obj[SOURCE_INDEX]   = null;

    		for (int i = 0; i < maxTargetTiers; i++) {
    			obj[TARGET1_INDEX + i] = null;
    		}

    		if (row == (rowcount-1)) {
    			configsModel.removeRow(row);
    			configsModel.addRow(obj);
    			configsModel.setValueAt(configsTable.getRowCount(), configsTable.getRowCount()-1, 0);
    		} else if (row > -1) {
				int n = row + 1;

				while (n < rowcount) {
					configsModel.setValueAt(n, n, NUMBER_INDEX);
					n++;
				}

				configsModel.removeRow(row);

				if (configsModel.getRowCount() == 0) {
					configsModel.addRow(obj);
	    			configsModel.setValueAt(1, 0, NUMBER_INDEX);
	    		} else {
					String t = (String) configsModel.getValueAt(configsModel.getRowCount() - 1, ANALYZER_INDEX);

					if (t == null || t.equals(NO_TIERS)) {
						configsModel.setValueAt(SELECT, configsModel.getRowCount() - 1, ANALYZER_INDEX);
					} else if (!t.equals(SELECT)) {
						configsModel.addRow(obj);
		    			configsModel.setValueAt(configsTable.getRowCount(), configsTable.getRowCount() - 1, NUMBER_INDEX);
					}
				}

				((TableCellEditor)configsTable.getDefaultEditor(Object.class)).setLastUpdatedRow(-1);
			}
    	}
    }

    @Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == listAllTypesChkB) {
			// this forces an update of the table editor's item list
			configsTable.editingStopped(new ChangeEvent(this));
		}
	}

	/**
	 * Loads all tiers/types and their direct children in a map.
	 * <p>
	 * The special key "" maps to the collection of all tiers/types.
	 */
	private void loadTierAndTypeMap() {
		TierTypeMapCreator mc = new TierTypeMapCreator( manager.getTranscription());

		tiersChildMap   = mc.getTiersChildrenMap();
		typesChildMap   = mc.getTypesChildrenMap();
		typesToTiersMap = mc.getTypesToTiersMap();
	}

	/**
     * Add the configurations to a list.
     */
    private void addConfigurations() {
		List<AnalyzerConfig> configMap = getConfigurationMap();
		configMap.clear();

		// For each line in the table, collect the columns
		// (one source, the others as destination tier).
		// If the number of destination tiers corresponds to
		// what an analyzer requires, remember it as a configuration.
		final int rowCount = configsModel.getRowCount();

		for (int i = 0; i < rowCount; i++) {
			List<String> destList = new ArrayList<String>();
			String analyzerName = (String) configsModel.getValueAt(i, ANALYZER_INDEX);

			if (!analyzerName.equals(SELECT)) {
				String sourceName = (String) configsModel.getValueAt(i, SOURCE_INDEX);

				if (!sourceName.equals(NO_TIERS) && !sourceName.equals(SELECT)) {
					final int columnCount = configsModel.getColumnCount();

					for (int c = TARGET1_INDEX; c < columnCount; c++) {
						String destName = (String) configsModel.getValueAt(i, c);

						if (destName != null && !destName.equals(SELECT)) {
							destList.add(destName);
						}
					}

					if (potentialTiersMap.get(analyzerName).getNumberOfTargetTiers() ==
							destList.size()) {
						Information info = getInformation(analyzerName);
						if (info != null) {
							AnalyzerConfig config = new AnalyzerConfig(info, sourceName, destList);
							configMap.add(config);
						}
					}
				}
			}
		}
    }

    /**
     * Returns the configurations map.
     *
     * @return List&lt;AnalyzerConfig>, a list with all configurations of the current mode.
     */
    private List<AnalyzerConfig> getConfigurationMap() {
    	return getConfigurationMap(isTypeMode());
    }

    /**
     * Returns the configurations map of the given mode.
     * To get all configurations, call this method with both <code>false</code>
     * and <code>true</code>.
     *
     * @param isTypeMode if {@code true} the dialog is in tier type mode,
     * otherwise in tier mode
     * @return List&lt;AConfig&gt;, a list with all configurations of the given mode.
     */
    public List<AnalyzerConfig> getConfigurationMap(boolean isTypeMode) {
    	if (isTypeMode) {
			return typeConfigMap;
		} else {
			return tierConfigMap;
		}
    }

    /**
     * Check if the configuration is based on type mapping.
     *
     * @return true if type based
     *               else false (tier based)
     */
    public boolean isTypeMode() {
    	return currentMode != TIER_MODE;
    }

    /**
     * Returns whether changes are applied or canceled.
     *
     * @return whether the user clicked the Apply button.
     */
    public boolean isApplied() {
    	return isApplied;
    }

    /**
     * Updates the table when the mode is changed from/to tier/type mode
     *
     * @param mode (TIER_MODE / TYPE_MODE) new mode to be updated
     * @param saveCurrentMapping if {@code true} the current mappings are applied
     * and stored
     */
    private void updateTableForMode(int mode, boolean saveCurrentMapping) {
    	if (configsTable.isEditing()) {
    		configsTable.editingStopped(new ChangeEvent(this));
    	}

    	if (saveCurrentMapping) {
    		addConfigurations();
    	}

    	while (configsModel.getRowCount() > 0) {
    		configsModel.removeRow(configsModel.getRowCount() - 1);
    	}

		//update the table for the new mode
		configsTable.setEnabled(false);
		currentMode = mode;
		List<AnalyzerConfig> configList = getConfigurationMap();

		int n = 1;
		for (AnalyzerConfig config : configList) {
			Object[] rowEntry = new Object[TARGET1_INDEX + maxTargetTiers];

			rowEntry[NUMBER_INDEX]   = n++;
			rowEntry[ANALYZER_INDEX] = config.getAnnotId().getName();
			rowEntry[SOURCE_INDEX]   = config.getSource();

			List<String> targetList = config.getDest();
			final int numTargetTiers = targetList.size();

			for (int i = 0; i < maxTargetTiers; i++) {
				if (i < numTargetTiers) {
    				rowEntry[TARGET1_INDEX + i] = targetList.get(i);
    			} else {
    				rowEntry[TARGET1_INDEX + i] = null;
    			}
    		}

			configsModel.addRow(rowEntry);
		}

		configsModel.addRow(new Object[]{ n++, SELECT, null, null});

		configsTable.setEnabled(true);
    }

	/**
     * Closes this dialog
     */
    private void doClose() {
		setVisible(false);
		dispose();
	}

    /**
     * Sets up the PotentialTiers administration for a new/changed row.
     */
    private void setupPotentialTiers(int modelRow) {

    	String analyzerName = (String) configsModel.getValueAt(modelRow, ANALYZER_INDEX);
    	potentialTiers = potentialTiersMap.get(analyzerName);
    	potentialTiers.setMaps(typesChildMap, tiersChildMap, typesToTiersMap, isTypeMode());
    	tierCheckerMap.get(analyzerName).setMaps(typesChildMap, tiersChildMap, typesToTiersMap, isTypeMode());
    	// Collect which tiers are already used as source.
    	// Assumes 1 source for each analyzer, as everywhere in the GUI.
    	usedSources = new ArrayList<String>();

    	final int rowCount = configsModel.getRowCount();
		for (int i = 0; i < rowCount; i++) {
    		if (i == modelRow) {
    			continue;
    		}
    		String sourceName = (String) configsModel.getValueAt(i, SOURCE_INDEX);

    		if (!usedSources.contains(sourceName)) {
    			usedSources.add(sourceName);
    		}
    	}
    }

    /**
     *
     * @param analyzerName the name of the analyzer to look for
     * @return the {@link Information} object of the analyzer of the specified
     * name or {@code null} if not there
     */
    private Information getInformation(String analyzerName) {
		List<Information> analyzersList = manager.getTextAnalyzerContext().listTextAnalyzersInfo();

		if (analyzersList != null && analyzersList.size() > 0) {
	    	for (Information infor : analyzersList) {
	    		if (infor.getName() != null && infor.getName().equals(analyzerName)) {
	    			return infor;
	    		}
	    	}
		}
    	return null;
    }

	/**
     * Cell Editor for the selection JTable
     *
     * @author aarsom
     */
    private class TableCellEditor extends DefaultCellEditor implements ActionListener {
    	private int startEditInOneClick = 1;
    	//private int viewColumn;  // in JTable coordinates
    	private int viewRow;
    	private int lastUpdatedRow = -1;
    	private int modelColumn; // in Model coordinates

    	public TableCellEditor() {
    		super(new JComboBox<String>());
    		setClickCountToStart(startEditInOneClick);
    	}

    	public void setLastUpdatedRow(int row) {
    		lastUpdatedRow = row;
    	}

    	@Override
		public Component getTableCellEditorComponent(
    			JTable table,
    			Object value,
    			boolean isSelected,
    			int viewRow, // row and column in JTable coordinates
    			int viewColumn) {

    		//this.viewColumn = viewColumn;
    		this.viewRow = viewRow;
    		this.modelColumn = table.convertColumnIndexToModel(viewColumn);
    		boolean allTierTypeMode = listAllTypesChkB.isSelected();

    		if (modelColumn == ANALYZER_INDEX) {
    			annotSelCB.removeActionListener(this);
    			if (value.equals(SELECT)) {
    				annotSelCB.setSelectedItem(null);
    			} else {
    				annotSelCB.setSelectedItem(value);
    			}
    			annotSelCB.addActionListener(this);
    			potentialTiers = null;
    			return annotSelCB;
    		}

    		if (lastUpdatedRow != viewRow || potentialTiers == null) {
    			setupPotentialTiers(table.convertRowIndexToModel(viewRow));
    			lastUpdatedRow = viewRow;
    		}

    		if (modelColumn == SOURCE_INDEX) {
    			sourceCB.removeActionListener(this);
    			sourceCB.removeAllItems();

    			/*
    			 * For now, don't use the usedSources list. (Maybe stop creating it completely.)
    			 * With our new 'policy' of creating small analyzers that do simple steps, to be
    			 * combined by recursion, it makes sense to use one tier as source for multiple
    			 * analyzers.
    			 */
        		potentialTiers.setTierName(modelColumn - SOURCE_INDEX, "");
    			List<String> sources = allTierTypeMode ? potentialTiers.getAllNames() :
    				potentialTiers.getPotentialSourceNames(modelColumn-SOURCE_INDEX,
    					/*usedSources*/
    					Collections.<String>emptyList()
    					);

    			if (sources.isEmpty()) {
					table.setValueAt(NO_TIERS, viewRow, viewColumn);
					return null;
				}

    			for (String s : sources) {
					sourceCB.addItem(s);
				}

    			if (value.equals(SELECT)) {
    				sourceCB.setSelectedItem(null);
    			} else {
    				sourceCB.setSelectedItem(value);
    			}
    			sourceCB.addActionListener(this);
    			return sourceCB;
			}

    		// Fill an editor for the Target1 or Target2 column
    		destinationCB.removeActionListener(this);
    		destinationCB.removeAllItems();

    		potentialTiers.setTierName(modelColumn - SOURCE_INDEX, "");
    		List<String> destList = allTierTypeMode ? potentialTiers.getAllNames() :
    			potentialTiers.getPotentialTargetNames(modelColumn - SOURCE_INDEX);

    		// null pointer exception
    		for (String n : destList) {
    			destinationCB.addItem(n);
    		}

    		if (value.equals(SELECT)) {
    			destinationCB.setSelectedItem(null);
    		} else {
    			destinationCB.setSelectedItem(value);
    		}

    		destinationCB.addActionListener(this);

    		return destinationCB;
    	}

    	@Override
		public Object getCellEditorValue() {
    		Object editorValue = null;

    		//int modelColumn = configsTable.convertColumnIndexToModel(viewColumn);

    		if (modelColumn == ANALYZER_INDEX) {
    			editorValue =  annotSelCB.getSelectedItem();
    		} else if (modelColumn == SOURCE_INDEX) {
    			editorValue =  sourceCB.getSelectedItem();
    		} else {
    			editorValue = destinationCB.getSelectedItem();
    		}

    		if (editorValue == null) {
    			return SELECT;
    		}

    		return editorValue;
    	}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!(e.getSource() instanceof JComboBox)) {
				return;
			}

    		//int modelColumn = configsTable.convertColumnIndexToModel(viewColumn);
			int modelRow = configsTable.convertRowIndexToModel(viewRow);

			Object selectedItem = getCellEditorValue() ;

			if (modelColumn == ANALYZER_INDEX) {
				lastUpdatedRow = -1;
			} else if (modelColumn >= SOURCE_INDEX && potentialTiers != null) {
				potentialTiers.setTierName(modelColumn - SOURCE_INDEX, selectedItem.toString());
			}

			if (selectedItem.toString().equals(configsModel.getValueAt(modelRow, modelColumn))) {
				configsTable.editingStopped(new ChangeEvent(this));
				return ;
			}

			configsTable.editingStopped(new ChangeEvent(this));

			int nextModelColumn = modelColumn+1;

			if (selectedItem.toString().equals(SELECT)) {
				while (nextModelColumn <= configsModel.getColumnCount()-1) {
					configsModel.setValueAt(null, modelRow, nextModelColumn);
					nextModelColumn++;
				}
				return;
			}

			// If last row/column, then add new row.
			// Don't let the user change this by changing the order of the columns.
			final int numTargetTiers =
					potentialTiers == null ? 0
					                       : potentialTiers.getNumberOfTargetTiers();

			if ((modelColumn == numTargetTiers+SOURCE_INDEX ||
					modelColumn == (configsModel.getColumnCount() - 1)) &&
					modelRow == (configsModel.getRowCount() -1)) {
				Object obj[] = new Object[TARGET1_INDEX + maxTargetTiers];
				obj[NUMBER_INDEX]   = configsModel.getRowCount()+1;
				obj[ANALYZER_INDEX] = SELECT;
				obj[SOURCE_INDEX]   = null;
	    		for (int i = 0; i < maxTargetTiers; i++) {
	    			obj[TARGET1_INDEX + i] = null;
	    		}
	    		configsModel.addRow(obj);
	    		return;
			}

			if (modelColumn == numTargetTiers+SOURCE_INDEX) {
				return;
			}

			while (nextModelColumn <= configsModel.getColumnCount()-1) {
				if (nextModelColumn == modelColumn + 1) {
					configsModel.setValueAt(SELECT, modelRow, nextModelColumn);
				} else {
					configsModel.setValueAt(null, modelRow, nextModelColumn);
				}

				nextModelColumn++;
			}
		}
    }

//### table model events handling
    /**
     * A TableModelListener that invokes a check on settings in the editing row
     * in order to show a warning or information message to the user.
     */
    class TableModelChecker implements TableModelListener, Runnable {
    	private int rowToCheck = -1;

		@Override
		public void run() {
			if (rowToCheck > -1) {
				checkRow(rowToCheck);
			}
		}

		@Override
		public void tableChanged(TableModelEvent e) {
			//  check the row and if filled completely, check the settings
			if (e.getType() == TableModelEvent.UPDATE &&
					e.getColumn() > 0) {
				//checkRow(e.getFirstRow());
				rowToCheck = e.getFirstRow();
				EventQueue.invokeLater(this);
			}
		}

    }

    /**
     * Note: this method assumes the table and model are in "Tier Type" mode,
     * not in "Tier" mode.
     * @param row the row index to check
     */
	private void checkRow(int row) {
		if (row < configsModel.getRowCount()) {
			for (int i = configsModel.getColumnCount() - 1; i > 1; i--) {
				Object value = configsModel.getValueAt(row, i);
				if (value == SELECT) {
					return;
				}
				if (value == null && i <= SOURCE_INDEX) {
					return;
				}
			}
			// check if there is at least one non-null target
			boolean hasTarget = false;
			for (int i = configsModel.getColumnCount() - 1; i > SOURCE_INDEX; i--) {
				Object value = configsModel.getValueAt(row, i);
				if (value != null && value != SELECT) {
					hasTarget = true;
					break;
				}
			}

			if (!hasTarget) {
				return;
			}
			// check more thoroughly now on all values in the row
			StringBuilder issueList = new StringBuilder();
			// get the analyzer, check lexicon access
			String anName = (String) configsModel.getValueAt(row, ANALYZER_INDEX);
			Information anInfo = getInformation(anName);
			// extract the requirements from the information parameters
			List<Parameter> params = anInfo.getParameters();
			// get source type, check lexicon connection, check whether there are tiers of that type
			String sourceTierType = (String) configsModel.getValueAt(row, SOURCE_INDEX);
			LinguisticType sourceLT = null;
			int numSrcTiers = 0;
			List<? extends Tier> sourceTierList = null;
			PotentialTiers tierChecker = tierCheckerMap.get(anName);

			if (sourceTierType != null) {
				tierChecker.setTierName(0, sourceTierType);
				sourceLT = manager.getTranscription().getLinguisticTypeByName(sourceTierType);
				sourceTierList = manager.getTranscription().getTiersWithLinguisticType(sourceTierType);
				numSrcTiers = sourceTierList.size();
				if (numSrcTiers == 0) {
					issueList.append(String.format(ElanLocale.getString(
							"InterlinearAnalyzerConfigDlg.IssueMessage.NoTier1"), sourceTierType));
					issueList.append("\n\n");
				}
				if (params != null) {
					for (Parameter p : params) {
						if (p instanceof TierTypeParameter) {
							TierTypeParameter ttp = (TierTypeParameter) p;

							if (ttp.getValue() == TierNodeType.SOURCE) {
								if (ttp.getRequiresLexiconLink() && sourceLT.getLexiconQueryBundle() == null) {
									issueList.append(ElanLocale.getString("InterlinearAnalyzerConfigDlg.IssueMessage.Lexicon1"));
									issueList.append(" ");
									issueList.append(String.format(ElanLocale.getString("InterlinearAnalyzerConfigDlg.IssueMessage.Lexicon2"),
											sourceTierType, ElanLocale.getString("Menu.Type.ChangeType")));
									issueList.append("\n\n");
								}

								break;
							}

						}
					}
				}
			}

			// get target type(s), check lexicon connection, check available tiers
			for (int col = TARGET1_INDEX; col < configsModel.getColumnCount(); col++) {
				String targetTierType = (String) configsModel.getValueAt(row, col);
				LinguisticType tarLT = null;
				int targetTypeIndex = col - TARGET1_INDEX + 1;// the n-th target type, 1-based, in practice first or second
				int numTarTiers = 0;
				List<? extends Tier> targetTierList = null;
				if (targetTierType != null) {
					tarLT = manager.getTranscription().getLinguisticTypeByName(targetTierType);
					targetTierList = manager.getTranscription().getTiersWithLinguisticType(targetTierType);
					numTarTiers = targetTierList.size();

					if (numTarTiers == 0) {
						issueList.append(String.format(ElanLocale.getString("InterlinearAnalyzerConfigDlg.IssueMessage.NoTier2"),
								targetTierType, targetTypeIndex));
						issueList.append("\n\n");
					}

					if (params != null) {
						int targetParamIndex = 0;// the n-th target parameter
						for (Parameter p : params) {
							if (p instanceof TierTypeParameter) {
								TierTypeParameter ttp = (TierTypeParameter) p;

								if (ttp.getValue() == TierNodeType.TARGET) {
									targetParamIndex++;
									// check the index and/or the relativeTo field of the parameter to know when to output target info
									if (targetTypeIndex == targetParamIndex) {
										if (ttp.getRequiresLexiconLink() && tarLT.getLexiconQueryBundle() == null) {
											issueList.append(ElanLocale.getString("InterlinearAnalyzerConfigDlg.IssueMessage.Lexicon3"));
											issueList.append(" ");
											issueList.append(String.format(ElanLocale.getString("InterlinearAnalyzerConfigDlg.IssueMessage.Lexicon4"),
													targetTierType, targetTypeIndex, ElanLocale.getString("Menu.Type.ChangeType")));
											issueList.append("\n\n");
										}

										if (listAllTypesChkB.isSelected()) {
											if (ttp.getConstraint() == TierTypeParameter.NO_CONSTRAINT) {
												break;
											} else {
												if (targetParamIndex <= tierChecker.getNumberOfTargetTiers()) {
													tierChecker.setTierName(targetTypeIndex, targetTierType);
													// check all combinations of tiers corresponding to the column left of the "relative-to" value
													List<String> checkedTargetNames = tierChecker.getPotentialTargetTierNames(targetTypeIndex);

													// report - no target
													if (checkedTargetNames.isEmpty()) {
														issueList.append(String.format(ElanLocale.getString(
																"InterlinearAnalyzerConfigDlg.IssueMessage.NoTier3"), targetTierType));
													}
												}
											}
										}

										break;
									}
								}

							}
						}
					}

				} else if (col == TARGET1_INDEX) {
					// missing target type
				}
			}

			if (issueList.length() != 0) {
				//create a message dialog or window
				JTextArea textArea = new JTextArea();
				textArea.setText(String.format(ElanLocale.getString(
						"InterlinearAnalyzerConfigDlg.IssueMessage.Intro"), anName, row + 1));
				textArea.append("\n\n");
				textArea.append(issueList.toString());
				textArea.setWrapStyleWord(true);
				textArea.setLineWrap(true);
				textArea.setFont(configsTable.getFont());
				JScrollPane scrollPane = new JScrollPane(textArea);
				scrollPane.setPreferredSize(new Dimension((int) ((this.getSize().width / 3f) * 2),
						(int) (this.getHeight() / 2f)));
				JOptionPane.showMessageDialog(AnalyzerConfigDialog.this, scrollPane,
						ElanLocale.getString("Message.Info"), JOptionPane.INFORMATION_MESSAGE);

			}
		}
	}
}
