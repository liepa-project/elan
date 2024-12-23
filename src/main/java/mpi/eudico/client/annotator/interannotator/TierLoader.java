package mpi.eudico.client.annotator.interannotator;

import mpi.eudico.server.corpora.clomimpl.abstr.ParseException;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.dobes.EAFSkeletonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class for loading tiers from a transcription or from multiple files. Should move to a different package, there is too
 * much overlap with the loading of tiers in some export functions.
 *
 * @author Han Sloetjes
 */
public class TierLoader extends Thread {
    private final List<File> fileList;
    private int numFilesProcessed = 0;
    private int numFilesFailed = 0;
    // might need more than just the tier names
    private List<String> tierNames;

    /**
     * Constructor.
     *
     * @param files a list containing File objects
     */
    public TierLoader(List<File> files) {
        if (files == null) {
            throw new NullPointerException("There is no list of files.");
        }
        fileList = files;
    }

    @Override
    public void run() {
        tierNames = new ArrayList<String>();

        for (File f : fileList) {
            if (f == null) {
                numFilesProcessed++;
                numFilesFailed++;
                continue;
            }
            try {
                //long st = System.currentTimeMillis();
                EAFSkeletonParser parser = new EAFSkeletonParser(f.getAbsolutePath());
                parser.parse();
                //System.out.println("Skeleton: " + f.getName() + ": " + (System.currentTimeMillis() - st));
                List<TierImpl> tiers = parser.getTiers();

                for (TierImpl tier : tiers) {
                    if (tier != null) {
                        if (!tierNames.contains(tier.getName())) {
                            tierNames.add(tier.getName());
                        }
                    }
                }

            } catch (ParseException pe) {
                numFilesFailed++;
            }

            numFilesProcessed++;
        }

        // sort the list of tiers alphabetically
        Collections.sort(tierNames);

    }

    /**
     * Returns the list of tier names.
     *
     * @return the list of tier names
     */
    public List<String> getTierNames() {
        return tierNames;
    }

    /**
     * Returns the number of processed files.
     *
     * @return the number of files that have been processed so far
     */
    public int getNumProccessed() {
        return numFilesProcessed;
    }

    /**
     * Returns the number of failed files.
     *
     * @return the number of files that failed so far.
     */
    public int getNumFailed() {
        return numFilesFailed;
    }

    /**
     * Returns the total number of files.
     *
     * @return returns the total number of files.
     */
    public int getTotalNumberOfFiles() {
        return fileList.size();
    }

}
