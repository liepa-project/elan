package mpi.eudico.client.annotator.export.multiplefiles;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.gui.FileChooser;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import nl.mpi.util.FileExtension;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for step 2: Export save as settings
 *
 * <p>Set the 'save as' setting for the files
 * that would be exported
 *
 * @author aarsom
 * @version Feb, 2012
 */
@SuppressWarnings("serial")
public class MultipleFileExportPraatStep2 extends AbstractMultiFileExportSaveSettingsStepPane {

    private JCheckBox correctTimesCB;
    private JComboBox selectEncodingCombo;

    /**
     * Constructor.
     *
     * @param multiStepPane the container for the step panes
     */
    public MultipleFileExportPraatStep2(MultiStepPane multiStepPane) {
        super(multiStepPane);
    }

    /**
     * Set the Praat preference strings
     */
    @Override
    protected void setPreferenceStrings() {
        saveWithOriginalNames = "MultiFileExportPraatDialog.saveWithOriginalNames";
        saveInOriginalFolder = "MultiFileExportPraatDialog.saveInOriginalFolder";
        saveInRelativeFolder = "MultiFileExportPraatDialog.saveInRelativeFolder";
        saveInRelativeFolderName = "MultiFileExportPraatDialog.saveInRelativeFolderName";
        saveInSameFolderName = "MultiFileExportPraatDialog.saveInSameFolderName";
        dontCreateEmptyFiles = "MultiFileExportPraatDialog.dontCreateEmptyFiles";

    }

    /**
     * @return the title for this step
     */
    @Override
    public String getStepTitle() {
        return ElanLocale.getString("MultiFileExportPraat.Title.Step2Title");
    }

    /**
     * @return the extension of the files to be exported
     */
    @Override
    protected String[] getExportExtensions() {
        return FileExtension.PRAAT_TEXTGRID_EXT;
    }

    /*
     * Other options panel
     */
    @Override
    protected void initOtherOptionsPanel() {
        super.initOtherOptionsPanel();

        correctTimesCB = new JCheckBox(ElanLocale.getString("ExportDialog.CorrectTimes"));

        //add table
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.insets = insets;
        otherOptionsPanel.add(correctTimesCB, gbc);


        selectEncodingCombo = new JComboBox();
        selectEncodingCombo.addItem(ElanLocale.getString("Button.Default"));
        selectEncodingCombo.addItem(FileChooser.UTF_8);
        selectEncodingCombo.addItem(FileChooser.UTF_16);

        JPanel encodingPanel = new JPanel(new GridLayout(1, 2));
        encodingPanel.add(new JLabel(ElanLocale.getString("FileChooser.Mac.Label.Encoding")));
        encodingPanel.add(selectEncodingCombo);

        gbc.gridy = 2;
        otherOptionsPanel.add(encodingPanel, gbc);
    }

    /**
     * Check and store properties, if all conditions are met.
     *
     * @see mpi.eudico.client.annotator.gui.multistep.Step#leaveStepForward()
     */
    @Override
    public boolean leaveStepForward() {

        // save settings
        multiPane.putStepProperty("CorrectTimes", correctTimesCB.isSelected());
        multiPane.putStepProperty("Encoding", selectEncodingCombo.getSelectedItem().toString());

        return super.leaveStepForward();
    }
}
