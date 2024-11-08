package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import mpi.eudico.client.annotator.gui.multistep.StepPane;
import mpi.eudico.client.annotator.tier.MergeTiersStep1;
import mpi.eudico.client.annotator.tier.MergeTiersStep2;
import mpi.eudico.client.annotator.tier.MergeTiersStep3;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;

import javax.swing.*;
import java.awt.*;


/**
 * Creates the "annotations from overlaps wizard".
 */
public class MergeTiersClasDlgCommand implements Command {
    private final String commandName;

    /**
     * Constructor.
     *
     * @param name the name of the command
     */
    public MergeTiersClasDlgCommand(String name) {
        commandName = name;
    }

    /**
     * Creates the "merge tiers wizard".
     *
     * @param receiver the transcription
     * @param arguments null
     *
     * @see mpi.eudico.client.annotator.commands.Command#execute(java.lang.Object, java.lang.Object[])
     */
    @Override
    public void execute(Object receiver, Object[] arguments) {
        TranscriptionImpl trans = (TranscriptionImpl) receiver;

        MultiStepPane pane = new MultiStepPane(ElanLocale.getResourceBundle());
        StepPane step1 = new MergeTiersStep1(pane, trans);
        StepPane step2 = new MergeTiersStep2(pane, trans);
        StepPane step3 = new MergeTiersStep3(pane, trans);

        pane.addStep(step1);
        pane.addStep(step2);
        pane.addStep(step3);

        JDialog dialog =
            pane.createDialog(ELANCommandFactory.getRootFrame(trans), ElanLocale.getString("Menu.Tier.MergeTiers"), true);
        dialog.pack();
        dialog.setSize(new Dimension(dialog.getSize().width, dialog.getSize().height + 100));
        dialog.setVisible(true);
    }

    @Override
    public String getName() {
        return commandName;
    }
}
