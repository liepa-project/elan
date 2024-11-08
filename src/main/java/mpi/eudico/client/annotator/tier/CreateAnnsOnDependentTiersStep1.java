package mpi.eudico.client.annotator.tier;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.Preferences;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import mpi.eudico.client.annotator.gui.multistep.StepPane;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;

/**
 * First step of the process to create annotations on dependent tiers for
 * all annotations on the parent tier. 
 */
@SuppressWarnings("serial")
public class CreateAnnsOnDependentTiersStep1 extends StepPane implements TableModelListener {
	
	private TranscriptionImpl transcription;
	
	// ui elements
	
    private JTable tierTable;
    private TierExportTableModel model;  
    
    /**
     * Constructor.
     *
     * @param multiPane the enclosing MultiStepPane
     * @param trans a current transcription
     */
    public CreateAnnsOnDependentTiersStep1(MultiStepPane multiPane,
        TranscriptionImpl trans) {
        super(multiPane);          
        transcription = trans;
        initComponents();
        extractTiers();
        loadPreferences();
    }

    /**
     * Initializes UI components.
     */
    @Override
	public void initComponents() {
    	 setLayout(new GridBagLayout());
         setBorder(new EmptyBorder(12, 12, 12, 12));   
         
         model = new TierExportTableModel();
         tierTable = new TierExportTable(model);

         model.addTableModelListener(this);
        
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
    }
    
    /**
     * Extract all tiers and fill the table with tiers that have at least 1 child.
     */
    private void extractTiers() {
        if (transcription != null) {
            List<TierImpl> v = transcription.getTiers();
            TierImpl t;
           // boolean selectFirstTier = false;

            for (int i = 0; i < v.size(); i++) {
                t = v.get(i);                
                if (t.getChildTiers().size() > 0){
                	// selects the first tier in the list
//                	if (!selectFirstTier) {
//                		model.addRow(Boolean.TRUE, t.getName() );
//                		selectFirstTier = true;
//                	} else{
                		model.addRow(Boolean.FALSE, t.getName());
//                	}
                }
            }             
        }               
    }    
    
    /**
     * Returns the tiers that have been selected in the table.
     * <p>
     * Don't add a tier if its parent is already in the list.
     * Note that this isn't visible for the user!
     * I'm not sure if it is even intended behaviour.
     *
     * @return a list of the selected tiers
     */
    private List<String> getSelectedTiers() {
        List<String> tiers = new ArrayList<String>();
        
        for (int i = 0; i < model.getRowCount(); i++) {
        	Object selObj = model.getValueAt(i, 0);

            if (Boolean.TRUE.equals(selObj)) {
            	Object nameObj = model.getValueAt(i, 1);
                final String nameString = nameObj.toString();
				TierImpl t = transcription.getTierWithId(nameString);
                if (t != null) {         
                	if (t.hasParentTier()) {
                		if (!tiers.contains(t.getParentTier().getName())) {
							tiers.add(nameString);
						}
                	} else {
                		tiers.add(nameString);
                	}
                }
            }
        }

        return tiers;
    }

    @Override
	public String getStepTitle() {
        return ElanLocale.getString("CreateAnnsOnDependentTiersDlg.Title");
    }

    @Override
	public void enterStepForward() {
        // the next button is already disabled   
    	List<String> selectedTiers = getSelectedTiers();
    	if (!selectedTiers.isEmpty()) {
    		multiPane.setButtonEnabled(MultiStepPane.NEXT_BUTTON, true);
    	}
    }

    @Override
	public void enterStepBackward() {
        multiPane.setButtonEnabled(MultiStepPane.NEXT_BUTTON, true);
        multiPane.setButtonEnabled(MultiStepPane.CANCEL_BUTTON, true);
    }

    @Override
	public boolean leaveStepForward() {    	
    	multiPane.putStepProperty("SelectedParentTiers", getSelectedTiers());
    	savePreferences();
    	
        return true;
    }
    
	@Override // TableModelListener
	public void tableChanged(TableModelEvent e) {
		// Weed out some notifications while setting up the table
		if (e.getType() != TableModelEvent.UPDATE ||
				e.getFirstRow() < 0 ||
				e.getColumn() == TableModelEvent.ALL_COLUMNS) {
			return;
		}
		
 		List<String> selectedTiers = getSelectedTiers();
		multiPane.setButtonEnabled(MultiStepPane.NEXT_BUTTON, !selectedTiers.isEmpty());
	}
	
	private void savePreferences() {
		List<String> selTiers = getSelectedTiers();
		if (!selTiers.isEmpty()) {
			Preferences.set("CreateAnnsOnDependentTiers.SelectedParentTiers", selTiers, null);
		}
	}
	
	private void loadPreferences() {
		List<String> selTiers = Preferences.getListOfString("CreateAnnsOnDependentTiers.SelectedParentTiers", null);
		int selected = 0;
		if (selTiers != null && !selTiers.isEmpty()) {
	        for (int i = 0; i < model.getRowCount(); i++) {
	        	Object nameObj = model.getValueAt(i, 1);
	        	if (selTiers.contains(nameObj)) {
	        		model.setValueAt(Boolean.TRUE, i, 0);
	        		selected++;
	        	} 
	        }
		}
		
		if (selected == 0) {
			model.setValueAt(Boolean.TRUE, 0, 0);
		}
	}
}
