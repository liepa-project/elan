package mpi.eudico.client.annotator.tier;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.Preferences;
import mpi.eudico.client.annotator.commands.Command;
import mpi.eudico.client.annotator.commands.ELANCommandFactory;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import mpi.eudico.client.annotator.gui.multistep.StepPane;
import mpi.eudico.client.util.RadioButtonCellEditor;
import mpi.eudico.client.util.RadioButtonTableCellRenderer;
import mpi.eudico.client.util.SelectEnableObject;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;

/**
 * Second and final step in the process of creating annotations on dependent
 * tiers.  
 */
@SuppressWarnings("serial")
public class CreateAnnsOnDependentTiersStep2 extends StepPane implements TableModelListener {
	private TranscriptionImpl transcription;
	
    private JTable tierTable;
    private DefaultTableModel model;   
    
    private JCheckBox overWriteCB;   
    
    private List<String> selectedParentTiers;    
    private List<String> emptyAnnTierList;
	private List<String> annWithValTierList;
    
       /** column id for the tier name column */
    private final String TIER_NAME_COLUMN = "Tiers";
    
    /** column id for the tier name column */
    private final String EMPTY_ANNOTATION_COLUMN = "Empty Annotations";
    
    /** column id for the tier name column */
    private final String ANNOTATION_WITH_VAL_COLUMN = "Annotation With Value of Parent";
    
    private static final int EMPTY_COL = 2;
    private static final int WITH_VAL_COL = 3;

