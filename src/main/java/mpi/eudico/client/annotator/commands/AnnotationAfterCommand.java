package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ElanMediaPlayerController;
import mpi.eudico.client.annotator.Preferences;
import mpi.eudico.client.annotator.ViewerManager2;
import mpi.eudico.client.annotator.util.AnnotationDataRecord;
import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clom.Transcription;
import mpi.eudico.server.corpora.clomimpl.abstr.AlignableAnnotation;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;

import java.util.Optional;


/**
 * A command for creating a new annotation after a given annotation.
 *
 * @author Han Sloetjes
 */
public class AnnotationAfterCommand implements UndoableCommand {
    private final String commandName;
    private TierImpl tier;
    private Transcription transcription;
    private AnnotationDataRecord activeAnnRecord;
    private long aaMiddle;
    private long newAnnMiddle;

    /**
     * Creates a new AnnotationAfterCommand instance
     *
     * @param name the name of the command
     */
    public AnnotationAfterCommand(String name) {
        commandName = name;
    }

    /**
     * The undo action.
     */
    @Override
    public void undo() {
        if (tier != null) {
            Annotation delAnn = tier.getAnnotationAtTime(newAnnMiddle);

            if (delAnn != null) {
                tier.removeAnnotation(delAnn);
            }

            if (tier.isTimeAlignable()) {
                AlignableAnnotation activeAnn = (AlignableAnnotation) (tier.getAnnotationAtTime(aaMiddle));

                if ((activeAnn != null) && ((activeAnn.getBegin().isTimeAligned() != activeAnnRecord.isBeginTimeAligned())
                                            || (activeAnn.getEnd().isTimeAligned() != activeAnnRecord.isEndTimeAligned()))) {
                    int curPropMode = 0;

                    curPropMode = transcription.getTimeChangePropagationMode();

                    if (curPropMode != Transcription.NORMAL) {
                        transcription.setTimeChangePropagationMode(Transcription.NORMAL);
                    }

                    activeAnn.updateTimeInterval(activeAnnRecord.getBeginTime(), activeAnnRecord.getEndTime());

                    // restore the time propagation mode
                    transcription.setTimeChangePropagationMode(curPropMode);
                }
            }
        }
    }

    /**
     * The redo action.
     */
    @Override
    public void redo() {
        if (tier != null) {
            Annotation afterAnn = tier.getAnnotationAtTime(aaMiddle);

            if (afterAnn != null) {
                tier.createAnnotationAfter(afterAnn);
            }
        }
    }

    /**
     * <b>Note: </b>it is assumed the types and order of the arguments are
     * correct.
     *
     * @param receiver the TierImpl
     * @param arguments the arguments:  <ul><li>arg[0] = the active annotation (Annotation)</li> </ul>
     */
    @Override
    public void execute(Object receiver, Object[] arguments) {
        tier = (TierImpl) receiver;

        Annotation activeAnn = (Annotation) arguments[0];
        activeAnnRecord = new AnnotationDataRecord(activeAnn);
        aaMiddle = (activeAnnRecord.getBeginTime() + activeAnnRecord.getEndTime()) / 2;

        transcription = tier.getTranscription();

        Annotation newAnnotation = tier.createAnnotationAfter(activeAnn);

        if (newAnnotation != null) {
            newAnnMiddle = (newAnnotation.getBeginTimeBoundary() + newAnnotation.getEndTimeBoundary()) / 2;

            boolean boolPref = Optional.ofNullable(Preferences.getBool("ClearSelectionAfterCreation", null))
                                       .orElse(false);

            if (boolPref) {
                ViewerManager2 viewerManager = ELANCommandFactory.getViewerManager(transcription);
                ElanMediaPlayerController mediaPlayerController = viewerManager.getMediaPlayerController();
                if (mediaPlayerController.getSelectionMode()) {
                    long mediaTime = mediaPlayerController.getMediaTime();
                    viewerManager.getSelection().setSelection(mediaTime, mediaTime);
                } else {
                    viewerManager.getSelection().setSelection(0, 0);
                }

            }
        }
    }

    @Override
    public String getName() {
        return commandName;
    }
}
