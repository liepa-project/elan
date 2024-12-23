package mpi.eudico.server.corpora.clomimpl.flex;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//import java.util.Deque;


/**
 * Flex2EAF file conversion as of 2008-12-31;
 * 
 * @author Alexander Nakhimovsky &lt;adnakh@gmail.com&gt;
 * @author Tom Myers &lt;tommyers@dreamscape.com&gt;
 */
public class Flex2EAF extends DefaultHandler {
    private int currentTimeSlot = 1;

    /* estimation of total media time */
    private int totalTime = 300000;

    /* type renaming */
    private Properties typeRenamings = null;

    /* extension to mime-type */
    private Properties extTypes = null;

    /* the media file name or path */
    private String mediaFile = null;

    /* current mime-type */
    private String mimeType = null;

    /* the level below which tiers should not be time aligned*/
    private String noSynch = "word"; // do not synchronize at or below this tag name

    private boolean addSynchData = true;

    /* map from tier name to tier record */
    private HashMap<String, tierObject> tiers = new HashMap<String, tierObject>();

    /* map from item name to item object */
    private HashMap<String, itemObject> items = new HashMap<String, itemObject>();

    /* a list of tier names */
    private ArrayList<String> tierNames = new ArrayList<String>();

    /* a parse time string stack */
    private Stack<String> stack = new Stack<String>();

    /* a stack for time values */
    private Stack<Integer> timeStack = new Stack<Integer>();

    /* a tier to item map */
    private HashMap<String, String> tierOfItem = new HashMap<String, String>();

    /* a stack for parent tier names */
    private Stack<String> parentTiersIDStack = new Stack<String>();

    /* a stack for parent items */
    private Stack<String> parentItemIDStack = new Stack<String>();

    /* the id of the current item */
    private String currentItemID = "";

    /* a builder for the value of an item */
    private StringBuilder itemVal = new StringBuilder();
    /* a print writer for writing the EAF */
    private PrintWriter pW = null;
    private boolean debugOn = false;
    private int annotation_ID = 1;

    /**
     * Creates a new Flex2EAF instance
     *
     * @param w the writer that will be wrapped in the print writer
     */
    public Flex2EAF(Writer w) {
        pW = new PrintWriter(w, true);
        extTypes = readExtTypes();
    }

    /**
     * Creates a new Flex2EAF instance
     *
     * @param w the writer that will be wrapped in the print writer
     * @param dbg a debug flag
     * @param totTime total time value
     * @param typePairs ty[e renaming pairs
     * @param mediaFileStr the media file path
     * @param noSynchStr the level below which not to use time alignmant
     *
     * @throws Exception any exception that can occur
     */
    public Flex2EAF(Writer w, boolean dbg, int totTime, String typePairs,
        String mediaFileStr, String noSynchStr) throws Exception {
        mediaFile = mediaFileStr;
        noSynch = noSynchStr;
        pW = new PrintWriter(w, true);

        if ((typePairs != null) && (typePairs.length() > 0)) {
            typeRenamings = new Properties();

            String[] pairs = typePairs.split(",");

            for (int i = 0; i < pairs.length; i++) {
                String[] pair = pairs[i].split(":");

                if ((pair != null) && (pair.length == 2)) {
                    typeRenamings.setProperty(pair[0], pair[1]);
                }
            }
        }

        debugOn = dbg;
        totalTime = totTime;
        extTypes = readExtTypes();

        if (mediaFile != null) {
            String[] fp = mediaFile.split("[.]");
            mimeType = extTypes.getProperty(fp[fp.length - 1]);
        }
    }

    /**
     * Creates a new Flex2EAF instance
     *
     * @param w the writer that will be wrapped in the print writer
     * @param dbg a debug flag
     * @param totTime total time value
     */
    public Flex2EAF(Writer w, boolean dbg, int totTime) {
        debugOn = dbg;
        totalTime = totTime;
        pW = new PrintWriter(w, true);
        extTypes = readExtTypes();
    }

    /**
     * Creates a new Flex2EAF instance
     *
     * @param w the writer that will be wrapped in the print writer
     * @param totTime total time value
     */
    public Flex2EAF(Writer w, int totTime) {
        totalTime = totTime;
        pW = new PrintWriter(w, true);
        extTypes = readExtTypes();
    }

