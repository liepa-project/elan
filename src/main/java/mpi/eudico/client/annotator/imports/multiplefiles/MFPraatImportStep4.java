package mpi.eudico.client.annotator.imports.multiplefiles;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import mpi.eudico.client.annotator.imports.praat.PraatTextGrid;
import mpi.eudico.client.annotator.util.AnnotationDataRecord;
import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clomimpl.abstr.MediaDescriptor;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;
import mpi.eudico.server.corpora.clomimpl.type.LinguisticType;
import mpi.eudico.server.corpora.clomimpl.util.MediaDescriptorUtility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static mpi.eudico.client.annotator.util.ClientLogger.LOG;

/**
 * Final step pane for the multiple file import functions
 */
@SuppressWarnings("serial")
public class MFPraatImportStep4 extends AbstractMFImportStep4 {
    private boolean ipt = false;
    private int dur = 40;
    private boolean skipEmptyIntervals;
    private boolean autoDetectWavFile;

    /**
     * Constructor
     *
     * @param multiPane the multiple step pane
     */
    public MFPraatImportStep4(MultiStepPane multiPane) {
        super(multiPane);
    }

    @Override
    public void enterStepForward() {
        Boolean includePT = (Boolean) multiPane.getStepProperty("PointTier");
        Integer duration = (Integer) multiPane.getStepProperty("PointDuration");
        Boolean skipEmpty = (Boolean) multiPane.getStepProperty("SkipEmpty");
        Boolean autoDetectWav = (Boolean) multiPane.getStepProperty("AutoDetectWav");

        if (includePT != null) {
            ipt = includePT.booleanValue();
        }

        if (duration != null) {
            dur = duration.intValue();
        }

        if (skipEmpty != null) {
            skipEmptyIntervals = skipEmpty.booleanValue();
        }

        if (autoDetectWav != null) {
            autoDetectWavFile = autoDetectWav.booleanValue();
        }

        super.enterStepForward();
    }


    @Override
    protected boolean doImport(File sourceFile) {
        final File impFile = sourceFile;
        boolean imported = false;


        if ((impFile == null) || !impFile.exists()) {
            //progressInterrupted(null, ElanLocale.getString("MultiFileImport.Report.NoFile"));
            report(ElanLocale.getString("MultiFileImport.Report.NoFile"));
            return false;
        }

        try {
            PraatTextGrid ptg = new PraatTextGrid(impFile, ipt, dur, encoding);
            imported = startImport(ptg);
        } catch (IOException ioe) {
            LOG.warning("Could not handle file: " + impFile + "\n" + ioe.getMessage());
            //progressInterrupted(null, ioe.getMessage());
            report(ElanLocale.getString("MultiFileImport.Report.ParseError") + "\n" + ioe.getMessage());
        }

        return imported;

        //create transcription
    }


    private boolean startImport(PraatTextGrid ptg) {
        // initialise some fields
        transImpl = new TranscriptionImpl();
        ArrayList<String> tierNames = new ArrayList<String>();
        ArrayList<AnnotationDataRecord> annRecords = new ArrayList<AnnotationDataRecord>();


        LinguisticType type = new LinguisticType("Praat");
        type.setTimeAlignable(true);
        transImpl.addLinguisticType(type);

        if (autoDetectWavFile) {
            String gridFile = ptg.getGridFile().getAbsolutePath();
            int extensionPos = gridFile.lastIndexOf('.');

            if (extensionPos >= 0) {

                String wavFilePath = gridFile.substring(0, extensionPos).concat(".wav");
                File wavFile = new File(wavFilePath);
                if (wavFile.exists()) {
                    List<MediaDescriptor> mds = new ArrayList<MediaDescriptor>(1);
                    MediaDescriptor md = MediaDescriptorUtility.createMediaDescriptor(wavFile.getAbsolutePath());
                    mds.add(md);
                    transImpl.setMediaDescriptors(mds);

                }

            }
        }

        try {
            int tierCount = ptg.getTierNames().size();

            if (tierCount == 0) {
                return false;
            }

            //float perTier = 10f / tierCount;

            for (int i = 0; i < tierCount; i++) {
                String name = ptg.getTierNames().get(i);


                TierImpl t = new TierImpl(name, "", transImpl, type);

                if (transImpl.getTierWithId(name) == null) {
                    transImpl.addTier(t);
                    tierNames.add(name);
                }
            }

            tierCount = tierNames.size();
            //perTier = 90f / tierCount;

            for (int i = 0; i < tierCount; i++) {
                String name = tierNames.get(i);
                TierImpl t = transImpl.getTierWithId(name);
                List<AnnotationDataRecord> anns = ptg.getAnnotationRecords(name);

                if ((anns.size() == 0) || (t == null)) {
                    continue;
                }

                //float perAnn = perTier / anns.size();
                AnnotationDataRecord annotationDataRecord = null;
                Annotation ann;
                for (int j = 0; j < anns.size(); j++) {
                    annotationDataRecord = anns.get(j);
                    if (skipEmptyIntervals && (annotationDataRecord.getValue() == null
                                               || annotationDataRecord.getValue().length() == 0)) {
                        // progress update??
                        continue;
                    }
                    ann = t.createAnnotation(annotationDataRecord.getBeginTime(), annotationDataRecord.getEndTime());
                    if (ann != null) {
                        ann.setValue(annotationDataRecord.getValue());
                        annRecords.add(new AnnotationDataRecord(ann));
                    }
                }
            }
            return true;

        } catch (Exception exc) {
            LOG.warning("Unknown Error:" + "\n" + exc.getMessage());
            //progressInterrupted(ElanLocale.getString("MultiFileImport.Report.ExceptionOccured"), exc.getMessage());

            report(ElanLocale.getString("MultiFileImport.Report.ExceptionOccured") + "\n" + exc.getMessage());
            return false;
        }
    }
}
