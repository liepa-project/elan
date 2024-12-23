package mpi.eudico.client.annotator.commands.global;

import mpi.eudico.client.annotator.ElanFrame2;
import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.export.multiplefiles.*;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Creates a dialog to export multiple files as {@code flextext}.
 *
 * @author Aarthy Somasundaram
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ExportFlexMultiMA extends FrameMenuAction {
    /**
     * Creates a new ExportPraatMultiMA instance
     *
     * @param name name of the action
     * @param frame the containing frame
     */
    public ExportFlexMultiMA(String name, ElanFrame2 frame) {
        super(name, frame);
    }

    /**
     * Creates a multistep dialog for praat export.
     *
     * @param e the event
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        MultiStepPane multipane = new MultiStepPane();
        multipane.addStep(new MultipleFileFlexExportStep1(multipane));
        multipane.addStep(new MultipleFileFlexExportStep2(multipane));
        multipane.addStep(new MultipleFileFlexExportStep3(multipane));
        multipane.addStep(new MultipleFileFlexExportStep4(multipane));
        multipane.addStep(new MultipleFileFlexExportStep5(multipane));

        JDialog dialog = multipane.createDialog(frame, ElanLocale.getString("ExportFlexDialog.Title"), true);
        dialog.setPreferredSize(new Dimension(600, 600));
        dialog.pack();
        dialog.setVisible(true);
    }
}
