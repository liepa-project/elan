package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.Constants;
import mpi.eudico.client.annotator.util.AnnotationDataRecord;
import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clom.Transcription;
import mpi.eudico.server.corpora.clomimpl.abstr.AbstractAnnotation;
import mpi.eudico.server.corpora.clomimpl.abstr.AlignableAnnotation;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;
import mpi.eudico.server.corpora.clomimpl.type.LinguisticType;
import mpi.eudico.util.TimeFormatter;

import java.util.ArrayList;
import java.util.List;

import static mpi.eudico.client.annotator.util.ClientLogger.LOG;

/**
 * A command that creates annotations based on the gaps between annotations on a given tier. The new annotations can either
 * be inserted on the same tier or created on an other tier.
 *
 * @author Han Sloetjes
 */
public class AnnotationsFromGapsCommand implements UndoableCommand {
    private final String commandName;
    private TranscriptionImpl transcription;
    private long mediaDuration;
    private String[] sourceTiers;
    private String destTierName;
    private String annValue;
    private String timeFormat;
    private boolean destTierCreated = false;
    private List<AnnotationDataRecord> createdAnnos;

    /**
     * Constructor.
     *
     * @param name the name of the command
     */
    public AnnotationsFromGapsCommand(String name) {
        commandName = name;
    }

    /**
     * Recreates the new tier (if applicable) and the newly created annotations.
     */
    @Override
    public void redo() {
        if (transcription != null) {
            TierImpl dt = transcription.getTierWithId(destTierName);

            if (dt == null) {
                dt = transcription.getTierWithId(sourceTiers[0]);

                LinguisticType linguisticType = dt.getLinguisticType();

                if (linguisticType.getConstraints() != null) {
                    // the source tier should be a toplevel tier
                    LOG.severe("The source tier is not a root tier.");

                    return;
                }

                TierImpl destinationTier =
                    new TierImpl(null, destTierName, dt.getParticipant(), transcription, linguisticType);
                destinationTier.setAnnotator(dt.getAnnotator());
                destinationTier.setDefaultLocale(dt.getDefaultLocale());
                destinationTier.setLangRef(dt.getLangRef());
                transcription.addTier(destinationTier);
                dt = destinationTier;
            }

            if (createdAnnos == null || createdAnnos.isEmpty()) {
                LOG.info("No annotations to restore");

                return;
            }

            int curPropMode = 0;

            curPropMode = transcription.getTimeChangePropagationMode();

            if (curPropMode != Transcription.NORMAL) {
                transcription.setTimeChangePropagationMode(Transcription.NORMAL);
            }

            transcription.setNotifying(false);

            Annotation ann;

            for (AnnotationDataRecord createdAnno : createdAnnos) {
                ann = dt.createAnnotation(createdAnno.getBeginTime(), createdAnno.getEndTime());

                if ((ann != null) && (createdAnno.getValue() != null)) {
                    ann.setValue(createdAnno.getValue());
                }
            }

            transcription.setNotifying(true);
            // restore the time propagation mode
            transcription.setTimeChangePropagationMode(curPropMode);
        }
    }

    /**
     * Deletes the new annotations and/or the new tier.
     */
    @Override
    public void undo() {
        if (transcription != null) {
            //if during the gap creation process, a new tier was added
            if (destTierCreated) {
                TierImpl dt = transcription.getTierWithId(destTierName);

                //remove that tier now
                if (dt != null) {
                    transcription.removeTier(dt);
                }
            } else {
                //annotations were added to an already existing tier, so remove annotations only
                TierImpl st = transcription.getTierWithId(destTierName);

                if (st != null) {
                    if ((createdAnnos != null) && (!createdAnnos.isEmpty())) {
                        transcription.setNotifying(false);

                        Annotation ann;

                        for (AnnotationDataRecord createdAnno : createdAnnos) {
                            ann = st.getAnnotationAtTime((createdAnno.getBeginTime()
                                                          + createdAnno.getEndTime()) / 2);

                            if (ann != null) {
                                st.removeAnnotation(ann);
                            }
                        }

                        transcription.setNotifying(true);
                    }
                }
            }
        }
    }

