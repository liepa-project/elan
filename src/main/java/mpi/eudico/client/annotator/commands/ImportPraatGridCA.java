package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ViewerManager2;


/**
 * A command action that creates an import Praat TextGrid dialog.
 */
@SuppressWarnings("serial")
public class ImportPraatGridCA extends CommandAction {
    /**
     * Constructor.
     *
     * @param viewerManager the viewer manager
     */
    public ImportPraatGridCA(ViewerManager2 viewerManager) {
        super(viewerManager, ELANCommandFactory.IMPORT_PRAAT_GRID);
    }

    @Override
    protected void newCommand() {
        command = ELANCommandFactory.createCommand(vm.getTranscription(), ELANCommandFactory.IMPORT_PRAAT_GRID_DLG);
    }

    /**
     * Returns the receiver of the command
     *
     * @return the receiver
     */
    @Override
    protected Object getReceiver() {
        return vm.getTranscription();
    }
}
