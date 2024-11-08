package mpi.search.content.result.model;

/**
 * A test content match with fixed values.
 * 
 * Created on Jul 30, 2004
 * @author Alexander Klassmann
 * @version Jul 30, 2004
 */
@SuppressWarnings("serial")
public class ExampleMatch implements ContentMatch {
	/** Creates a new match. */
	public ExampleMatch() {
		super();
	}

	@Override
	public long getBeginTimeBoundary() {
		return 1234l;
	}

	@Override
	public long getEndTimeBoundary() {
		return 9876l;
	}

	@Override
	public String getValue() {
		return "This is an example value!";
	}

	@Override
	public String getTierName() {
		return "ExampleTier1";
	}

	@Override
	public String getFileName() {
		return "No file name yet";
	}

	@Override
	public String getLeftContext() {
		return "Some left context";
	}

	@Override
	public String getRightContext() {
		return "Some right context";
	}
	//mod. Coralie Villes
	@Override
	public String getChildrenContext() {
		return "child1 child2";
	}
	
	@Override
	public String getParentContext() {
		return "parent";
	}

	@Override
	public int getIndex() {
		return 1;
	}

	@Override
	public int[][] getMatchedSubstringIndices() {
		return new int[][] { { 9, 15 }};
	}
	
	/**
	 * Returns the id.
	 * 
	 * @return the id
	 */
	public String getId(){
		return "007";
	}

}
