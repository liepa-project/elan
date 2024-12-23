package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.util.AnnotationValuesRecord;
import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clom.Transcription;
import mpi.eudico.server.corpora.clomimpl.abstr.AbstractAnnotation;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static mpi.eudico.client.annotator.util.ClientLogger.LOG;

/**
 * A command to change the case of annotations.
 *
 * @author Han Sloetjes
 */
public class ChangeCaseCommand implements UndoableCommand {
    private final String name;
    private Transcription transcription;
    private List<String> tierNames;
    private boolean uppercase;
    private boolean beginCapital;
    private List<AnnotationValuesRecord> records;

    /**
     * Constructor.
     *
     * @param name the name of the command
     */
    public ChangeCaseCommand(String name) {
        super();
        this.name = name;
    }

    /**
     * Changes the case again
     */
    @Override
    public void redo() {
        setWaitCursor(true);
        ((TranscriptionImpl) transcription).setNotifying(false);

        String name = null;
        TierImpl tier = null;

        if (tierNames != null) {
            if (records != null) {
                Annotation ann = null;
                AnnotationValuesRecord annotationValuesRecord = null;

                for (int i = 0; i < records.size(); i++) {
                    annotationValuesRecord = records.get(i);

                    name = annotationValuesRecord.getTierName();

                    if ((tier == null) || !tier.getName().equals(name)) {
                        tier = (TierImpl) transcription.getTierWithId(name);
                    }

                    if (tier != null) {
                        ann = tier.getAnnotationAtTime(annotationValuesRecord.getBeginTime());

                        if ((ann != null) && (ann.getEndTimeBoundary() == annotationValuesRecord.getEndTime())) {
                            ann.setValue(annotationValuesRecord.getNewLabelValue());
                        } else {
                            LOG.warning("The annotation could not be found for undo");
                        }
                    } else {
                        LOG.warning("The tier could not be found: " + name);
                    }
                }
            } else {
                LOG.info("No annotation records have been stored for undo.");
            }
        } else {
            LOG.warning("No tier names have been stored.");
        }

        ((TranscriptionImpl) transcription).setNotifying(true);
        setWaitCursor(false);
    }

    /**
     * Returns the annotation values to their original state
     */
    @Override
    public void undo() {
        setWaitCursor(true);
        ((TranscriptionImpl) transcription).setNotifying(false);

        String name = null;
        TierImpl tier = null;

        if (tierNames != null) {
            if (records != null) {
                Annotation ann = null;
                AnnotationValuesRecord annotationValuesRecord = null;

                for (int i = 0; i < records.size(); i++) {
                    annotationValuesRecord = records.get(i);

                    name = annotationValuesRecord.getTierName();

                    if ((tier == null) || !tier.getName().equals(name)) {
                        tier = (TierImpl) transcription.getTierWithId(name);
                    }

                    if (tier != null) {
                        ann = tier.getAnnotationAtTime(annotationValuesRecord.getBeginTime());

                        if ((ann != null) && (ann.getEndTimeBoundary() == annotationValuesRecord.getEndTime())) {
                            ann.setValue(annotationValuesRecord.getValue());
                        } else {
                            LOG.warning("The annotation could not be found for undo");
                        }
                    } else {
                        LOG.warning("The tier could not be found: " + name);
                    }
                }
            } else {
                LOG.info("No annotation records have been stored for undo.");
            }
        } else {
            LOG.warning("No tier names have been stored.");
        }

        ((TranscriptionImpl) transcription).setNotifying(true);
        setWaitCursor(false);
    }

