package mpi.eudico.client.annotator.commands.global;

import mpi.eudico.client.annotator.ElanFrame2;
import mpi.eudico.client.annotator.spellcheck.EditSpellCheckerDialog;

import java.awt.event.ActionEvent;

/**
 * A menu action that creates and shows the Edit Spell Checker dialog.
 *
 * @author Han Sloetjes
 * @version 1.0
 */
@SuppressWarnings("serial")
public class EditSpellCheckerMA extends FrameMenuAction {
    /**
     * Creates a new EditSpellCheckerMA instance
     *
     * @param name the name of the command
     * @param frame the associated frame
     */
    public EditSpellCheckerMA(String name, ElanFrame2 frame) {
        super(name, frame);
    }

    /**
     * Shows the dialog.
     *
     * @param e the event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        new EditSpellCheckerDialog(frame, true).setVisible(true);
    }
}
