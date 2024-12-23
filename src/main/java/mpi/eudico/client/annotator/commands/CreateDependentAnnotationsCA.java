package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ActiveAnnotationListener;
import mpi.eudico.client.annotator.ViewerManager2;
import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clom.Tier;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;

import java.util.List;

/**
 * Creates child annotations on all the depending tiers of the selected/active annotation
 *
 * @author aarsom
 */
@SuppressWarnings("serial")
public class CreateDependentAnnotationsCA extends CommandAction implements ActiveAnnotationListener {
    private Tier receiver;
    private long beginTime;
    private long endTime;

    /**
     * Creates a new CreateDependentAnnotationsCA instance
     *
     * @param viewerManager the viewer manager
     */
    public CreateDependentAnnotationsCA(ViewerManager2 viewerManager) {
        super(viewerManager, ELANCommandFactory.CREATE_DEPEND_ANN);
        viewerManager.connectListener(this);
        setEnabled(false);
    }

    /**
     * Before just creating a command check for the active annotation and if so, on which tier. If receiver is
     * <code>null</code> no command is created (since the command should be undoable we don't want to check in the command
     * itself).
     */
    @Override
    protected void newCommand() {
        command = null;
        if (checkState()) {
            //doublecheck
            if (receiver != null) {
                command = ELANCommandFactory.createCommand(vm.getTranscription(), ELANCommandFactory.CREATE_DEPEND_ANN);
            }
        }
    }

    /**
     * The receiver of this CommandAction is a TierImpl object. This can be either the active tier or the tier the active
     * annotation is on.
     *
     * @return the receiver, a tier
     */
    @Override
    protected Object getReceiver() {
        return receiver;
    }

    /**
     * @return an array containing begin and end time
     */
    @Override
    protected Object[] getArguments() {
        Object[] args = new Object[2];
        args[0] = Long.valueOf(beginTime);
        args[1] = Long.valueOf(endTime);

        return args;
    }

    /**
     * Checks for a active annotation and dependent tiers.
     *
     * @return {@code true} if there is an active annotation and if the tier it is on has dependent tiers
     */
    protected boolean checkState() {
        //setEnabled(false);
        receiver = null;
        beginTime = 0;
        endTime = 0;

        if ((vm.getActiveAnnotation() != null) && (vm.getActiveAnnotation().getAnnotation() != null)) {
            Annotation activeAnn = vm.getActiveAnnotation().getAnnotation();
            receiver = activeAnn.getTier();
            List<TierImpl> depTiers = ((TierImpl) activeAnn.getTier()).getDependentTiers();

            if ((depTiers == null) || depTiers.isEmpty()) {
                return false;
            }

            beginTime = activeAnn.getBeginTimeBoundary();
            endTime = activeAnn.getEndTimeBoundary();
            return true;
        }
        return false;
    }

    @Override
    public void updateActiveAnnotation() {
        setEnabled(vm.getActiveAnnotation().getAnnotation() != null);
    }
}
