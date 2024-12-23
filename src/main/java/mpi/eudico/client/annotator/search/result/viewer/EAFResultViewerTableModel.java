/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package mpi.eudico.client.annotator.search.result.viewer;

import mpi.eudico.client.annotator.grid.GridViewerTableModel;
import mpi.eudico.client.annotator.search.result.model.AnnotationMatch;
import mpi.eudico.client.annotator.search.result.model.ElanMatch;
import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clom.AnnotationCore;

import mpi.search.content.result.model.ContentMatch;


/**
 * The result viewer table model.
 *
 * @author Alexander Klassmann
 * @version Aug 24, 2004
 */
@SuppressWarnings("serial")
public class EAFResultViewerTableModel extends GridViewerTableModel {
	private int firstRealIndex = 0;
	
    /**
     * Creates a new EAFResultViewerTableModel object.
     * Adds a number of columns.
     */
    public EAFResultViewerTableModel() {
        usedColumnNames.add(2, GridViewerTableModel.FILENAME);
        usedColumnNames.add(3, GridViewerTableModel.TIERNAME);
        usedColumnNames.add(4, GridViewerTableModel.LEFTCONTEXT);
        usedColumnNames.add(6, GridViewerTableModel.RIGHTCONTEXT);
        //add column mod. Coralie Villes
        usedColumnNames.add(7, GridViewerTableModel.PARENT);
        usedColumnNames.add(8, GridViewerTableModel.CHILD);
    }

    /**
     * Returns the annotation from the specified row.
     *
     * @param row the row index
     *
     * @return the annotation at that row
     */
    @Override
	public AnnotationCore getAnnotationCore(int row) {
        AnnotationCore annotationCore = super.getAnnotationCore(row);

        return (annotationCore instanceof ElanMatch) ? ((ElanMatch) annotationCore).getAnnotation()
                                                     : annotationCore;
    }
    
    /**
     * This method first calls the {@code super} implementation and if 
     * not found, tries to find the annotation contained in one of the
     * {@code ElanMatch}es (if the model contains matches).
     * 
     * @param annotationCore the annotation to find the row index for
     *   
     * @return the row of the match or annotation in the model, or -1
     */
    @Override
	public int getRowForAnnotation(AnnotationCore annotationCore) {
		int row = super.getRowForAnnotation(annotationCore);
		if (row > -1) {
			return row;
		} else {
			// the annotation parameter might not be the same object as the matches in the model
			for (AnnotationCore ac : annotations) {
				if (ac instanceof ElanMatch) {
					if (((ElanMatch) ac).getAnnotation() == annotationCore) {
						return annotations.indexOf(ac);
					}
				}
			}
		}
		
		return row;
	}

	/**
     * Returns whether or not the cell at the given row and column can be edited.
     *
     * @param row the row index
     * @param column the column index
     *
     * @return {@code true} if the cell can be edited, {@code false} otherwise
     */
    @Override
	public boolean isCellEditable(int row, int column) {
    	Object value = getValueAt(row, column);
        return value instanceof ElanMatch || value instanceof Annotation;
    }

    /**
     * Sets the index of the first row containing real data.
     * 
     * @param i the row index
     */
    public void setFirstRealIndex(int i){
    		firstRealIndex = i;
    }
    
    /**
     * Returns the value for the table cell at the specified row and column.
     *
     * @param row the row index
     * @param column the column index
     *
     * @return the value that has to be rendered
     */
    @Override
	public Object getValueAt(int row, int column) {
    	Object value = annotations.get(row);
        if (value instanceof ContentMatch) {
            ContentMatch match = (ContentMatch) value;

            String cn = getColumnName(column);

            if (cn.equals(COUNT)) {
                return "" + (firstRealIndex + row + 1);
           }

            if (cn.equals(GridViewerTableModel.ANNOTATION)) {
                return match;
            }

            if (cn.equals(GridViewerTableModel.FILENAME)) {
                return match.getFileName();
            }

            if (cn.equals(GridViewerTableModel.TIERNAME)) {
                return match.getTierName();
            }

            if (cn.equals(GridViewerTableModel.LEFTCONTEXT)) {
            	if (match instanceof AnnotationMatch) {
                    Annotation a = ((AnnotationMatch)match).getLeftContextAnnotation();
                    if (a != null) {
                    	return a;
                    }
            	}
           		return match.getLeftContext();
            }

            if (cn.equals(GridViewerTableModel.RIGHTCONTEXT)) {
            	if (match instanceof AnnotationMatch) {
            		Annotation a = ((AnnotationMatch)match).getRightContextAnnotation();
                    if (a != null) {
                    	return a;
                    }
            	}
            	return match.getRightContext();
            }
            
            if (cn.equals(GridViewerTableModel.PARENT)) {
            	if (match instanceof AnnotationMatch) {
            		Annotation a = ((AnnotationMatch)match).getParentContextAnnotation();
                    if (a != null) {
                    	return a;
                    }
            	} 
            	return match.getParentContext();
            }
            
            if (cn.equals(GridViewerTableModel.CHILD)) {
            	return match.getChildrenContext();
            }
        }

        return super.getValueAt(row, column);
    }
}
