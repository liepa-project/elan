package mpi.eudico.client.annotator.commands;

import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.Preferences;
import mpi.eudico.client.annotator.gui.ReportDialog;
import mpi.eudico.server.corpora.clom.Transcription;
import mpi.eudico.server.corpora.clomimpl.abstr.TierImpl;
import mpi.eudico.server.corpora.clomimpl.abstr.TranscriptionImpl;
import mpi.eudico.server.corpora.clomimpl.type.LinguisticType;
import mpi.eudico.server.corpora.util.SimpleReport;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

import static mpi.eudico.client.annotator.util.ClientLogger.LOG;

/**
 * A command to add tiers for a new participant to the transcription. Either based on an existing tier structure (top level
 * tier with dependent tiers) or on (the tiers for) an existing participant.
 */
public class AddParticipantCommand implements UndoableCommand {
    private final String commandName;
    private TranscriptionImpl transcription;
    private List<String> names;
    private String participantName;
    private ArrayList<TierImpl> newTiers;
    private HashMap<String, String> oldNameToNewName;
    private String oldValue;
    private String newValue;
    private boolean changePrefix;
    private SimpleReport report;

    /**
     * Constructor.
     *
     * @param name the name of the command
     */
    public AddParticipantCommand(String name) {
        commandName = name;
    }

    /**
     * <b>Note: </b>it is assumed the types and order of the arguments are correct.<br>
     * If arg[2] is null it is assumed that there is no number part.
     *
     * @param receiver the TranscriptionImpl
     * @param arguments the arguments: <ul>
     *     <li>arg[0] = the selected tiers/participants (ArrayList)</li>
     *     <li>arg[1] = participantName (String)</li>
     *     <li>arg[2] = oldValue (String)</li>
     *     <li>arg[3] = newValue (String)</li>
     *     <li>arg[4] = changePrefix (Boolean)</li>
     *     <li>arg[5] = tierStrucSelected (Boolean)</li>
     *     </ul>
     */
    @SuppressWarnings("unchecked")
    @Override
    public void execute(Object receiver, Object[] arguments) {
        transcription = (TranscriptionImpl) receiver;
        names = (List<String>) arguments[0];
        participantName = (String) arguments[1];
        oldValue = (String) arguments[2];
        newValue = (String) arguments[3];
        changePrefix = (Boolean) arguments[4];
        boolean tierStrucSelected = (Boolean) arguments[5];
        report = new SimpleReport(ElanLocale.getString("AddParticipantDlg.Title"));

        newValue = newValue.trim();

        if ((names == null) || (names.size() == 0)) {
            LOG.warning("No tier/participant selected.");
            transcription.setNotifying(true);
            return;
        }

        setWaitCursor(true);

        newTiers = new ArrayList<TierImpl>();
        oldNameToNewName = new HashMap<String, String>();

        TierImpl parentTier = null;

        if (tierStrucSelected) {
            report("Number of selected tier structures: " + names.size());
            report("Replace suffix/prefix : " + (changePrefix ? "prefix" : "suffix"));
            report("Value to be replaced : " + oldValue);
            report("New value to replace : " + newValue);
            report("New participant name : " + participantName);
            report("\n");

            createTierStructures(names);

        } else {
            report("Selected Participants: " + names.size());
            report("Replace suffix/prefix : " + (changePrefix ? "prefix" : "suffix"));
            report("Value to be replaced : " + oldValue);
            report("New value to replace : " + newValue);
            report("New participant name : " + participantName);
            report("\n");

            List<TierImpl> tiers = transcription.getTiers();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            for (int i = 0; i < tiers.size(); i++) {
                parentTier = tiers.get(i);
                if (parentTier != null && !parentTier.hasParentTier()) {
                    if (names.contains(parentTier.getParticipant())) {
                        List<String> rootTiers = map.get(parentTier.getParticipant());
                        if (rootTiers == null) {
                            rootTiers = new ArrayList<String>();
                        }
                        rootTiers.add(parentTier.getName());
                        map.put(parentTier.getParticipant(), rootTiers);
                    }
                }
            }

            for (Entry<String, List<String>> entry : map.entrySet()) {
                report("Creating tier structures of participant : " + entry.getKey());
                createTierStructures(entry.getValue());
                report("\n");
            }
        }

        ReportDialog dialog = new ReportDialog(report);
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        setWaitCursor(false);
    }

