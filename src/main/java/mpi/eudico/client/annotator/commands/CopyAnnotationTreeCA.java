package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ActiveAnnotationListener;
import mpi.eudico.client.annotator.ViewerManager2;

import javax.swing.*;

/**
 * A CommandAction to copy an annotation with its depending annotations (i.e. a transferable DefaultMutableTreeNode) to the
 * System's Clipboard.
 */
public class CopyAnnotationTreeCA extends CopyAnnotationCA implements ActiveAnnotationListener {

    /**
     * Constructor
     *
     * @param viewerManager the viewer manager
     */
    public CopyAnnotationTreeCA(ViewerManager2 viewerManager) {
        super(viewerManager, ELANCommandFactory.COPY_ANNOTATION_TREE);
        putValue(Action.NAME, ELANCommandFactory.COPY_ANNOTATION_TREE);
        viewerManager.connectListener(this);
        updateLocale();
    }

    @Override
    protected void newCommand() {
        command = ELANCommandFactory.createCommand(vm.getTranscription(), ELANCommandFactory.COPY_ANNOTATION_TREE);
    }

}
