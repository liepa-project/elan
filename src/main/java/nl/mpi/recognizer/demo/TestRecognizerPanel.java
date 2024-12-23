package nl.mpi.recognizer.demo;


import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpi.eudico.client.annotator.ElanLocale;

/**
 * A panel for a test recognizer implementation.
 * 
 * @author albertr
 */
@SuppressWarnings("serial")
public class TestRecognizerPanel extends JPanel implements ChangeListener {
	/** silence duration label */
	private JLabel minimalSilenceDurationLabel;
	/** silence duration slider */
	private JSlider minimalSilenceDuration;
	/** non-silence duration label */
	private JLabel minimalNonSilenceDurationLabel;
	/** non silence duration slider */
	private JSlider minimalNonSilenceDuration;
	
	/**
	 * Creates a new panel instance.
	 */
	public TestRecognizerPanel() {
		int initialSilenceDuration = 400;
		int initialNonSilenceDuration = 300;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
		minimalSilenceDurationLabel = new JLabel(
				ElanLocale.getString("Recognizer.Silence.MinimalSilenceDuration") + 
				" " + initialSilenceDuration + " " + ElanLocale.getString("PlayAroundSelDialog.Ms"));
		add(minimalSilenceDurationLabel);
		minimalSilenceDuration = new JSlider(JSlider.HORIZONTAL, 0, 1000, initialSilenceDuration);
		minimalSilenceDuration.setMajorTickSpacing(200);
		minimalSilenceDuration.setMinorTickSpacing(25);
		minimalSilenceDuration.setPaintTicks(true);
		minimalSilenceDuration.setPaintLabels(true);
		minimalSilenceDuration.addChangeListener(this);
		add(minimalSilenceDuration);
		add(Box.createVerticalStrut(10));
		minimalNonSilenceDurationLabel = new JLabel(
				ElanLocale.getString("Recognizer.Silence.MinimalNonSilenceDuration") + 
				" " + initialNonSilenceDuration + " " + ElanLocale.getString("PlayAroundSelDialog.Ms"));
		add(minimalNonSilenceDurationLabel);
		minimalNonSilenceDuration = new JSlider(JSlider.HORIZONTAL, 0, 1000, initialNonSilenceDuration);
		minimalNonSilenceDuration.setMajorTickSpacing(200);
		minimalNonSilenceDuration.setMinorTickSpacing(25);
		minimalNonSilenceDuration.setPaintTicks(true);
		minimalNonSilenceDuration.setPaintLabels(true);
		minimalNonSilenceDuration.addChangeListener(this);
		add(minimalNonSilenceDuration);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
    	int duration = (int)source.getValue();
        
        if (source == minimalSilenceDuration) {
        	minimalSilenceDurationLabel.setText(
        			ElanLocale.getString("Recognizer.Silence.MinimalSilenceDuration") + 
        			" " + duration + " " + ElanLocale.getString("PlayAroundSelDialog.Ms"));
        } else if (source == minimalNonSilenceDuration) {
        	minimalNonSilenceDurationLabel.setText(
        			ElanLocale.getString("Recognizer.Silence.MinimalNonSilenceDuration") +
        			" " + duration + " " + ElanLocale.getString("PlayAroundSelDialog.Ms"));
        }
    }

	/**
	 * Returns the minimal silence duration.
	 * 
	 * @return the minimal silence duration
	 */
	public int getMinimalSilenceDuration() {
		return minimalSilenceDuration.getValue();
	}
	
	/**
	 * Returns the minimal non-silence duration.
	 * 
	 * @return the minimal non silence duration
	 */
	public int getMinimalNonSilenceDuration() {
		return minimalNonSilenceDuration.getValue();
	}
}