    /**
     * Creates a new MergeStep2 instance.
     *
     * @param multiPane the enclosing MultiStepPane
     * @param trans the transcription containing the tiers
     */
    public CreateAnnsOnDependentTiersStep2(MultiStepPane multiPane, TranscriptionImpl trans) {
        super(multiPane);    
        emptyAnnTierList = new  ArrayList<String>();
    	annWithValTierList = new  ArrayList<String>();
        transcription = trans;
        initComponents();                
    }
    /**
     * Initializes the components of the step UI.
     */
    @Override
	public void initComponents() {        
        
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));   
        
        model = new TierExportTableModel();
        model.addTableModelListener(this);
        model.setColumnIdentifiers(new String[] { "", TIER_NAME_COLUMN, EMPTY_ANNOTATION_COLUMN, ANNOTATION_WITH_VAL_COLUMN });        
       
        tierTable = new TierExportTable(model, true);

        tierTable.getColumn(EMPTY_ANNOTATION_COLUMN).setCellEditor(new RadioButtonCellEditor(new JCheckBox()));
        tierTable.getColumn(EMPTY_ANNOTATION_COLUMN).setCellRenderer(new RadioButtonTableCellRenderer());
        tierTable.getColumn(EMPTY_ANNOTATION_COLUMN).setWidth(75);
       
        tierTable.getColumn(ANNOTATION_WITH_VAL_COLUMN).setCellEditor(new RadioButtonCellEditor(new JCheckBox()));
        tierTable.getColumn(ANNOTATION_WITH_VAL_COLUMN).setCellRenderer(new RadioButtonTableCellRenderer());
        tierTable.getColumn(ANNOTATION_WITH_VAL_COLUMN).setWidth(150);
        
        Insets insets = new Insets(2, 6, 2, 6);
       
        Dimension tableDim = new Dimension(450, 100);
        JScrollPane tierScrollPane = new JScrollPane(tierTable);
        tierScrollPane.setPreferredSize(tableDim);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = insets;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(tierScrollPane, gridBagConstraints);  
        
        overWriteCB = new JCheckBox(ElanLocale.getString("CreateAnnsOnDependentTiersDlg.Label.Overwrite"));
        overWriteCB.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = insets;
        add(overWriteCB, gridBagConstraints);  
    }
    
    
    /**
     * Extract all tiers and fill the table.
     */
    private void extractTiers() {
    	model.setRowCount(0);
        if (transcription != null) {        	
        	for(int i = 0; i < selectedParentTiers.size(); i++){
        		TierImpl t = transcription.getTierWithId(selectedParentTiers.get(i));   
        		List<TierImpl> v = t.getDependentTiers();
        		
        		for (int x = 0; x < v.size(); x++) {
        			 t = v.get(x); 
        			 SelectEnableObject<String> emptySEO = new SelectEnableObject<String>("", true , false);
        			 SelectEnableObject<String> withValSEO = new SelectEnableObject<String>("", false , false);
        			 model.addRow(new Object[] { Boolean.FALSE, t.getName(), emptySEO, withValSEO });
        			 /*
        			 Boolean checked;
        			 if (i == 0 && x == 0) { 
        				 checked = Boolean.TRUE;
        				 emptySEO.setEnabled(true);
        				 withValSEO.setEnabled(true);
                     } else{
                    	 checked = Boolean.FALSE;
                     }
        			 model.addRow(new Object[] { checked, t.getName(), emptySEO, withValSEO });
        			 */
        		}
        	}
        }                     	
    }    
    
    /**
     * Returns the tiers that have been selected in the table.
     *
     * @return a list of the selected tiers
     */
    @SuppressWarnings("unchecked")
	private void updateSelectedTierList() {      	
    	emptyAnnTierList.clear();
    	annWithValTierList.clear();
    	for (int i = 0; i < tierTable.getRowCount(); i++) {
    		Object selObj = model.getValueAt(i, TierExportTableModel.CHECK_COL);// Boolean

            if (Boolean.TRUE.equals(selObj)) {
            	SelectEnableObject<String> emptyAnnSEO = (SelectEnableObject<String>) model.getValueAt(i, EMPTY_COL);
            	String nameObj = (String)model.getValueAt(i, TierExportTableModel.NAME_COL);   
            	if (nameObj != null) {
            		if(emptyAnnSEO.isSelected()){
            			emptyAnnTierList.add(nameObj);
                    }else{
                    	annWithValTierList.add(nameObj);
                    }
            	}
            }
        }
    }

    @Override
	public String getStepTitle() {
        return ElanLocale.getString("CreateAnnsOnDependentTiersDlg.Title");
    }

    @SuppressWarnings("unchecked")
	@Override
	public void enterStepForward() {
    	selectedParentTiers = (List<String>) multiPane.getStepProperty("SelectedParentTiers");
    	extractTiers();

    	multiPane.setButtonEnabled(MultiStepPane.NEXT_BUTTON, false);
    	multiPane.setButtonEnabled(MultiStepPane.FINISH_BUTTON, true);
	    multiPane.setButtonEnabled(MultiStepPane.CANCEL_BUTTON, true);
	    multiPane.setButtonEnabled(MultiStepPane.PREVIOUS_BUTTON, true);  
    	loadPreferences();
    }

    /**
     * Notification that this step will become the active step, moving down.
     */
    @Override
	public void enterStepBackward() {
    }

    /**
     * Notification that this step will no longer be the active step, moving
     * up.
     */
    @Override
	public boolean leaveStepForward() {
        return true;
    }

    @Override
	public boolean leaveStepBackward() {
        multiPane.setButtonEnabled(MultiStepPane.FINISH_BUTTON, false);
        overWriteCB.setEnabled(false);
        return true;
    }

    /**
     * Store selected tiers, check the overwrite checkbox, create a command,
     * register as listener and activate the progress UI.
     */
    @Override
	public boolean doFinish() {
    	updateSelectedTierList();
    	savePreferences();
    	
        Object[] args = new Object[] { emptyAnnTierList, annWithValTierList, overWriteCB.isSelected() };
        Command command = ELANCommandFactory.createCommand(transcription,
                ELANCommandFactory.ANN_ON_DEPENDENT_TIER);

        command.execute(transcription, args);
        return true;
    }
    
    /**
     * Store preferences anyway?
     */
    @Override
	public void cancelled() {
//    	updateSelectedTierList();
//    	savePreferences();
	}
    
	/**
     * Notification that one of the checkboxes in the CHECK_COL column changed
     * via <code>model.setValue(...)</code>.
     * It does fortunately not fire for the manipulations here, since they don't
     * involve that method.
     */
	@Override
	public void tableChanged(TableModelEvent e) {
		int selectedRowIndex = e.getFirstRow();
		int selectedColumnIndex = e.getColumn();
		
		// Weed out some notifications while setting up the table
		if (e.getType() != TableModelEvent.UPDATE ||
				selectedRowIndex < 0 ||
				selectedColumnIndex == TableModelEvent.ALL_COLUMNS) {
			return;
		}
		
		if (selectedColumnIndex == TierExportTableModel.CHECK_COL) {		
			boolean enableRadioButtons = (Boolean)model.getValueAt(selectedRowIndex, TierExportTableModel.CHECK_COL);
			((SelectEnableObject<?>)model.getValueAt(selectedRowIndex,EMPTY_COL)).setEnabled(enableRadioButtons);
			((SelectEnableObject<?>)model.getValueAt(selectedRowIndex,WITH_VAL_COL)).setEnabled(enableRadioButtons);
		}
		
		if (model.getValueAt(selectedRowIndex, selectedColumnIndex) instanceof SelectEnableObject) {
			// Do manual mutual exclusion between the radio buttons
			SelectEnableObject<?> seo1 = (SelectEnableObject<?>) model.getValueAt(selectedRowIndex, selectedColumnIndex);
			if (seo1.isSelected()) {
				 SelectEnableObject<?> seo2 = (SelectEnableObject<?>) model.getValueAt(
						 selectedRowIndex, EMPTY_COL + WITH_VAL_COL - selectedColumnIndex);
				if (seo2 != null && seo2.isEnabled()) {
					seo2.setSelected(false);
				}
			} else {
				seo1.setSelected(true);
			}
		}
		
		updateSelectedTierList();
    	updateComps();
		
		// Notify the table to repaint, and process the new enable-states of
		// the cells afresh in the cell renderers, since the changes here don't
		// involve setValue()
		// (nor would we want to, since then we get recursive notifications).
		tierTable.repaint();
	}
	
	/**
	 * Update some buttons and check boxes.
	 */
	private void updateComps() {
		boolean enableFinish = !emptyAnnTierList.isEmpty() || !annWithValTierList.isEmpty();
		multiPane.setButtonEnabled(MultiStepPane.FINISH_BUTTON, enableFinish);
		boolean enableOverwrite = !annWithValTierList.isEmpty();
		overWriteCB.setEnabled(enableOverwrite);
	}
	
	private void savePreferences() {
		Preferences.set("CreateAnnsOnDependentTiers.SelectedDependTiers.Empty", emptyAnnTierList, null);
		Preferences.set("CreateAnnsOnDependentTiers.SelectedDependTiers.ParValue", annWithValTierList, null);
		Preferences.set("CreateAnnsOnDependentTiers.Overwrite", Boolean.valueOf(overWriteCB.isSelected()), null);
	}
	
	private void loadPreferences() {
		Boolean overWrPref = Preferences.getBool("CreateAnnsOnDependentTiers.Overwrite", null);
		if (overWrPref != null) {
			overWriteCB.setSelected(overWrPref.booleanValue());
		}
		List<String> depEmptyList = Preferences.getListOfString("CreateAnnsOnDependentTiers.SelectedDependTiers.Empty", null);
		List<String> depValueList = Preferences.getListOfString("CreateAnnsOnDependentTiers.SelectedDependTiers.ParValue", null);
		int selected = 0;
		model.removeTableModelListener(this);
		
		if (depEmptyList != null || depValueList != null) {		
	    	for (int i = 0; i < tierTable.getRowCount(); i++) {
	    		String nameObj = (String) model.getValueAt(i, TierExportTableModel.NAME_COL);
            	if (nameObj != null) {
            		boolean isEmptyAnn = (depEmptyList != null && depEmptyList.contains(nameObj));
            		boolean isWithVal  = (depValueList != null && depValueList.contains(nameObj));
            		
            		if (isEmptyAnn || isWithVal) {
            			selected++;
            			// set selected
            			model.setValueAt(Boolean.TRUE, i, TierExportTableModel.CHECK_COL);
            			SelectEnableObject<?> emptySEO = (SelectEnableObject<?>) model.getValueAt(i, EMPTY_COL);
            			emptySEO.setEnabled(true);
            			SelectEnableObject<?> valSEO = (SelectEnableObject<?>) model.getValueAt(i, WITH_VAL_COL);
            			valSEO.setEnabled(true);
            			
            			emptySEO.setSelected(isEmptyAnn);
            			valSEO.setSelected(!isEmptyAnn);
            		}
            	}           	
	    	}			
		}	
		
		if (selected == 0) {
			// set first row selected, the 'old' behavior
			if (model.getRowCount() > 0) {
				model.setValueAt(Boolean.TRUE, 0, TierExportTableModel.CHECK_COL);
    			SelectEnableObject<?> emptySEO = (SelectEnableObject<?>) model.getValueAt(0, EMPTY_COL);
    			emptySEO.setEnabled(true);
    			SelectEnableObject<?> valSEO = (SelectEnableObject<?>) model.getValueAt(0, WITH_VAL_COL);
    			valSEO.setEnabled(true);
			}
		}
		updateSelectedTierList();
		updateComps();
		tierTable.repaint();
		
		model.addTableModelListener(this);
	} // load preferences
	
}