    /**
     * Adds a message to the report.
     *
     * @param message the message
     */
    public void report(String message) {
        if (report != null) {
            report.append(message);
        }
    }

    /**
     * @param tierList list of tier structures
     */
    private void createTierStructures(List<String> tierList) {
        for (int i = 0; i < tierList.size(); i++) {
            String tierName = tierList.get(i);

            report("Creating tier structure : " + tierName);

            TierImpl parentTier = transcription.getTierWithId(tierName);
            if (parentTier != null) {
                report("Number of tiers to be created : " + (parentTier.getDependentTiers().size() + 1));

                String newParentTier = getNewTierName(parentTier.getName());
                //check if tier exists with the same name as newParentTier
                if (transcription.getTierWithId(newParentTier) == null) {
                    addTiers(parentTier);
                } else {
                    report("Number of tiers created : 0");
                    report("Creation failed. "
                           + newParentTier
                           + " could not be created. A tier with this name already exists.");
                }
            } else {
                report("Number of tiers created : 0");
                report("Creation failed since the tier '" + tierName + "' cannot be found");
            }

            report("\n");
        }
    }

    /**
     * @param parentTier the top level tier of the tiers to copy
     */
    private void addTiers(TierImpl parentTier) {
        List<TierImpl> depTiers = parentTier.getDependentTiers();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(parentTier);
        DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[depTiers.size()];
        HashMap<TierImpl, DefaultMutableTreeNode> nodeMap = new HashMap<TierImpl, DefaultMutableTreeNode>();
        for (int i = 0; i < depTiers.size(); i++) {
            TierImpl ti = depTiers.get(i);
            nodes[i] = new DefaultMutableTreeNode(ti);
            nodeMap.put(ti, nodes[i]);
        }

        for (int i = 0; i < depTiers.size(); i++) {
            TierImpl ti = depTiers.get(i);

            if (ti.getParentTier() == parentTier) {
                rootNode.add(nodes[i]);
            } else {
                DefaultMutableTreeNode dn = nodeMap.get(ti.getParentTier());

                if (dn != null) {
                    dn.add(nodes[i]);
                }
            }
        }

        DefaultMutableTreeNode dn;
        TierImpl refTier;
        TierImpl newTier;
        TierImpl newParentTier;
        LinguisticType type;
        String newName;
        Enumeration<TreeNode> en = rootNode.breadthFirstEnumeration();

        int tiersCreated = 0;
        int failedTiers = 0;

        while (en.hasMoreElements()) {
            newTier = null;

            dn = (DefaultMutableTreeNode) en.nextElement();
            refTier = (TierImpl) dn.getUserObject();

            newName = getNewTierName(refTier.getName());

            // if tier with the newName already exists
            if (transcription.getTierWithId(newName) != null) {
                report("Could not create tier with this name : " + newName + " since a tier with this name already exists.");
                failedTiers++;
                continue;
            }
            oldNameToNewName.put(refTier.getName(), newName);
            type = refTier.getLinguisticType();

            if (dn.getParent() != null) {
                String oldParentName = ((TierImpl) ((DefaultMutableTreeNode) dn.getParent()).getUserObject()).getName();
                String newPName = oldNameToNewName.get(oldParentName);
                newParentTier = transcription.getTierWithId(newPName);

                if (newParentTier != null) {
                    newTier = new TierImpl(newParentTier, newName, participantName, transcription, type);
                } else {
                    report("Could not create tier : "
                           + newName
                           + " since its parent tier '"
                           + newPName
                           + "' could not be created.");
                    failedTiers++;
                    continue;
                }
            } else {
                newTier = new TierImpl(newName, participantName, transcription, type);
            }

            if (newTier != null) {
                newTier.setDefaultLocale(refTier.getDefaultLocale());
                newTier.setAnnotator(refTier.getAnnotator());
                newTier.setLangRef(refTier.getLangRef());

                newTiers.add(newTier);
                transcription.addTier(newTier);
                tiersCreated++;
                // copy tier colors and font prefs to the new tier
                copyTierAttributes(refTier.getName(), newName);
            }
        }

        report("Number of tiers created : " + tiersCreated);
        if (failedTiers > 0) {
            report("Number of tiers that could not be created : " + failedTiers);
            report("Creation succeeded with incomplete tier structure. A few depending tiers/child tiers could not be "
                   + "created.");
        } else {
            report("Creation succeeded.");
        }
    }

