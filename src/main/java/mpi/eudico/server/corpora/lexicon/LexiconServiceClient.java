package mpi.eudico.server.corpora.lexicon;

import java.util.ArrayList;

import javax.swing.tree.TreeModel;


/**
 * Interface for a Lexicon Service Client that is generated by 
 * a factory from a Lexicon Service extension.
 * 
 * @author Micha Hulsbosch
 */
public interface LexiconServiceClient {
	/**
	 * Returns the client type string.
	 * 
	 * @return the client type
	 */
	public String getType();
	
	/**
	 * Returns the description of the service client.
	 * 
	 * @return a description of the client
	 */
	public String getDescription();
	
	/**
	 * Sets the URL of the lexicon web service.
	 * 
	 * @param lexiconWebserviceUrl the web service URL
	 */
	public void setUrl(String lexiconWebserviceUrl);
	
	/**
	 * Returns the web service URL.
	 * 
	 * @return the URL of the web service
	 */
	public String getUrl();

	/**
	 * Sets the name of the user of the service.
	 * 
	 * @param username the user name
	 */
	public void setUsername(String username);
	
	/**
	 * Returns the name of the user of the service.
	 *  
	 * @return the user name
	 */
	public String getUsername();

	/**
	 * Sets the user's password for the service.
	 * 
	 * @param password the password
	 */
	public void setPassword(String password);

	/**
	 * Returns the user's password for the service.
	 * 
	 * @return the password
	 */
	public String getPassword();
	
	/**
	 * Returns a list of lexicon identifiers.
	 * 
	 * @return a list of lexicon identifiers
	 * @throws LexiconServiceClientException if an error occurs when loading
	 * the identifiers
	 */
	public ArrayList<LexiconIdentification> getLexiconIdentifications() throws LexiconServiceClientException;
	
	/**
	 * Returns the lexicon for the specified identifier.
	 * 
	 * @param lexId the lexicon identifier
	 * @return the lexicon object
	 * @throws LexiconServiceClientException if an error occurs loading the
	 * lexicon
	 */
	public Lexicon getLexicon(LexiconIdentification lexId) throws LexiconServiceClientException;
	
	/**
	 * Returns a list of lexicon entry identifiers.
	 * 
	 * @param lexId the lexicon identifier
	 * @return a list of entry field identifiers
	 * @throws LexiconServiceClientException if an error occurs loading the 
	 * identifiers
	 */
	public ArrayList<LexicalEntryFieldIdentification> getLexicalEntryFieldIdentifications(LexiconIdentification lexId)  throws LexiconServiceClientException;
	
	/**
	 * Returns a tree model of the structure of the lexical entries.
	 * 
	 * @param lexId the lexicon identifier
	 * @return the structure of lexical entries as a tree model
	 * @throws LexiconServiceClientException if an error occurs loading the
	 * lexicon or its structure
	 */
	public TreeModel getLexicalEntryStructure(LexiconIdentification lexId) throws LexiconServiceClientException;
	
	/**
	 * Returns a list of search constraints.
	 * 
	 * @return a list of search constraints
	 */
	public ArrayList<String> getSearchConstraints();
	
	/**
	 * Returns a lexicon based on a search query.
	 * 
	 * @param lexId the lexicon identifier
	 * @param fldId the entry field identifier
	 * @param constraint search constraints
	 * @param searchString query string
	 * @return a lexicon object
	 * @throws LexiconServiceClientException if an error occurs when executing
	 * the query
	 */
	public Lexicon search(LexiconIdentification lexId, LexicalEntryFieldIdentification fldId, String constraint, String searchString) throws LexiconServiceClientException;
}