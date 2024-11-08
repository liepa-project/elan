/*
 * Created on Sep 24, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package mpi.eudico.server.corpora.clomimpl.shoebox.interlinear;

import java.util.Iterator;
import java.util.List;

import mpi.eudico.server.corpora.clom.Annotation;

/**
 * An interlinear renderer based on {@code byte} positions.
 * 
 * @author hennie
 */
public class ByteRenderer extends Renderer {

	/**
	 * Private constructor, all methods are static.
	 */
	private ByteRenderer() {
		super();
	}

	/**
	 * Creates a layout based on {@code byte} positions.
	 * 
	 * @param metrics the object containing all settings and all annotations
	 *    to be able to create the interlinear layout
	 * @return an array of strings, each string representing a line
	 */
	public static String[] render(Metrics metrics) {
		String[] outputLines = new String[metrics.getMaxVerticalPosition() + 1];

		// initialize
		for (int i = 0; i < outputLines.length; i++) {
			outputLines[i] = "";		
		}
		
//		renderTierLabels(metrics, outputLines);
		renderAnnotationValues(metrics, outputLines);	
		
		return outputLines;	
	}
	
	/**
	 * Renders the tier labels for interlinear lines.
	 * 
	 * @param metrics the object containing all settings and all tiers
	 *    for the creation of the tier labels
	 * @param outputLines the array to add the labels to
	 */
	public static void renderTierLabels(Metrics metrics, String[] outputLines) {
		Integer vPos = null;
		String tierLabel = "";

		List<Integer> vPositions = metrics.getPositionsOfNonEmptyTiers();
	
		Iterator posIter = vPositions.iterator();
		while (posIter.hasNext()) {
			vPos = (Integer) posIter.next();
			tierLabel = metrics.getTierLabelAt(vPos.intValue());			
									
			outputLines[vPos.intValue()] += tierLabel + " ";
		}
	}
	
	/**
	 * Renders the annotation values in the interlinear layout.
	 *  
	 * @param metrics the object containing all settings and all annotations
	 *    to be able to create the interlinear layout
	 * @param outputLines the interlinear lines to add the annotations to
	 */
	public static void renderAnnotationValues(Metrics metrics, String[] outputLines) {		
		List<Annotation> annots = metrics.getBlockWiseOrdered();

		Iterator<Annotation> annIter = annots.iterator();
		while (annIter.hasNext()) {
			Annotation a = annIter.next();
			int vPos = metrics.getVerticalPosition(a);
			int hPos = metrics.getHorizontalPosition(a);
			
			String tierLabel = metrics.getTierLabelAt(vPos);
			if (!(metrics.getInterlinearizer().getCharEncoding(tierLabel) == Interlinearizer.UTF8)) {
				outputLines[vPos] += nSpaces(hPos - outputLines[vPos].length()) + a.getValue();
			}
			else {
				outputLines[vPos] += nSpaces(hPos - 
						SizeCalculator.getNumOfBytes(outputLines[vPos])) + a.getValue();
			}
		}		
	}
	
	private static String nSpaces(int n) {
		String ret = "";
		for (int i = 0; i < n; i++) {
			ret += " ";
		}
		return ret;
	}
}