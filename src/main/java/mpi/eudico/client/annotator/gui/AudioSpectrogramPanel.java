package mpi.eudico.client.annotator.gui;

import mpi.eudico.client.annotator.Constants;
import mpi.eudico.client.annotator.Selection;
import mpi.eudico.client.annotator.viewer.TimeRuler;
import mpi.eudico.util.TimeFormatter;
import nl.mpi.media.spectrogram.SpectrogramSettings;

import javax.swing.*;
import java.awt.*;

/**
 * The actual JPanel for rendering the audio spectrogram
 *
 * @author Allan van Hulst
 * @version 1.0
 */
public class AudioSpectrogramPanel extends JPanel {
    private int[] audioSamples;
    private double[][] freqData;
    private SpectrogramSettings settings;
    private Selection selection;
    private long intervalStart;
    private long intervalEnd;

    // border around the actual image, for frequency labels (left),
    // selection knobs (bottom), time scale (top), ? (right)
    // all painted in paintComponent, or on separate panels in a
    // border layout?
    private Insets insets;
    private TimeRuler ruler;
    private final double msPerPixel = 10d;

    /**
     * No argument constructor
     */
    public AudioSpectrogramPanel() {
        super(null);
        initPanel();
    }


    /**
     * Constructor to  audio samples, frequency data, settings, selection, interval start and interval end variables
     *
     * @param audioSamples the audio samples array
     * @param freqData frequency data array
     * @param settings spectrogram settings
     * @param selection selection object, the currently selected interval
     * @param intervalStart interval start time
     * @param intervalEnd interval end time
     */
    public AudioSpectrogramPanel(int[] audioSamples, double[][] freqData, SpectrogramSettings settings, Selection selection,
                                 long intervalStart, long intervalEnd) {
        super(null); // null layout
        // store/set local references
        // wait for setVisible and/or setSize before generating the image
        showSpectrogram(audioSamples, freqData, settings, selection, intervalStart, intervalEnd);
    }

    private void initPanel() {
        insets = new Insets(10, 10, 10, 10);
        // calculate a default width for frequency labels
        // max. e.g. " 10000Hz "
        String tempText = "*10000Hz*";
        insets.left = getFontMetrics(getFont()).stringWidth(tempText) + 1;
        insets.right = insets.left;
        if (Constants.DEFAULT_LF_LABEL_FONT != null) {
            ruler = new TimeRuler(Constants.deriveSmallFont(Constants.DEFAULT_LF_LABEL_FONT), TimeFormatter.toString(0), 5);
        } else {
            ruler = new TimeRuler(Constants.DEFAULTFONT, TimeFormatter.toString(0), 5);
        }
        insets.top = ruler.getHeight();
        insets.bottom = 2 * insets.top; // for the knobs and selection times labels
        intervalStart = 0;
        intervalEnd = 10000;
    }

    /**
     * Updates the panel with new data for a new spectrogram image.
     *
     * @param audioSamples the audio samples array
     * @param freqData frequency data array
     * @param settings spectrogram settings
     * @param selection selection object, the currently selected interval
     * @param intervalStart interval start time
     * @param intervalEnd interval end time
     */
    public void showSpectrogram(int[] audioSamples, double[][] freqData, SpectrogramSettings settings, Selection selection,
                                long intervalStart, long intervalEnd) {
        this.audioSamples = audioSamples;
        this.freqData = freqData;
        this.settings = settings;
        this.selection = selection;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;

        // create image
        repaint();
    }

    /**
     * Re-draw the spectrogram.
     *
     * @param g The graphics context
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        synchronized (getTreeLock()) {
            Graphics2D g2d = (Graphics2D) g;
            // paint background
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(getForeground());
            //g.drawString("Please specify an interval in the text fields above", 20, 20);
            // draw labels and ruler etc.
            drawLabels(g2d);
            //
            //g2d.setClip(insets.left, 0, getWidth() - insets.left - insets.right, insets.top);
            g2d.translate((insets.left - (int) (5000L / msPerPixel)), 0);
            ruler.paint(g2d, 5000L, getWidth(), 10, SwingConstants.BOTTOM);
            g2d.translate(-(insets.left - (int) (5000L / msPerPixel)), 0);
            //g2d.setClip(null);
            // paint image, creating a border around it

            // draw selection markers and knobs

            // draw frame round image
            g2d.setColor(getForeground());
            g2d.drawRect(insets.left,
                         insets.top,
                         getWidth() - insets.left - insets.right,
                         getHeight() - insets.top - insets.bottom);
        }

    }

    private void drawLabels(Graphics2D g2d) {
        //if (settings == null) return;
        //String maxHz = settings.getMaxDisplayFrequency() + "Hz";
        String maxHz = 5000.20d + "Hz";
        int sw = g2d.getFontMetrics().stringWidth(maxHz);
        g2d.drawString(maxHz, insets.left - sw - 2, insets.top + g2d.getFont().getSize());

        //String minHz = settings.getMinDisplayFrequency() + "Hz";
        String minHz = 1000.44d + "Hz";
        sw = g2d.getFontMetrics().stringWidth(minHz);
        g2d.drawString(minHz, insets.left - sw - 2, getHeight() - insets.bottom);
    }

    /**
     * Show the current panel in a dialog.
     *
     * @param owner the owner frame
     */
    public void show(Frame owner) {
        AudioSpectrogramDialog dlg = new AudioSpectrogramDialog(owner, this);
        dlg.setVisible(true);
        //dlg.setDefaultCloseOperation();
    }


    @Override
    public void setBounds(int x, int y, int width, int height) {
        // TODO Auto-generated method stub
        super.setBounds(x, y, width, height);
        //System.out.println(String.format("setBounds xywh %d,%d,%d,%d", x,y,width,height)  );

    }

    /**
     * Main for testing.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                AudioSpectrogramPanel p = new AudioSpectrogramPanel();
                p.show(null);
            }
        });
    }
}