    /**
     * NChanges the case of annotations of selected tiers
     * <b>Note: </b>it is assumed the types and order of the arguments are
     * correct.
     *
     * @param receiver the transcription
     * @param arguments the arguments: <ul><li>arg[0] = the selected tier names ({@code List<String>})</li> <li>arg[1] if
     *     true change to uppercase (Boolean)</li>
     *     <li>arg[2] use begin capital in case arg[1] is false (meaning lowercase) (Boolean)</li>
     *     </ul>
     */
    @Override
    public void execute(Object receiver, Object[] arguments) {
        transcription = (Transcription) receiver;
        tierNames = (List<String>) arguments[0];
        if (tierNames == null || tierNames.isEmpty()) {
            return;
        }
        uppercase = ((Boolean) arguments[1]).booleanValue();
        beginCapital = ((Boolean) arguments[2]).booleanValue();
        records = new ArrayList<AnnotationValuesRecord>();
        changeCase();
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Changes the cursor to either a 'busy' cursor or the default cursor.
     *
     * @param showWaitCursor when <code>true</code> show the 'busy' cursor
     */
    private void setWaitCursor(boolean showWaitCursor) {
        if (showWaitCursor) {
            ELANCommandFactory.getRootFrame(transcription)
                              .getRootPane()
                              .setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            ELANCommandFactory.getRootFrame(transcription).getRootPane().setCursor(Cursor.getDefaultCursor());
        }
    }

    private void changeCase() {
        TierImpl t;
        AbstractAnnotation aa;
        AnnotationValuesRecord avr;
        setWaitCursor(true);
        ((TranscriptionImpl) transcription).setNotifying(false);
        for (String name : tierNames) {
            t = (TierImpl) transcription.getTierWithId(name);
            if (t != null) {
                int size = t.getAnnotations().size();
                for (int i = 0; i < size; i++) {
                    aa = t.getAnnotations().get(i);

                    if (aa.getValue() != null && !aa.getValue().isEmpty()) {
                        avr = new AnnotationValuesRecord(aa);
                        records.add(avr);
                        String postFix = aa.getValue().substring(1);
                        String firstCharacter = aa.getValue().substring(0, 1);
                        String capitalizedFirstLetter = firstCharacter.toUpperCase(ElanLocale.getLocale());
                        if (uppercase) {
                            if (beginCapital) {
                                // Change only the first character to upper case and leave the rest alone.
                                aa.setValue(capitalizedFirstLetter + postFix);
                            } else {
                                aa.setValue(aa.getValue().toUpperCase(ElanLocale.getLocale()));
                            }
                        } else {
                            if (beginCapital) {
                                // Change the first character to upper case and the rest to lower case.
                                aa.setValue(capitalizedFirstLetter + postFix.toLowerCase(ElanLocale.getLocale()));
                            } else {
                                aa.setValue(aa.getValue().toLowerCase(ElanLocale.getLocale()));
                            }
                        }
                        avr.setNewLabelValue(aa.getValue());
                    }
                }
            }
        }
        ((TranscriptionImpl) transcription).setNotifying(true);
        setWaitCursor(false);
        /*
        TierImpl t;
        AbstractAnnotation aa;
        //AnnotationValuesRecord avr;
        int numAnn = 0;
        for (String name : tierNames) {
            t = (TierImpl) transcription.getTierWithId(name);
            if (t != null) {
                int size = t.getAnnotations().size();
                numAnn += size;
            }
        }
        // doesn't seem to be necessary
        ChangeCaseThread cct = new ChangeCaseThread("Change Case");
        ProgressMonitor monitor = new ProgressMonitor(ELANCommandFactory.getRootFrame(transcription),
                ElanLocale.getString("ChangeCaseDialog.Message"), "", 0, numAnn);
        cct.setMonitor(monitor);
        cct.start();
        */
    }

    /*
     * A thread to change the case of the annotations of several tiers.
     *
     * @author Han Sloetjes
     */
    /*
    private class ChangeCaseThread extends Thread {
        private ProgressMonitor monitor;


        public ChangeCaseThread(String name) {
            super(name);
            // TODO Auto-generated constructor stub
        }


        public void setMonitor(ProgressMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public void run() {
            TierImpl t;
            AbstractAnnotation aa;
            AnnotationValuesRecord avr;
            int count = 0;
            ((TranscriptionImpl) transcription).setNotifying(false);
            for (String name : tierNames) {
                if (monitor != null && monitor.isCanceled()) {
                    monitor.close();
                    ((TranscriptionImpl) transcription).setNotifying(true);
                    return;
                }
                t = (TierImpl) transcription.getTierWithId(name);
                if (t != null) {
                    int size = t.getAnnotations().size();
                    for (int i = 0; i < size; i++) {
                        if (monitor != null && monitor.isCanceled()) {
                            monitor.close();
                            ((TranscriptionImpl) transcription).setNotifying(true);
                            return;
                        }
                        aa = (AbstractAnnotation) t.getAnnotations().get(i);

                        if (aa.getValue() != null && aa.getValue().length() > 0) {
                            avr = new AnnotationValuesRecord(aa);
                            records.add(avr);
                            if (uppercase) {
                                aa.setValue(aa.getValue().toUpperCase());
                            } else {
                                if (beginCapital) {
                                    aa.setValue(aa.getValue().substring(0, 1).toUpperCase() + aa.getValue().substring
                                    (1).toLowerCase());
                                } else {
                                    aa.setValue(aa.getValue().toLowerCase());
                                }
                            }
                            avr.setNewLabelValue(aa.getValue());
                        }
                        count++;
                        if (monitor != null) {
                            monitor.setProgress(count);
                        }
                    }
                }
            }
            if (monitor != null) {
                monitor.setProgress(monitor.getMaximum());
                ((TranscriptionImpl) transcription).setNotifying(true);
            }
        }

    }
    */
}