    /**
     * Adds a tier name and record to a list and map.
     *
     * @param tOb the tier object record
     */
    public void addTier(tierObject tOb) {
        tierNames.add(tOb.tierID);
        tiers.put(tOb.tierID, tOb);
    }

    /**
     * Writes root XML element.
     */
    public void writeDocEltOpen() {
        pW.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        pW.write(
            "<ANNOTATION_DOCUMENT AUTHOR='' DATE='2004-02-12T14:59:03+01:00' " +
            " FORMAT='2.6' VERSION='2.6' " +
            " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            " xsi:noNamespaceSchemaLocation='http://www.mpi.nl/tools/elan/EAFv2.6.xsd'>\n");
    }

    /**
     * Writes the HEADER element.
     */
    public void writeHeader() {
        if (mediaFile == null) {
            pW.write(" <HEADER MEDIA_FILE='' TIME_UNITS='milliseconds'>\n " +
                "  <MEDIA_DESCRIPTOR MEDIA_URL='file:///home/tjm/elan-example1.mpg' MIME_TYPE='video/mpeg' RELATIVE_MEDIA_URL='file:/./elan-example1.mpg'/>\n" +
                "  <MEDIA_DESCRIPTOR MEDIA_URL='file:///home/tjm/elan-example1.wav' MIME_TYPE='audio/x-wav' RELATIVE_MEDIA_URL='file:/./elan-example1.wav' EXTRACTED_FROM='file:///home/tjm/elan-example1.mpg'/>\n" +
                "</HEADER>\n");
        } else if (mediaFile.startsWith("http://")) {
            pW.write(" <HEADER MEDIA_FILE='' TIME_UNITS='milliseconds'>\n " +
                "  <MEDIA_DESCRIPTOR MIME_TYPE='" + mimeType + "' MEDIA_URL='" +
                mediaFile + "'/>\n" + "</HEADER>\n");
        } else {
            pW.write(" <HEADER MEDIA_FILE='' TIME_UNITS='milliseconds'>\n " +
                "  <MEDIA_DESCRIPTOR MIME_TYPE='" + mimeType +
                "' MEDIA_URL='file:/./" + mediaFile + "'/>\n" + "</HEADER>\n");
        }
    }

    /**
     * Parses the input.
     *
     * @param iSource the input source
     */
    public void parse(InputSource iSource) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();

            try {
                spf.setFeature("http://xml.org/sax/features/namespace-prefixes",
                    true);
            } catch (SAXException e) {
                pW.write("error " + e + " in setting up parser feature");
            }

            SAXParser parser = spf.newSAXParser();
            parser.parse(iSource, this);
        } catch (Exception ex) {
            ex.printStackTrace(pW);
        }
    }

    @Override
	public void startDocument() {
        writeDocEltOpen();
        writeHeader();
    }

    @Override
	public void characters(char[] ch, int offset, int len) { // del leading
                                                             // newlines

        {
            int lim = offset + len;

            while (offset < lim) {
                char c = ch[offset++];

                if (c == '&') {
                    itemVal.append("&amp;");
                } else if (c == '<') {
                    itemVal.append("&lt;");
                } else if (c == '>') {
                    itemVal.append("&gt;");
                } else {
                    itemVal.append(c);
                }
            }
        }
    }

    /**
     * Creates a new {@code itemObject}.
     * 
     * @param uri the URI
     * @param localName the local name
     * @param qName the qualified name
     * @param atts the attributes of the element
     * @throws SAXException when a parser exception occurs
     */
    public void startItem(String uri, String localName, String qName,
        Attributes atts) throws SAXException {
        new itemObject(atts);

        // itemObject itemOb=new
        // itemObject(isAlign,timeSlot,timeSlot,itemID,pItemID,tierID,isParent);
    }

    @Override
	public void startElement(String uri, String localName, String qName,
        Attributes atts) throws SAXException {
        if ("item".equals(qName)) {
            startItem(uri, localName, qName, atts);
        }

        if (debugOn) {
            pW.write("<" + qName);

            for (int i = 0; i < atts.getLength(); i++)
                pW.write(" " + atts.getQName(i) + "='" + atts.getValue(i) +
                    "'\n");

            pW.write(">");
        }

        stack.push(qName);
        itemVal.setLength(0);
        timeStack.push(currentTimeSlot);

        if (qName.equals(noSynch)) {
            addSynchData = false;
        }
    }

    private int newAnnotationID() {
        return annotation_ID++;
    }

    private void endItem(String uri, String localName, String qName)
        throws SAXException {
        try {
            stack.pop();
            timeStack.pop();

            if (debugOn) {
                pW.write(itemVal.toString());
                pW.write("</item>");
            }

            itemObject item = items.get(currentItemID);
            // if(currentTimeSlot==item.ts1)currentTimeSlot++;
            // item.ts2=currentTimeSlot;
            item.append(itemVal.toString());
            itemVal.setLength(0);
        } catch (Exception ex) {
            pW.write("Exception in endItem: " + ex);
        }
    }

    @Override
	public void endElement(String uri, String localName, String qName)
        throws SAXException {

        if ("item".equals(qName)) {
            endItem(uri, localName, qName);
        } else { // clear any alignable tiers based on this element; in any case,
                 // close element
            clearTiers(qName + "-");

            if (debugOn) {
                pW.write("</" + qName + ">\n");
            }

            stack.pop();
            timeStack.pop();

            if (qName.equals(noSynch)) {
                addSynchData = true;
            }
        }
    }

    private void clearTiers(String tierPrefix) {
        // clears any alignable tiers based on tierPrefix, and any items of
        // those tiers
        while (parentTiersIDStack.size() > 0) {
            String tierID = parentTiersIDStack.peek();

            if (!tierID.startsWith(tierPrefix)) {
                return;
            }

            parentTiersIDStack.pop();

            if (debugOn) {
                pW.write("<POPPINGTIER>" + tierID + "</POPPINGTIER>\n");
            }

            clearTierItems(tierID);
        }
    }

    private void clearTierItems(String tierID) { // clears any alignable
                                                 // items belonging to this
                                                 // tier.

        while (parentItemIDStack.size() > 0) {
            String itemID = parentItemIDStack.peek();

            if (!tierID.equals(tierOfItem.get(itemID))) {
                return;
            }

            parentItemIDStack.pop();

            itemObject item = items.get(itemID);

            if (currentTimeSlot == item.ts1) {
                currentTimeSlot++;
            }

            item.ts2 = currentTimeSlot;

            if (debugOn) {
                pW.write("<POPPING>" + itemID + ": " + tierID + " =" +
                    item.value + "\n</POPPING>\n");
            }
        }
    }

    @Override
	public void endDocument() {
        int aveTime = totalTime / (1 + currentTimeSlot);
        HashSet<Integer> synchedTimeSlots = new HashSet<Integer>();

        for (int i = 0; i < tierNames.size(); i++) {
            String tierID = tierNames.get(i);
            tiers.get(tierID).addTimeSlots(synchedTimeSlots);
        }

        pW.write("<TIME_ORDER>");

        for (int i = 0; i < currentTimeSlot; i++) {
            if (synchedTimeSlots.contains(i + 1)) {
                pW.write("<TIME_SLOT TIME_SLOT_ID='ts" + (i + 1) +
                    "' TIME_VALUE='" + (i * aveTime) + "'/>\n");
            } else {
                pW.write("<TIME_SLOT TIME_SLOT_ID='ts" + (i + 1) + "'/>\n");
            }
        }

        pW.write("</TIME_ORDER>\n");

        for (int i = 0; i < tierNames.size(); i++) {
            String tierID = tierNames.get(i);
            pW.write(tiers.get(tierID).toString());
        }

        pW.write(
            "<LINGUISTIC_TYPE LINGUISTIC_TYPE_ID='text' TIME_ALIGNABLE='true' GRAPHIC_REFERENCES='false'/>\n");
        pW.write(
            "<LINGUISTIC_TYPE LINGUISTIC_TYPE_ID='chunk' TIME_ALIGNABLE='false' GRAPHIC_REFERENCES='false' CONSTRAINTS='Symbolic_Subdivision'/>\n");
        pW.write(
            "<LINGUISTIC_TYPE LINGUISTIC_TYPE_ID='gloss' TIME_ALIGNABLE='false' GRAPHIC_REFERENCES='false' CONSTRAINTS='Symbolic_Association'/>\n");
        pW.write("<LOCALE LANGUAGE_CODE='en' COUNTRY_CODE='US'/>\n");
        pW.write(
            "<CONSTRAINT STEREOTYPE='Time_Subdivision' DESCRIPTION=\"Time subdivision of parent annotation's time interval, no time gaps allowed within this interval\"/>\n");
        pW.write(
            "<CONSTRAINT STEREOTYPE='Symbolic_Subdivision' DESCRIPTION='Symbolic subdivision of a parent annotation. Annotations refering to the same parent are ordered'/>\n");
        pW.write(
            "<CONSTRAINT STEREOTYPE='Symbolic_Association' DESCRIPTION='1-1 association with a parent annotation'/>\n");
        pW.write(
            "<CONSTRAINT STEREOTYPE='Included_In' DESCRIPTION=\"Time alignable annotations within the parent annotation's time interval, gaps are allowed\"/>\n");

        pW.write("</ANNOTATION_DOCUMENT>\n");
        pW.flush();
    }

    private Properties readExtTypes() {
        Properties extTypes = new Properties();
        String[][] extTypePairs = {
                { "abs", "audio/x-mpeg" },
                { "aif", "audio/x-aiff" },
                { "aifc", "audio/x-aiff" },
                { "aiff", "audio/x-aiff" },
                { "asf", "video/x-ms-asf" },
                { "asx", "video/x-ms-asf" },
                { "au", "audio/basic" },
                { "avi", "video/x-msvideo" },
                { "avx", "video/x-rad-screenplay" },
                { "dv", "video/x-dv" },
                { "kar", "audio/midi" },
                { "m3u", "audio/x-mpegurl" },
                { "mid", "audio/midi" },
                { "midi", "audio/midi" },
                { "mov", "video/quicktime" },
                { "movie", "video/x-sgi-movie" },
                { "mp1", "audio/x-mpeg" },
                { "mp2", "audio/mpeg" },
                { "mp3", "audio/mpeg" },
                { "mp4", "video/mp4" },
                { "mpa", "audio/x-mpeg" },
                { "mpe", "video/mpeg" },
                { "mpeg", "video/mpeg" },
                { "mpega", "audio/x-mpeg" },
                { "mpg", "video/mpeg" },
                { "mpv2", "video/mpeg2" },
                { "qt", "video/quicktime" },
                { "smf", "audio/x-midi" },
                { "snd", "audio/basic" },
                { "ulw", "audio/basic" },
                { "wav", "audio/x-wav" },
                { "wmv", "video/x-ms-wmv" }
            };

        for (int i = 0; i < extTypePairs.length; i++) {
            String[] p = extTypePairs[i];
            extTypes.setProperty(p[0], p[1]);
        }

        if (debugOn) {
            pW.println(extTypes.toString());
        }

        return extTypes;
    }

    /**
     * The main to run on the command line.
     *
     * @param args the arguments for the program
     *
     * @throws Exception any exception
     */
    public static void main(String[] args) throws Exception {
        String filePathPrefix = "";

        if (args.length < 1) {
            System.out.println(
                "usage: java Flex2EAF flexFile.xml [timeInMSec [mediaFile [noSynch [renamings [showDebug]]]]]");
            System.out.println(
                " e.g.: java Flex2EAF khinalug.xml 37000 khinalug.mpg word word-txt-en:word,word-txt-ru:word true");
            System.out.println(
                " which uses renaming to merge two tiers into the single tier 'word' and shows debugging output");
            System.out.println(" or  : java Flex2EAF khinalug.xml");
            System.out.println(
                "        which defaults to 300000 msec, i.e. five minutes and elan-example1.mpg");
            System.exit(0);
        }

        Reader reader = null;
        String fileName = args[0];
        int totalTime = 300000; // five minutes, default in milliseconds

        if (args.length > 1) {
            try {
                totalTime = Integer.parseInt(args[1]);
            } catch (Exception ex) {
                System.out.println("total Time in milliseconds=[" + args[1] +
                    "]");
                System.exit(1);
            }
        }

        String mediaFileStr = null;

        if (args.length > 2) {
            mediaFileStr = args[2];
        }

        String noSynchStr = null;

        if (args.length > 3) {
            noSynchStr = args[3];
        }

        String typeRenamings = null;

        if ((args.length > 4) && (args[4].length() > 0)) {
            typeRenamings = args[4];
        }

        boolean showDebug = false;

        if (args.length > 5) {
            showDebug = "true".equalsIgnoreCase(args[5]);
        }

        String filePath = filePathPrefix + fileName;
        reader = new InputStreamReader(new FileInputStream(filePath), "utf-8");

        Writer writer = null;
        PushbackReader pbReader = null;

        try {
			writer = new OutputStreamWriter(new FileOutputStream(filePath +
	                    ".eaf"), "utf-8");

            Flex2EAF f2e = new Flex2EAF(writer, showDebug, totalTime,
                    typeRenamings, mediaFileStr, noSynchStr);
			pbReader = new PushbackReader(reader);
            int first = pbReader.read();

            if ((first != -1) && (first != 0xFEFF)) {
                pbReader.unread(first);
            }

            f2e.parse(new InputSource(pbReader));
        } finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
			}
			try {
				if (pbReader != null) {
					pbReader.close();
				}
			} catch (IOException e) {
			}

        }
    }

    class tierObject {
        public String tierID = "";

        public String parent_ref = "";

        public boolean isAlignable = false;

        public boolean isParent = false;

        public String default_locale = "";

        public String linguistic_type_ref = "";

        public String participant = "";
        private ArrayList<itemObject> items = null;

        /**
         * Creates a new tierObject instance
         *
         * @param xtierID the tier id
         * @param xparent_ref the parent ref
         * @param xisAlignable alignable flag
         * @param xdefault_locale the locale
         * @param xlinguistic_type_ref the tier type
         * @param xparticipant participant
         * @param xisParent the "is parent" flag
         */
        public tierObject(String xtierID, String xparent_ref,
            boolean xisAlignable, String xdefault_locale,
            String xlinguistic_type_ref, String xparticipant, boolean xisParent) {
            tierID = xtierID;
            parent_ref = xparent_ref;
            isAlignable = xisAlignable;
            isParent = xisParent;
            default_locale = xdefault_locale;

            if ((default_locale == null) || default_locale.equals("")) {
                default_locale = "en";
            }

            linguistic_type_ref = xlinguistic_type_ref;

            if ((linguistic_type_ref == null) ||
                    linguistic_type_ref.equals("")) {
                linguistic_type_ref = isAlignable ? "text"
                                                  : (isParent ? "chunk" : "gloss");
            }

            participant = xparticipant;
            items = new ArrayList<itemObject>();
        }

        /**
         *
         * @return a TIER element as string
         */
        @Override
		public String toString() {
            StringBuilder sBuff = new StringBuilder("<TIER TIER_ID='" + tierID +
                    "'");

            if ((parent_ref != null) && !("".equals(parent_ref))) {
                sBuff.append(" PARENT_REF='" + parent_ref + "'");
            }

            sBuff.append(" DEFAULT_LOCALE='" + default_locale + "'");
            sBuff.append(" LINGUISTIC_TYPE_REF='" + linguistic_type_ref + "'");

            if ((participant != null) && (participant.length() > 0)) {
                sBuff.append(" PARTICIPANT='" + participant + "'");
            }

            sBuff.append(">\n");

            for (int i = 0; i < items.size(); i++)
                sBuff.append(items.get(i).toString());

            sBuff.append("</TIER>\n");

            return sBuff.toString();
        }

        /**
         *
         * @param synchedTimeSlots the time slots to add
         */
        public void addTimeSlots(HashSet<Integer> synchedTimeSlots) {
            for (int i = 0; i < items.size(); i++) {
                itemObject item = items.get(i);

                if (item.isAlign) {
                    synchedTimeSlots.add(item.ts1);
                    synchedTimeSlots.add(item.ts2);
                }
            }
        }
    }

    /**
     * Class for item objects.
     */
    class itemObject {
        String itemType = null;

        String itemLang = null;

        public boolean isAlign = false;

        public boolean isParent = false;

        public int ts1;

        public int ts2 = 0;

        public int tierLoc = -1; // position of this item within its tier;

        public String itemID;

        public String refID;

        public String defaultTierID;

        public String tierID;

        public String pTierID = "";

        public String prevAnnotationRefID = null;

        public String prevAnnotationID = null;
        private StringBuilder value = null;

        // public itemObject(boolean xisAlign,int xts1, int xts2, String
        /**
         * Creates a new itemObject instance
         *
         * @param atts the attributes
         */
        public itemObject(Attributes atts) {
            itemType = atts.getValue("type");
            itemLang = atts.getValue("lang");

            String tierPrefix = stack.peek() + "-"; // qname of container
                                                    // element on stack
                                                    //

            defaultTierID = tierPrefix + itemType + "-" + itemLang;

            if (typeRenamings == null) {
                tierID = defaultTierID;
            } else {
                tierID = typeRenamings.getProperty(defaultTierID, defaultTierID);
            }

            //
            try {
                pTierID = parentTiersIDStack.peek(); // candidate parentTier or
                                                     // null; in case of Deqeue
            } catch (EmptyStackException ese) {
                pTierID = null;
            }

            try {
                refID = parentItemIDStack.peek(); // candidate parent Item or
                                                  // null;
            } catch (EmptyStackException ese) {
                refID = null;
            }

            //
            ts1 = timeStack.peek(); // time associated with container element
                                    // entry; ts2 filled in later

            itemID = "a" + newAnnotationID();
            items.put(itemID, this);
            currentItemID = itemID; // global mark, so item value will be added
                                    // to this.value

            value = new StringBuilder();

            // this item is alignable iff it is the first item-type in its
            // container element
            // 20090108: not alignable if it is at or below noSynch, but still
            // behaves as a
            // parent.
            isParent = ((pTierID == null) || !(pTierID.startsWith(tierPrefix))); // either
                                                                                 // text
                                                                                 // or
                                                                                 // chunk

            isAlign = addSynchData && isParent;

            tierObject tOb = tiers.get(tierID);

            if (null == tOb) {
                tOb = new tierObject(tierID, pTierID, isAlign, "", "", "",
                        isParent);
                tierNames.add(tOb.tierID);
                tiers.put(tOb.tierID, tOb);
            }

            tOb.default_locale = itemLang;

            if (isParent) {
                parentTiersIDStack.push(tierID);

                if (debugOn) {
                    pW.write("<PUSHINGTIER>" + tierID + "</PUSHINGTIER>\n");
                }
            }

            tierLoc = tOb.items.size();
            tOb.items.add(this);

            if (isParent) {
                parentItemIDStack.push(itemID);
                tierOfItem.put(itemID, tierID);

                if (debugOn) {
                    pW.write("<PUSHING>" + itemID + "</PUSHING>\n");
                }
            }

            if (!isAlign && isParent && (tierLoc > 0)) { // a "chunk", Symbolic
                                                         // Subdivision

                itemObject prevItem = tOb.items.get(tierLoc - 1);
                // prevItem may actually be of the same type, or may be of a
                // different
                // type mapped into the same type by typeRenamings
                prevAnnotationRefID = prevItem.refID;
                prevAnnotationID = prevItem.itemID;
            }
        }

        /**
         * 
         * @param val the string to append
         */
        public void append(String val) {
            value.append(val);
        }

        /**
         *
         * @return an ANNOTATION XML fragment 
         */
        @Override
		public String toString() {
            StringBuilder sBuff = new StringBuilder("<ANNOTATION>");

            if (isAlign) {
                sBuff.append("<ALIGNABLE_ANNOTATION TIME_SLOT_REF1='ts")
                     .append(ts1).append("' TIME_SLOT_REF2='ts").append(ts2)
                     .append("' ANNOTATION_ID='").append(itemID)
                     .append("'>\n<ANNOTATION_VALUE>").append(value.toString())
                     .append("</ANNOTATION_VALUE></ALIGNABLE_ANNOTATION>\n");
            } else if (refID.equals(prevAnnotationRefID)) { // prev is non-null, so
                                                            // non-first of "chunk"
                sBuff.append("<REF_ANNOTATION ANNOTATION_REF='").append(refID)
                     .append("' PREVIOUS_ANNOTATION='").append(prevAnnotationID)
                     .append("' ANNOTATION_ID='").append(itemID)
                     .append("'>\n<ANNOTATION_VALUE>").append(value.toString())
                     .append("</ANNOTATION_VALUE></REF_ANNOTATION>\n");
            } else {
                sBuff.append("<REF_ANNOTATION ANNOTATION_REF='").append(refID)
                     .append("' ANNOTATION_ID='").append(itemID)
                     .append("'>\n<ANNOTATION_VALUE>").append(value.toString())
                     .append("</ANNOTATION_VALUE></REF_ANNOTATION>\n");
            }

            sBuff.append("</ANNOTATION>\n");

            return sBuff.toString();
        }
    }
}
