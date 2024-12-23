package mpi.eudico.server.corpora.clomimpl.dobes;

import org.w3c.dom.Element;

/**
 * EAF 2.6 extends previous versions by:
 * - adding an EXTERNAL_REF element which can be used for references to ISOcat data categories
 * (and maybe later to other types of external resources) 
 * - adding an EXT_REF attribute to annotations, linguistic types and CV entries
 * 
 * See {@link EAFBase} The methods there are no longer final (as in its predecessors) so that the 
 * it can be extended
 * @version EAF 2.6 
 * (May 2008: added support for references to concepts in the ISO Data Category Registry.
 * This applies to annotations (alignable and ref), CV entries and Linguistic Types.)
*/
public class EAF26 extends EAFBase{
	/** the schema location for EAF 2.6 */
	public static final String EAF26_SCHEMA_LOCATION = "http://www.mpi.nl/tools/elan/EAFv2.6.xsd";
	/** the local path to the schema */
	public static final String EAF26_SCHEMA_RESOURCE = "/mpi/eudico/resources/EAFv2.6.xsd";

    /**
     * Constructor. Sets the format attribute to 2.6 and the schema location to the location 
	 * of the EAF 2.6 xsd. 
	 * 
	 * @throws Exception any checked exception that can occur in the process of
     * building a {@code Document}
 	 */
    public EAF26() throws Exception {
    	super();
    	EAF_Format = "2.6";
    	EAF_Schema_Location = EAF26.EAF26_SCHEMA_LOCATION;
    }

	/**
	 * Adds an EXT_REF attribute to an alignable annotation as produced by EAFBase.
	 *
	 * @param id the ANNOTATION_ID attribute
	 * @param beginTimeSlot the TIME_SLOT_REF1 reference attribute
	 * @param endTimeSlot the TIME_SLOT_REF2 reference attribute
	 * @param extRefId the reference to the id of an EXTERNAL_REF element
	 * 
	 * @return a new ALIGNABLE_ANNOTATION element
	 * 
	 * @see EAFBase#newAlignableAnnotation(String, String, String) 
	 */
	public Element newAlignableAnnotation
		(String id,
		 String beginTimeSlot,
		 String endTimeSlot,
		 String extRefId) {
		Element result = super.newAlignableAnnotation(id, beginTimeSlot, 
				endTimeSlot);
		attributeIfNotEmpty(result, "EXT_REF", extRefId);
		return result;
    }
	
	/**
	 * Adds an EXT_REF attribute to an reference annotation as produced by EAFBase.
	 * 
	 * @param id the ANNOTATION_ID attribute
	 * @param annotationRef the ANNOTATION_REF attribute, a reference to a parent 
	 * annotation, not null
	 * @param previousAnnotation the PREVIOUS_ANNOTATION attribute, can be null 
	 * @param extRefId the reference to the id of an EXTERNAL_REF element
	 * 
	 * @return a new REF_ANNOTATION element
	 * 
	 * @see EAFBase#newRefAnnotation(String, String, String)
	 */
	public Element newRefAnnotation
		(String id,
		 String annotationRef,
		 String previousAnnotation,
		 String extRefId) {
		Element result = super.newRefAnnotation(id, annotationRef, 
				previousAnnotation);
		attributeIfNotEmpty(result, "EXT_REF", extRefId);
		return result;
    }

	/**
	 * Adds an EXT_REF attribute to a linguistic type element as produced by EAFBase.
	 * 
	 * @param id the LINGUISTIC_TYPE_ID attribute, the name of the type
	 * @param timeAlignable the TIME_ALIGNABLE attribute
	 * @param constraint a reference to a CONSTRAINT element, the CONSTRAINTS attribute
	 * @param controlledVocabularyName the name of the CV reference, the 
	 * CONTROLLED_VOCABULARY_REF attribute, can be null
	 * @param extRefId the ref to the id of an EXTERNAL_REF element
	 *
	 * @return a new LINGUISTIC_TYPE element
	 * 
	 * @see EAFBase#newLinguisticType(String, boolean, String, String)
	 */
	public Element newLinguisticType (String id, boolean timeAlignable, 
		String constraint, String controlledVocabularyName, String extRefId){
		Element result = super.newLinguisticType(id, timeAlignable, constraint,
				controlledVocabularyName);
		attributeIfNotEmpty(result, "EXT_REF", extRefId);
		return result;
    }
    
	/**
	 *  Adds an EXT_REF attribute to a CV entry element as produced by EAFBase.
	 * 
	 * @param value the value of the CVEntry
	 * @param description the description of the entry, can be null
	 * @param extRef the reference to the id of an EXTERNAL_REF element
	 * 
	 * @return a new CV_ENTRY Element 
	 */
    public Element newCVEntry(String value, String description, String extRef) {
    	Element result = super.newCVEntry(value, description);
    	//if (value == null) throw new RuntimeException("EAF");//TODO has already been checked?
		attributeIfNotEmpty(result, "EXT_REF", extRef);
		return result;
    }

    /**
     * Creates an element for external references, references to externally defined concepts or resources.
     * 
     * @param id the ID of the element
     * @param type the type of reference, one of the types listed in the eaf schema, e.g. "iso12620" 
     * @param value the value of the reference, e.g. a pid or urid of a Data Category. The combination 
     * of type and value must be sufficient to know how to treat the reference.
     * 
     * @return a new EXTERNAL_REF element
     */
    public Element newExternalReference(String id, String type, String value) {
    	if (id == null || type == null || value == null) throw new RuntimeException("EAF");
    	
    	Element result = this.doc.createElement("EXTERNAL_REF");
    	result.setAttribute("EXT_REF_ID", id);
    	result.setAttribute("TYPE", type);
    	result.setAttribute("VALUE", value);
    	
    	return result;
    }

}