    /**
     * <b>Note: </b>it is assumed the types and order of the arguments are correct.<br>
     * If arg[1] is null it is assumed that the annotations have to be inserted on the source tier.
     *
     * @param receiver the TranscriptionImpl
     * @param arguments the arguments: <ul><li>arg[0] = the selected tier (String)</li> <li>arg[1] = the new tier name.
     *     can be null (String)     </li> <li>arg[2] = the value for the new annotations, can be null(String)</li> <li>arg[3]
     *     = the time format in case the duration should be the value (can be null, if not null the duration should be used)
     *     (String) </li> <li>arg[4] = the total media duration (Long)</li> </ul>
     */
    @Override
    public void execute(Object receiver, Object[] arguments) {
        transcription = (TranscriptionImpl) receiver;
        sourceTiers = (String[]) arguments[0];
        destTierName = (String) arguments[1];
        annValue = (String) arguments[2];
        timeFormat = (String) arguments[3];
        mediaDuration = (Long) arguments[4];

        // to be sure
        if (timeFormat != null) {
            annValue = null;
        }

        createdAnnos = new ArrayList<>();
        createAnnotations();
    }

    @Override
    public String getName() {
        return commandName;
    }

    /**
     * Iterates over the annotations of the source tier and creates annotations for the gaps.
     */
    private void createAnnotations() {
        //retrieve original tier (the tiers with gaps)
        ArrayList<TierImpl> tierList = new ArrayList<>();

        for (String sourceTier : sourceTiers) {
            TierImpl tier = transcription.getTierWithId(sourceTier);

            //check if tier is valid
            if (tier == null) {
                LOG.severe("The source tier '" + sourceTier + "' was not found");

                // message
                return;
            }

            tierList.add(tier);
        }

        //check if all source tiers are toplevel tiers (it's demanded)
        LinguisticType linguisticType = null;

        for (TierImpl tier : tierList) {
            linguisticType = tier.getLinguisticType();

            if (linguisticType.getConstraints() != null) {
                // the source tier should be a toplevel tier
                LOG.severe("The source tier is not a root tier.");

                // message
                return;
            }
        }



        //if destination tier name is null, we must create the new tier
        TierImpl destinationTier = transcription.getTierWithId(destTierName);

        if (destinationTier == null) {
            //Create new destination tier
            //NOTE: getParticipant takes participant of first tier in the list
            //      Maybe think about combining all participants in the dest. tier.
            final TierImpl tierImpl = tierList.get(0);
            destinationTier = new TierImpl(null, destTierName, tierImpl.getParticipant(), transcription, linguisticType);
            destinationTier.setAnnotator(tierImpl.getAnnotator());
            destinationTier.setDefaultLocale(tierImpl.getDefaultLocale());
            destinationTier.setLangRef(tierImpl.getLangRef());
            transcription.addTier(destinationTier);

            //flag for the undo command that the destination tier is created so that
            //when undo takes place, the command knows that we can remove the tier as a whole
            destTierCreated = true;
        }

        //Stage 2: Computing gaps
        //         destinationTier contains name of tier where the gaps should be placed
        //         tierList contains the tiers where the gaps should be computed from
        int curPropMode = transcription.getTimeChangePropagationMode();
        transcription.setNotifying(false);

        if (curPropMode != Transcription.NORMAL) {
            transcription.setTimeChangePropagationMode(Transcription.NORMAL);
        }

        //get the annotations from the source tier
        ArrayList<Gap> commonGaps = getCommonGaps(tierList);
        AlignableAnnotation aa = null;

        for (Gap gap : commonGaps) {
            if (gap.begin == gap.end) {
                continue;
            }

            aa = (AlignableAnnotation) destinationTier.createAnnotation(gap.begin, gap.end);

            if (aa != null) {
                if (timeFormat != null) {
                    aa.setValue(getTimeString(gap.end - gap.begin));
                } else if (annValue != null) {
                    aa.setValue(annValue);
                    // TODO: maybe set the CvEntryId
                }

                createdAnnos.add(new AnnotationDataRecord(aa));
            } else {
                LOG.warning("Annotation could not be created " + gap.begin + "-" + gap.end);
            }
        }

        LOG.info("Number of annotations created: " + commonGaps.size());

        if (transcription.getTimeChangePropagationMode() != curPropMode) {
            transcription.setTimeChangePropagationMode(curPropMode);
        }

        transcription.setNotifying(true);
    }

    /**
     * Converts a numeric time value to a (formatted) string.
     *
     * @param time to convert to string
     *
     * @return a string representation
     */
    private String getTimeString(long time) {
        if (Constants.HHMMSSMS_STRING.equals(timeFormat)) {
            return TimeFormatter.toString(time);
        } else if (Constants.SSMS_STRING.equals(timeFormat)) {
            return TimeFormatter.toSSMSString(time);
        } else {
            return String.valueOf(time);
        }
    }

