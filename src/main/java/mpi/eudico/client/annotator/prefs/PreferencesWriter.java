package mpi.eudico.client.annotator.prefs;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import static mpi.eudico.client.annotator.util.ClientLogger.LOG;
import mpi.eudico.util.IoUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Encodes and saves the preferences in an xml file.
 *
 * @author Han Sloetjes
 * @version 1.0
 * @version 1.1 the VERSION has been incremented to 1.1
 * prefGroups can now be nested and a prefList is allowed as child of prefGroup
 */
public class PreferencesWriter implements PrefConstants {
    private DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    private PrefObjectConverter poConverter;

    /** Holds value of the current version */
    public final String VERSION = "1.1";

    /**
     * Constructor. Instantiates the DocumentBuilder.
     */
    public PreferencesWriter() {
        super();
        poConverter = new PrefObjectConverter();

        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
        } catch (FactoryConfigurationError fce) {
        	LOG.severe("Unable to create a Document Builder: " +
                fce.getMessage());
        } catch (ParserConfigurationException pce) {
        	LOG.severe("Unable to create a Document Builder: " +
                pce.getMessage());
        }
    }

    /**
     * Creates the DOM for the objects in the prefs map and stores in the
     * specified location.
     *
     * @param prefs the preferences objects
     * @param path the file path
     */
    public synchronized void encodeAndSave(Map<String, ?> prefs, String path) {
        if ((prefs == null) || (path == null)) {
            return;
        }

        Element root = createDOM(prefs);

        try {
        	LOG.info("Writing preferences: " + path);
            IoUtil.writeEncodedFile("UTF-8", path, root);
        } catch (IOException ioe) {
        	LOG.severe("Could not save the preferences xml file to: " + path +
                "\n" + " Cause: " + ioe.getMessage());
        } catch (Exception e) {
        	LOG.severe("Could not save the preferences xml file to: " + path +
                "\n" + " Cause: " + e.getMessage());
        }
    }

    /**
     * Creates a Document object, iterates over the key-value pairs in the map
     * and returns the root element.
     *
     * @param prefs the preferences objects
     *
     * @return the document element
     */
    private Element createDOM(Map<String, ?> prefs) {
        if (db != null) {
            Document doc = db.newDocument();
            Element root = doc.createElement("preferences");
            root.setAttribute("xmlns:xsi",
                "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xsi:noNamespaceSchemaLocation",
                "http://www.mpi.nl/tools/elan/Prefs_v1.1.xsd");
            root.setAttribute(VERS_ATTR, VERSION);
            doc.appendChild(root);

            // iterate over the objects in the prefs map and create and add elements 
            for (Map.Entry<String, ? extends Object> prefEntry : prefs.entrySet()) {
            	String key = prefEntry.getKey();
            	Object value = prefEntry.getValue();

                if ((key != null) && (value != null)) {
                	Element nextElem = createPrefElement(doc, key, value);

                    if (nextElem != null) {
                        root.appendChild(nextElem);
                    }
                }                
            }

            return root;
        }

        return null;
    }

    /**
     * Creates  a <code>pref</code>, <code>prefGroup</code> or
     * <code>prefList</code>  Element, depending on the nature of the
     * specified value.
     *
     * @param doc the DOM document
     * @param key the preferences key attribute
     * @param value a Map, List or a single Object to store
     *
     * @return the element
     */
    private Element createPrefElement(Document doc, String key, Object value) {
        Element pref = null;

        if (value instanceof Map) {
            pref = doc.createElement(PREF_GROUP);
            pref.setAttribute(KEY_ATTR, key);

            // add the key-value pairs as pref elements
            for (Map.Entry<?, ?> keyEntry : ((Map<?, ?>) value).entrySet()) {
            	String childKey = (String) keyEntry.getKey();
            	Object obj = keyEntry.getValue();

                if (obj != null) {
                	Element childElem = createPrefElement(doc, childKey, obj);

                    if (childElem != null) {
                        pref.appendChild(childElem);
                    }
                }
            }
        } else if (value instanceof List) {
            pref = doc.createElement(PREF_LIST);
            pref.setAttribute(KEY_ATTR, key);

            // add the values as the know object types elements
            for (Object obj : (List<?>) value) {                
            	Element childElem = createContentElement(doc, obj);

                if (childElem != null) {
                    pref.appendChild(childElem);
                }
            }
        } else if (value instanceof Object[]) {
            pref = doc.createElement(PREF_LIST);
            pref.setAttribute(KEY_ATTR, key);

            // add the values as the know object types elements
            for (Object element : (Object[]) value) {
            	Element childElem = createContentElement(doc, element);

                if (childElem != null) {
                    pref.appendChild(childElem);
                }
            }
        } else {
            pref = doc.createElement(PREF);
            pref.setAttribute(KEY_ATTR, key);

            Element childElem = createContentElement(doc, value);

            if (childElem != null) {
                pref.appendChild(childElem);
            }
        }

        return pref;
    }

    /**
     * Creates the content element which is one of the primitives wrappers, a
     * String or Object element.
     *
     * @param doc the DOM document
     * @param value a Map, List or a single Object to store
     *
     * @return the content element
     */
    private Element createContentElement(Document doc, Object value) {
        if (value == null) {
            return null;
        }

        Element objPref = null;

        if (value instanceof String) {
            objPref = doc.createElement(STRING);
            objPref.appendChild(doc.createTextNode((String) value));
        } else if (value instanceof Boolean) {
            objPref = doc.createElement(BOOLEAN);
            objPref.appendChild(doc.createTextNode(((Boolean) value).toString()));
        } else if (value instanceof Integer) {
            objPref = doc.createElement(INT);
            objPref.appendChild(doc.createTextNode(((Integer) value).toString()));
        } else if (value instanceof Long) {
            objPref = doc.createElement(LONG);
            objPref.appendChild(doc.createTextNode(((Long) value).toString()));
        } else if (value instanceof Float) {
            objPref = doc.createElement(FLOAT);
            objPref.appendChild(doc.createTextNode(((Float) value).toString()));
        } else if (value instanceof Double) {
            objPref = doc.createElement(DOUBLE);
            objPref.appendChild(doc.createTextNode(((Double) value).toString()));
        } else {
            objPref = doc.createElement(OBJECT);
            objPref.setAttribute(CLASS_ATTR, value.getClass().getName());
            objPref.appendChild(doc.createTextNode(poConverter.objectToString(
                        value)));
        }

        return objPref;
    }
}
