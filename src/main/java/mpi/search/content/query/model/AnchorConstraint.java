package mpi.search.content.query.model;

import java.util.Map;

/**
 * Contains the anchor constraint of a composite query
 *
 * @author Alexander Klassmann
 * @version July 2004
 */
public class AnchorConstraint extends AbstractConstraint {
    /**
     * Creates a new AnchorConstraint object.
     */
    public AnchorConstraint() {
    }

    /**
     * Creates a new AnchorConstraint object.
     *
     * @param tierNames the tier names
     */
    public AnchorConstraint(String[] tierNames) {
        this.tierNames = tierNames;
    }

    /**
     * Minimal constructor; used by multiple file search
     *
     * @param tierName the tier name
     * @param patternString the search pattern
     * @param isRegEx whether this is a regular expression
     * @param isCaseSensitive whether the search is case sensitive
     */
    public AnchorConstraint(String tierName, String patternString,
        boolean isRegEx, boolean isCaseSensitive) {
        this(tierName, patternString, 0L, 0L, "", isRegEx, isCaseSensitive, null);
    }

    /**
     * 'old' constructor; used by corex
     *
     * @param tierName constraint number within a query
     * @param patternString string/regular expression to be searched
     * @param lowerBoundary negative number (of units) (e.g. 0, -1, -2, ... -X)
     * @param upperBoundary positive number (of units) (e.g. 0, 1, 2 ... +X)
     * @param unit search unit which defines scope to be searched (in respect
     *        to referential constraint)
     * @param isRegEx is pattern regular expression (or string) ?
     * @param isCaseSensitive is pattern case sensitive ?
     * @param attributes should contain (as strings) attribute names (key) and
     *        values (value)
     */
    public AnchorConstraint(String tierName, String patternString,
        long lowerBoundary, long upperBoundary, String unit, boolean isRegEx,
        boolean isCaseSensitive, Map<String, String> attributes) {
        super(new String[] { tierName }, patternString, lowerBoundary,
            upperBoundary, unit, isRegEx, isCaseSensitive, attributes);
    }

    /**
     * Constructor.
     * @param tierNames constraint tiers within a query
     * @param patternString string/regular expression to be searched
     * @param lowerBoundary negative number (of units) (e.g. 0, -1, -2, ... -X)
     * @param upperBoundary positive number (of units) (e.g. 0, 1, 2 ... +X)
     * @param unit search unit which defines scope to be searched (in respect
     *        to referential constraint)
     * @param isRegEx is pattern regular expression (or string) ?
     * @param isCaseSensitive is pattern case sensitive ?
     * @param attributes should contain (as strings) attribute names (key) and
     *        values (value)
     * @see AbstractConstraint#AbstractConstraint(String[], String, long, long, String, boolean, boolean, Map)
     */
    public AnchorConstraint(String[] tierNames, String patternString,
        long lowerBoundary, long upperBoundary, String unit, boolean isRegEx,
        boolean isCaseSensitive, Map<String, String> attributes) {
        super(tierNames, patternString, lowerBoundary, upperBoundary, unit,
            isRegEx, isCaseSensitive, attributes);
    }

    /**
     * @param object the object to test
     *
     * @return {@code true} if the other AnchorConstraint is equal to this one
     */
    @Override
	public boolean equals(Object object) {
    	if (object == this) {
    		return true;
    	}
    	
        if (!(object instanceof AnchorConstraint)) {
            return false;
        }

        //System.out.println("Tiers equal: "+ ((AnchorConstraint) object).getTierNames().equals(
        //       getTierNames()));
        AnchorConstraint constraint = (AnchorConstraint) object;

        if (constraint.getTierNames().length != getTierNames().length) {
            return false;
        }

        for (int i = 0; i < getTierNames().length; i++) {
            if (!getTierNames()[i].equals(constraint.getTierNames()[i])) {
                return false;
            }
        }

        //System.out.println("TierNames equal");
        return super.equals(object);
    }

    /*
     * See Javadoc comment of super.hashCode() 
     */
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
