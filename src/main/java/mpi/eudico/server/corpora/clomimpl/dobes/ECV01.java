package mpi.eudico.server.corpora.clomimpl.dobes;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Creates an ECV document
 * 
 * @author Micha Hulsbosch
 * @version jul 2010
 */
public class ECV01 {
	/** the Document object */
	protected Document doc;
	
    /**
     * Creates an empty document
     * 
     * @throws Exception any document building exception
     */
    public ECV01() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder        db  = dbf.newDocumentBuilder();
		this.doc = db.newDocument();
    }

	/** 
	 * Returns the document element.
	 * 
	 * @return the document or root element
	 * @see Document#getDocumentElement()
	 */
	public Element getDocumentElement() { return this.doc.getDocumentElement(); }

	/** 
	 * Adds  and returns the specified node.
	 * 
	 * @param e the node to add
	 * @return the appended node
	 * @see Element#appendChild(Node) 
	 */
	public Node appendChild(Node e) { return this.doc.appendChild(e); }

	/**
	 * Creates a new external CV document element
	 * 
	 * @param creationDate the date
	 * @param author the author or owner
	 * @param version the version number
	 * @return result (Element)
	 */
	public Element newExternalCVDocument
	(String creationDate, String author, String version) {
		// these attributes are all optional
		//if (creationDate == null) throw new RuntimeException("ECV");
		//if (author == null) throw new RuntimeException("ECV");
		//if (version == null) throw new RuntimeException("ECV");
		Element result = this.doc.createElement("CV_RESOURCE");
		result.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		result.setAttribute("xsi:noNamespaceSchemaLocation", "http://www.mpi.nl/tools/elan/EAFv2.7.xsd");
		if (creationDate != null) {
			result.setAttribute("DATE",   creationDate);
		}
		if (author != null) {
			result.setAttribute("AUTHOR", author);
		}
		if (version != null) {
			result.setAttribute("VERSION", version);
		} else {
			result.setAttribute("VERSION", "0.1");// default
		}
		//result.setAttribute("FORMAT", "0.1");
		return result;
	}

//	?? Micha: See whether to use the next to method	
//
//	public Element newTempExternalCVDocument
//	(String creationDate, String author, String version, String url) {
//		if (url == null) throw new RuntimeException("ECV");
//		Element result = newExternalCVDocument(creationDate, author, version);
//		result.setAttribute("URL", url);
//		return result;
//	}
//
//	public Element newLocale (Locale l){
//		if (l == null) throw new RuntimeException("ECV");
//		Element result = this.doc.createElement("LOCALE");
//		result.setAttribute("LANGUAGE_CODE", l.getLanguage());
//		if (!l.getCountry().equals("")) result.setAttribute("COUNTRY_CODE", l.getCountry());
//		if (!l.getVariant().equals("")) result.setAttribute("VARIANT", l.getVariant());
//		return result;
//	}

	/**
	 * Creates a new CV element
	 * 
	 * @param conVocId the id of the CV
	 * @param description the description
	 * @return result (Element)
	 */
	public Element newControlledVocabulary (String conVocId, String description) {
		if (conVocId == null) throw new RuntimeException("ECV");
		//if (name == null) throw new RuntimeException("ECV"); //name not supported yet
		Element result = this.doc.createElement("CONTROLLED_VOCABULARY");
		result.setAttribute("CV_ID", conVocId);
		//result.setAttribute("NAME", name);
		if (description != null) {
			result.setAttribute("DESCRIPTION", description);
		}
		return result;
	}

	/**
	 * Creates a new CVEntry element
	 * 
	 * @param id the id of the entry
	 * @param value the value of the entry
	 * @param description the description
	 * @param extRef an external reference
	 * @return result (Element)
	 */
	public Element newCVEntry(String id, String value, String description, String extRef) {
		if (value == null) throw new RuntimeException("ECV");
		// TO DO add EXT REF
		Element result = this.doc.createElement("CV_ENTRY");
		result.appendChild(doc.createTextNode(value));
		result.setAttribute("CVE_ID", id);
		if (description != null) {
			result.setAttribute("DESCRIPTION", description);
		}
		if (extRef != null) {
			result.setAttribute("EXT_REF", extRef);
		}

		return result;
	}
	
	/**
	 * Creates a new External Reference element
	 * 
	 * @param id the id attribute
	 * @param type the external reference type attribute
	 * @param value the value attribute
	 * @return result (Element)
	 */
	public Element newExternalReference(String id, String type, String value) {
    	if (id == null || type == null || value == null) throw new RuntimeException("ECV");
    	
    	Element result = this.doc.createElement("EXTERNAL_REF");
    	result.setAttribute("EXT_REF_ID", id);
    	result.setAttribute("TYPE", type);
    	result.setAttribute("VALUE", value);
    	
    	return result;
    }
}
