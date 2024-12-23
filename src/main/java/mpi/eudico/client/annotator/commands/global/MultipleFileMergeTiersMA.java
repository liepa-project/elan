package mpi.eudico.client.annotator.commands.global;

import mpi.eudico.client.annotator.ElanFrame2;
import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import mpi.eudico.client.annotator.gui.multistep.StepPane;
import mpi.eudico.client.annotator.tier.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * A action that creates a dialog for merging tiers for multiple files
 *
 * @author aarsom
 * @version Feb 2014
 */
@SuppressWarnings("serial")
public class MultipleFileMergeTiersMA extends FrameMenuAction {

    /**
     * Constructor
     *
     * @param name, name of the action
     * @param frame, parent frame
     */
    public MultipleFileMergeTiersMA(String name, ElanFrame2 frame) {
        super(name, frame);
    }

    /**
     * Creates a dialog
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        MultiStepPane pane = new MultiStepPane(ElanLocale.getResourceBundle());
        StepPane step1 = new MergeMultiTiersStep1(pane, null);
        StepPane step2 = new MergeMultiTiersStep2(pane, null);
        StepPane step3 = new MergeMultiTiersStep3(pane, null);
        StepPane step4 = new MergeMultiTiersStep4(pane);
        StepPane step5 = new MergeMultiTiersStep5(pane, null);

        pane.addStep(step1);
        pane.addStep(step2);
        pane.addStep(step3);
        pane.addStep(step4);
        pane.addStep(step5);

        JDialog dialog = pane.createDialog(frame, ElanLocale.getString("MergeTiers.Title"), true);
        dialog.setPreferredSize(new Dimension(600, 600));
        dialog.pack();
        dialog.setVisible(true);
    }
}