    private String getNewTierName(String tierName) {
        if (oldValue != null && !oldValue.trim().isEmpty()) {
            if (changePrefix) {
                if (tierName.startsWith(oldValue)) {
                    tierName = tierName.replaceFirst(oldValue, newValue);
                } else {
                    tierName = newValue + "-" + tierName;
                }
            } else {
                if (tierName.endsWith(oldValue)) {
                    tierName = tierName.substring(0, tierName.lastIndexOf(oldValue)) + newValue;
                } else {
                    tierName = tierName + "-" + newValue;
                }
            }

        } else {
            if (changePrefix) {
                tierName = newValue + "-" + tierName;
            } else {
                tierName = tierName + "-" + newValue;
            }
        }

        return tierName;
    }

    /**
     * Copies tier colors and the tier font from one tier to another.
     *
     * @param srcTierName the name of the source tier
     * @param destTierName the name of the destination tier
     */
    private void copyTierAttributes(String srcTierName, String destTierName) {
        Map<String, Color> map = Preferences.getMapOfColor("TierColors", transcription);
        if (map != null && map.containsKey(srcTierName)) {
            map.put(destTierName, map.get(srcTierName));
            Preferences.set("TierColors", map, transcription, true);
        }

        Map<String, Color> hlMap = Preferences.getMapOfColor("TierHighlightColors", transcription);
        if (hlMap != null && hlMap.containsKey(srcTierName)) {
            hlMap.put(destTierName, hlMap.get(srcTierName));
            Preferences.set("TierHighlightColors", hlMap, transcription, true);
        }

        Map<String, Font> fontsMap = Preferences.getMapOfFont("TierFonts", transcription);
        if (fontsMap != null && fontsMap.containsKey(srcTierName)) {
            fontsMap.put(destTierName, fontsMap.get(srcTierName));
            Preferences.set("TierFonts", fontsMap, transcription, true);
        }
    }

    /**
     * Removes copied tier attributes from the preferences.
     */
    private void undoTierAttributes() {
        if (newTiers != null) {
            Map<String, Font> fontsMap = Preferences.getMapOfFont("TierFonts", transcription);
            Map<String, Color> colMap = Preferences.getMapOfColor("TierColors", transcription);
            Map<String, Color> hlMap = Preferences.getMapOfColor("TierHighlightColors", transcription);

            for (TierImpl t : newTiers) {
                if (fontsMap != null) {
                    fontsMap.remove(t.getName());
                }
                if (colMap != null) {
                    colMap.remove(t.getName());
                }
                if (hlMap != null) {
                    hlMap.remove(t.getName());
                }
            }
        }
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

    @Override
    public String getName() {
        return commandName;
    }

    /**
     * Undo
     */
    @Override
    public void undo() {
        if ((transcription != null) && (newTiers != null)) {
            for (int i = 0; i < newTiers.size(); i++) {
                TierImpl t = newTiers.get(i);
                transcription.removeTier(t);
            }
            undoTierAttributes();
        }
    }

    /**
     * Redo
     */
    @Override
    public void redo() {
        if ((transcription != null) && (newTiers != null)) {
            int curPropMode = transcription.getTimeChangePropagationMode();

            if (curPropMode != Transcription.NORMAL) {
                transcription.setTimeChangePropagationMode(Transcription.NORMAL);
            }
            setWaitCursor(true);

            TierImpl tier = null;

            for (int i = 0; i < newTiers.size(); i++) {
                tier = newTiers.get(i);

                if (transcription.getTierWithId(tier.getName()) == null) {
                    transcription.addTier(tier);
                }
            }

            Iterator<String> tierIter = oldNameToNewName.keySet().iterator();
            while (tierIter.hasNext()) {
                String k = tierIter.next();
                copyTierAttributes(k, oldNameToNewName.get(k));
            }

            setWaitCursor(false);
            // restore the time propagation mode
            transcription.setTimeChangePropagationMode(curPropMode);
        }
    }
}