    /**
     * Given a list of tiers, it computes the common gaps that all the tiers in the list have. A common gap is the space
     * between two annotations (from the same or a different tier)  in which no annotation occurs.
     *
     * @param tierList the list of tiers where the common gaps are computed for
     *
     * @return a list of gaps
     */
    private ArrayList<Gap> getCommonGaps(ArrayList<TierImpl> tierList) {
        ArrayList<Gap> gapList = new ArrayList<>();

        //if there are no tiers to be inspected, then there are no gaps as well
        if (tierList.isEmpty()) {
            return gapList;
        }

        //Stage 1: First loop over the first tier and store all gaps
        TierImpl currentTier = tierList.get(0);
        List<AbstractAnnotation> annotations = currentTier.getAnnotations();

        long timeSoFar = 0;
        long beginTime;

        if (annotations.isEmpty()) {
            gapList.add(new Gap(0, mediaDuration));
        } else {
            //loop over all annotations and store the gaps
            for (AbstractAnnotation abstrAnnotation : annotations) {
                beginTime = abstrAnnotation.getBeginTimeBoundary();

                //check if beginTime is greater than timeSoFar. If so, then there is a gap
                if (beginTime > timeSoFar) {
                    gapList.add(new Gap(timeSoFar, beginTime));
                }

                //update timeSoFar so that it represents the end of the current annotation
                timeSoFar = abstrAnnotation.getEndTimeBoundary();
            }

            //add gap between last annotation and media duration
            if (timeSoFar < mediaDuration) {
                gapList.add(new Gap(timeSoFar, mediaDuration));
            }
        }

        //Stage 2: Loop over the remaining tiers
        for (int i = 1; i < tierList.size(); i++) {
            //check if there are gaps
            if (gapList.isEmpty()) {
                break;
            }

            currentTier = tierList.get(i);
            annotations = currentTier.getAnnotations();
            Gap currentGap;

            for (int gapNr = 0; gapNr < gapList.size(); gapNr++) {
                if (annotations.isEmpty()) {
                    break;
                }

                currentGap = gapList.get(gapNr);
                AbstractAnnotation abstrAnnotation = annotations.get(0);
                beginTime = abstrAnnotation.getBeginTimeBoundary();

                long endTime = abstrAnnotation.getEndTimeBoundary();

                // annotation and gap coincide
                if ((beginTime == currentGap.begin) && (endTime == currentGap.end)) {
                    gapList.remove(currentGap);
                    annotations.remove(0);
                    gapNr--;
                    continue;
                }

                //check if annotations falls within gap
                if ((beginTime >= currentGap.begin) && (endTime <= currentGap.end)) {
                    //annotation falls within gap, so we need to update one gap and add another
                    long temp = currentGap.end;
                    currentGap.end = beginTime;
                    gapList.add(gapNr + 1, new Gap(endTime, temp));

                    annotations.remove(0);

                    continue;
                }

                if ((beginTime <= currentGap.begin) && (endTime > currentGap.begin) && (endTime <= currentGap.end)) {
                    currentGap.begin = endTime;
                    if (currentGap.begin == currentGap.end) {
                        gapList.remove(currentGap);
                    }
                    annotations.remove(0);
                    gapNr--;

                    continue;
                }

                if ((beginTime >= currentGap.begin) && (beginTime < currentGap.end) && (endTime >= currentGap.end)) {
                    currentGap.end = beginTime;
                    if (currentGap.begin == currentGap.end) {
                        gapList.remove(currentGap);
                        gapNr--;
                    }

                    continue;
                }

                //if annotation is after gap
                if (beginTime >= currentGap.end) {
                    continue;
                }

                //if annotation is before gap
                if ((beginTime < currentGap.begin) && (endTime <= currentGap.begin)) {
                    annotations.remove(0);
                    gapNr--;

                    continue;
                }

                //if annotation spans over whole gap
                if ((beginTime <= currentGap.begin) && (endTime >= currentGap.end)) {
                    //remove gap and lower gapNr so that in for loop update, it points to
                    //the correct next gap
                    gapList.remove(gapNr);
                    gapNr--;

                    //don't remove annotation, because it can also affect next gap
                    continue;
                }
            }
        }

        return gapList;
    }

    /**
     * Utility class for a time interval, with public access to the time members  (as opposed to
     * mpi.eudico.util.TimeInterval)
     */
    private class Gap {
        /**
         * begin time
         */
        public long begin;

        /**
         * end time
         */
        public long end;

        /**
         * Creates a new Gap instance
         *
         * @param begin begin time
         * @param end end time
         */
        public Gap(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }
    }
}
