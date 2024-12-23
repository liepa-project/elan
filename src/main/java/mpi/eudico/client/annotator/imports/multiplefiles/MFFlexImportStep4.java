package mpi.eudico.client.annotator.imports.multiplefiles;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.gui.multistep.MultiStepPane;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;
import mpi.eudico.server.corpora.clomimpl.flex.FlexDecoderInfo;

import java.io.File;

import static mpi.eudico.client.annotator.util.ClientLogger.LOG;


/**
 * Final step pane for the multiple file import functions
 *
 * @author aarsom
 * @version April 2013
 */
public class MFFlexImportStep4 extends AbstractMFImportStep4 {
    private FlexDecoderInfo decoderInfo;

    /**
     * Constructor
     *
     * @param multiPane the multiple step pane
     */
    public MFFlexImportStep4(MultiStepPane multiPane) {
        super(multiPane);
    }

    @Override
    public void enterStepForward() {
        decoderInfo = (FlexDecoderInfo) multiPane.getStepProperty("FlexDecoderInfo");
        super.enterStepForward();
    }

    @Override
    protected boolean doImport(File sourceFile) {
        final File impFile = sourceFile;

        if (impFile == null || !impFile.exists()) {
            //progressInterrupted(null, ElanLocale.getString("MultiFileImport.Report.NoFile"));
            LOG.severe("Flex file not found : " + impFile != null ? impFile.getAbsolutePath() : "path is not available.");
            report(ElanLocale.getString("MultiFileImport.Report.NoFile"));
            return false;
        }

        if (decoderInfo == null) {
            return false;
        }

        decoderInfo.setSourceFilePath(impFile.getAbsolutePath());

        try {
            transImpl = new TranscriptionImpl(impFile.getAbsolutePath(), decoderInfo);
        } catch (Exception e) {
            LOG.warning(ElanLocale.getString("MultiFileImport.Report.ExceptionOccured : ") + e.getMessage());
            report(ElanLocale.getString("MultiFileImport.Report.ExceptionOccured : ") + e.getMessage());
            return false;
        }

        return true;
    }


}
