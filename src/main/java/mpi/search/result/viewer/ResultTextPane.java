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
package mpi.search.result.viewer;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * A text pane for showing search results.
 * 
 * @author klasal
 */
@SuppressWarnings("serial")
public class ResultTextPane extends JTextPane {
    /** style of normal text */
    protected final Style regular;

    /** style of highlighted text */
    protected final Style highlighted;

    /**
     * Creates a new SimpleResultTextPane object.
     */
    public ResultTextPane() {
        setEditable(false);
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(
                StyleContext.DEFAULT_STYLE);
        regular = addStyle("regular", defaultStyle);
        highlighted = addStyle("highlight", regular);
    }

    /**
     * Appends string without highlighting anything.
     * 
     * @param s the string to append
     */
    public void appendString(String s) {
        insertString(getDocument().getLength(), s);
    }

    /**
     * Inserts a string at the specified position.
     * 
     * @param startPosition the position where to insert
     * @param s the string to insert
     */
    protected void insertString(int startPosition, String s) {
        try {
            getDocument().insertString(startPosition, s, regular);
        } catch (BadLocationException e) {
            ;
        }
    }

    /**
     * Append String in ResultPane and highlights the characters between begin
     * and end-positions (in order to show matches subparts of a string)
     * 
     * Note that highlight style has to be set explicitly. Default value is the
     * normal style.
     * 
     * @param s the string to append
     * @param highlights array of begin positions
     * 
     * @throws IndexOutOfBoundsException if the highlights are out of bound
     */
    public void appendString(String s, int[][] highlights) throws IndexOutOfBoundsException {
        insertString(getDocument().getLength(), s, highlights);
    }
    
    /**
     * Same as append, but on arbitrary position in document.
     * 
     * @param startPosition the insert index 
     * @param s the source string
     * @param highlights the highlights array
     */
    protected void insertString(int startPosition, String s, int[][] highlights){
        if (highlights != null && arrayIsConsistent(s, highlights)) {
            try {
                Document doc = getDocument();
                String substring;
                if (highlights.length > 0) {
                    for (int j = 0; j < highlights.length; j++) {
                        substring = s.substring((j == 0) ? 0
                                : highlights[j - 1][1], highlights[j][0]);
                        
                        doc.insertString(startPosition, substring, regular);
                        startPosition += substring.length();
                        
                        substring = s.substring(highlights[j][0],
                                highlights[j][1]);
                        doc.insertString(startPosition, substring, highlighted);
                        startPosition += substring.length();                        
                    }

                    doc.insertString(startPosition, s
                            .substring(highlights[highlights.length - 1][1]), regular);
                }
                else {
                    doc.insertString(startPosition, s, regular);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        else {
            insertString(startPosition, s);
        }
    }

    /**
     * @param s the string to check
     * @param highlights the highlights in the string
     * @return {@code true} if the highlights consistent and within the string's
     * bounds
     */
    private boolean arrayIsConsistent(String s, int[][] highlights) {
        for (int j = 0; j < highlights.length; j++) {
            if ((highlights[j][0] < 0) || (highlights[j][0] > highlights[j][1])
                    || (highlights[j][1] > s.length())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resets the result panel to progress(0), hits(0) and no text in the result
     * panel.
     */
    public void reset() {
        try {
            getDocument().remove(0, getDocument().getLength());
        } catch (BadLocationException e) {
            ;
        }
    }

    /**
     * Sets the color for background highlighting.
     * 
     * @param color the new highlight background color
     */
    public void setHighlightedBackground(Color color) {
        StyleConstants.setBackground(highlighted, color);
    }

    /**
     * Sets the color for foreground highlighting.
     * 
     * @param color the new highlight foreground color
     */
    public void setHighlightedForeground(Color color) {
        StyleConstants.setForeground(highlighted, color);
    }

    /**
     * Sets whether highlighted text should be in bold font style.
     * 
     * @param b the bold flag
     */
    public void setHighlightedBold(boolean b) {
        StyleConstants.setBold(highlighted, b);
    }
 
}
