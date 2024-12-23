package mpi.eudico.client.annotator.player;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import mpi.eudico.client.annotator.ElanLayoutManager;
import mpi.eudico.client.annotator.ElanLocale;
import mpi.eudico.client.annotator.Preferences;
import mpi.eudico.client.annotator.gui.TextAreaMessageDlg;
import static mpi.eudico.client.annotator.util.ClientLogger.LOG;
import mpi.eudico.client.mediacontrol.ControllerEvent;
import mpi.eudico.client.mediacontrol.ControllerListener;
import mpi.eudico.client.mediacontrol.ControllerManager;
import mpi.eudico.server.corpora.clomimpl.abstr.MediaDescriptor;
import nl.mpi.jds.JDSCanvas;
import nl.mpi.jds.JDSException;
import nl.mpi.jds.JDSPanel;
import nl.mpi.jds.JDSPlayer;

/**
 * Implementation of an ElanMediaPlayer that encapsulates a JDSPlayer,
 * a Java Native Interface to Direct Show player. 
 * 
 * @author Han Sloetjes
 */
public class JDSMediaPlayer extends ControllerManager
    implements ElanMediaPlayer, ControllerListener, VideoFrameGrabber, 
    VideoScaleAndMove, ActionListener {
	private static boolean regFiltersPrinted = false;
	private JDSPlayer jdsPlayer;
	private JDSPanel jdsPanel;
	private JDSCanvas jdsCanvas = null;
	private MediaDescriptor mediaDescriptor;
	private long offset = 0L;
	private long stopTime;
	private long duration;// media duration minus offset
	private float origAspectRatio;
	private float aspectRatio;
	private double millisPerSample;
	private boolean playing;
	private boolean playingInterval;
	private PlayerStateWatcher stopThread = null;
	private PlayerEndWatcher endThread = null;
	private float curSubVolume;
	private boolean mute;
	
    private boolean frameRateAutoDetected = true;
	/** if true frame forward and frame backward always jump to the begin
	 * of the next/previous frame, otherwise it jumps with the frame duration */
	private boolean frameStepsToFrameBegin = false;
	private boolean pre47FrameStepping = false;
	// gui
	private ElanLayoutManager layoutManager;
	private JMenuItem graphItem;
	private JMenuItem allFiltersItem;
	
	private boolean useNativeStopTime = true;
	private final ReentrantLock syncLock = new ReentrantLock();
	private VideoMouseAdapter mouseAdapter;
	private static boolean useCanvas = false;
	private static boolean createInitFrame = true;
	
	// read the use canvas property
	static {
    	String canvasProp = System.getProperty("JDSPlayer.UseCanvas");
    	if ("true".equalsIgnoreCase(canvasProp)) {
    		useCanvas = true;
    	}
    	String createInitFrameProp = System.getProperty("JDSPlayer.CreateInitPlayerFrame");
    	if ("false".equalsIgnoreCase(createInitFrameProp)) {
    		createInitFrame = false;
    	}
	}

	/**
	 * Creates a new player instance.
	 * 
	 * @param mediaDescriptor the media descriptor containing the URL of the 
	 * media file
	 * @throws NoPlayerException if the file cannot be found or is not supported
	 */
	public JDSMediaPlayer(MediaDescriptor mediaDescriptor) throws NoPlayerException {
		this.mediaDescriptor = mediaDescriptor;
		offset = mediaDescriptor.timeOrigin;
		
        String URLString = mediaDescriptor.mediaURL;

        if (URLString.startsWith("file:") &&
                !URLString.startsWith("file:///")) {
            URLString = URLString.substring(5);
        }
        // check preferred codec/splitter
         String prefSplitter = System.getProperty("JDSPreferredSplitter");
        try {
        	if (prefSplitter == null || prefSplitter.length() == 0) {
        		jdsPlayer = new JDSPlayer(URLString);
        	} else {
        		jdsPlayer = new JDSPlayer(URLString, prefSplitter);
        	}
        	duration = jdsPlayer.getDuration();
        	// after this call it is known whether setStopTome is supported or not
        	setMediaTime(0L);
        	jdsPlayer.setStopTime(duration);
        	duration -= offset;
        	origAspectRatio = jdsPlayer.getAspectRatio();
        	aspectRatio = origAspectRatio;
        	millisPerSample = jdsPlayer.getTimePerFrame();
        	if (millisPerSample == 0.0) {
        		millisPerSample = 40;// default 40 ms per frame, 25 frames per second
        		frameRateAutoDetected = false;
        	}
        	if (jdsPlayer.isVisualMedia()) {
        		if (createInitFrame && numVideoPlayers <= 0) {      			    			
        			createGhostFrame(jdsPlayer);
                	if (prefSplitter == null || prefSplitter.length() == 0) {
                		jdsPlayer = new JDSPlayer(URLString);
                	} else {
                		jdsPlayer = new JDSPlayer(URLString, prefSplitter);
                	}
        		}
        		numVideoPlayers++;
        		if (!useCanvas) {
        			jdsPanel = new JDSPanel(jdsPlayer);
        		} else {
        			jdsCanvas = new JDSCanvas(jdsPlayer);
        		}

        	} else {
        		millisPerSample = 40;// for audio default to 40
        		frameRateAutoDetected = false;
        		aspectRatio = 0.0f;
        	}
        	if (!regFiltersPrinted) {
        		printRegisteredFilters();
        	}
        	printFiltersInChain();
        } catch (JDSException jdse) {
        	throw new NoPlayerException("JDSPlayer cannot handle the file: " + jdse.getMessage());
        } catch (UnsatisfiedLinkError ue) {
        	// although an error should normally not be caught, catching this error when a native library
        	// is not found, allows the program to try alternative media solutions
        	throw new NoPlayerException("JDSPlayer cannot handle the file: " + ue.getMessage());
        } catch (Throwable tr) {
        	throw new NoPlayerException("JDSPlayer cannot handle the file: " + tr.getMessage());
        }
	}

	private void printRegisteredFilters() {
		if (jdsPlayer != null) {
			String[] allFilters = jdsPlayer.getRegisteredFilters();
			System.out.println("Registered Filters:");
			for (int i = 0; i < allFilters.length; i++) {
				System.out.println(i + ": " + allFilters[i]);
			}
			regFiltersPrinted = true;
			System.out.println();
		}
	}
	
	private void printFiltersInChain() {
		if (jdsPlayer != null) {
			String[] filters = jdsPlayer.getFiltersInGraph();
			System.out.println("Filters in the filter chain: " + mediaDescriptor.mediaURL);
			for (String filter : filters) {
				System.out.println(filter);
			}
		}
	}
	
	@Override
	public void cleanUpOnClose() {
		if (jdsPlayer != null) {
			if (jdsPlayer.isVisualMedia()) {
				numVideoPlayers--;
			}
			jdsPlayer.cleanUpOnClose();
			jdsPlayer = null;
			jdsPanel = null;
			jdsCanvas = null;
			if (mouseAdapter != null) {
				mouseAdapter.release();
			}
		}
		layoutManager = null;
		if (createInitFrame && numVideoPlayers <= 0) {
			disposeGhostFrame();
		}
	}

	@Override
	public float getAspectRatio() {
		return aspectRatio;
	}

	/**
	 * @return the string "JDS - Java DirectShow Player"
	 */
	@Override
	public String getFrameworkDescription() {
		String compType = jdsPanel != null ? "Panel" : "Canvas";
		return  String.format("JDS - Java DirectShow Player (with %s)", compType);
	}

	@Override
	public MediaDescriptor getMediaDescriptor() {
		return mediaDescriptor;
	}

	@Override
	public long getMediaDuration() {
		return duration;
	}

	@Override
	public long getMediaTime() {
		if (jdsPlayer != null) {
			return jdsPlayer.getMediaTime() - offset;
		}
		return 0;
	}

	@Override
	public double getMilliSecondsPerSample() {
		return millisPerSample;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public float getRate() {
		if (jdsPlayer != null) {
			return jdsPlayer.getRate();
		}
		return 0;
	}

	@Override
	public int getSourceHeight() {
		if (jdsPlayer != null) {
			return jdsPlayer.getSourceHeight();
		}
		return 0;
	}

	@Override
	public int getSourceWidth() {
		if (jdsPlayer != null) {
			return jdsPlayer.getSourceWidth();
		}
		return 0;
	}

	@Override
	public Component getVisualComponent() {
		if (!useCanvas) {
			return jdsPanel;
		} else {
			return jdsCanvas;
		}
	}

	@Override
	public float getVolume() {
		if (jdsPlayer != null) {
			return jdsPlayer.getVolume();
		}
		return 0;
	}

    @Override
    public void setSubVolume(float level) {
    	curSubVolume = level;
    }
    
    @Override
    public float getSubVolume(){
    	return curSubVolume;
    }
    
    @Override
    public void setMute(boolean mute) {
    	this.mute = mute;
    }
    
    @Override
    public boolean getMute() {
    	return mute;
    }
    
	@Override
	public boolean isFrameRateAutoDetected() {
		return frameRateAutoDetected;
	}

	@Override
	public boolean isPlaying() {
		if (jdsPlayer != null) {
			return jdsPlayer.isPlaying();
		}
		return false;
	}

	@Override
	public void nextFrame() {
		if (jdsPlayer != null) {
			//if (jdsPlayer.getState() != JDSPlayer.STATE_PAUSE) {
			if (jdsPlayer.isPlaying()) {
				//jdsPlayer.pause();
				stop();
			}
			                       // quick temporary? fix for audio only players
	        if (pre47FrameStepping || !jdsPlayer.isVisualMedia()) {
	        	nextFramePre47();
	        	return;
	        }
	        
			double nextTime = jdsPlayer.nextFrame(frameStepsToFrameBegin);

			setControllersMediaTime((long) Math.ceil(nextTime) - offset);
		}

	}
	
    /**
     * The pre 4.7 implementation of next frame.
     */
    private void nextFramePre47() {
    	// assumes player != null and is paused
        if (frameStepsToFrameBegin) {
        	long curFrame = (long)(getMediaTime() / millisPerSample);
    		setMediaTime((long) Math.ceil((curFrame + 1) * millisPerSample));
        } else {
        	long curTime = jdsPlayer.getMediaTime();
        	curTime = (long) Math.ceil(curTime + millisPerSample);
        	jdsPlayer.setMediaTime(curTime);
        	setControllersMediaTime(curTime - offset);
        }
    }

	@Override
	public void playInterval(long startTime, long stopTime) {
		if (jdsPlayer != null) {
			if (jdsPlayer.isPlaying()) {
				//jdsPlayer.stop();
				stop();
			}
			setStopTime(stopTime);
			setMediaTime(startTime);
			startInterval();
		}
		
	}
	
	@Override
	public void setStopTime(long stopTime) {
		this.stopTime = stopTime;
        // see if the stop time must be increased to ensure correct frame rendering at a frame boundary
        long nFrames = (long) ((stopTime + offset) / getMilliSecondsPerSample());

        if ((long) Math.ceil(nFrames * getMilliSecondsPerSample()) == (stopTime + offset)) { // on a frame boundary
            this.stopTime += 1;
        }
        if (useNativeStopTime) {
        	jdsPlayer.setStopTime(this.stopTime + offset);
        }
        setControllersStopTime(this.stopTime);
	}

	@Override
	public void previousFrame() {
		if (jdsPlayer != null) {
			//if (jdsPlayer.getState() != JDSPlayer.STATE_PAUSE) {
			if (jdsPlayer.isPlaying()) {
				//jdsPlayer.pause();
				stop();
			}
			                       // quick temporary? fix for audio only players
			if (pre47FrameStepping || !jdsPlayer.isVisualMedia()) {
				previousFramePre47();
				return;
			}
			
			double prevTime = jdsPlayer.previousFrame(frameStepsToFrameBegin);
			setControllersMediaTime((long) Math.ceil(prevTime) - offset);
		}

	}

    /**
     * The previous implementation of previous frame, with (more) rounding effects.
     */
    private void previousFramePre47() {
    	// assumes player != null and is paused
        if (frameStepsToFrameBegin) {
        	long curFrame = (long)(getMediaTime() / millisPerSample);
        	if (curFrame > 0) {
        		setMediaTime((long) Math.ceil(((curFrame - 1) * millisPerSample)));
        	} else {
        		setMediaTime(0);
        	}
        } else {
        	long curTime = jdsPlayer.getMediaTime();
        	curTime = (long) Math.ceil(curTime - millisPerSample);
        	
	        if (curTime < 0) {
	        	curTime = 0;
	        }
	
        	jdsPlayer.setMediaTime(curTime);
        	setControllersMediaTime(curTime - offset);
        }
    }
    
	@Override
	public void setAspectRatio(float aspectRatio) {
		this.aspectRatio = aspectRatio;
		// update popup??
	}

	@Override
	public void setFrameStepsToFrameBegin(boolean stepsToFrameBegin) {
		frameStepsToFrameBegin = stepsToFrameBegin;
	}
	@Override
	public void setLayoutManager(ElanLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
		if (layoutManager != null) {
			Double prefFrameDur = Preferences.getDouble(
				"MediaPlayer.CustomFrameDuration", 
				layoutManager.getViewerManager().getTranscription());
			if (prefFrameDur != null) {
				millisPerSample = prefFrameDur.doubleValue();
			}
			if (jdsPanel != null) {
				if (mouseAdapter != null) {
					mouseAdapter.release();
				}
				mouseAdapter = new VideoMouseAdapter(this, layoutManager, jdsPanel);
				mouseAdapter.setAdjustCoordinates(true);		
				graphItem = new JMenuItem(ElanLocale.getString("Player.FilterGraph"));
				graphItem.addActionListener(this);
				allFiltersItem = new JMenuItem(ElanLocale.getString("Player.AllFilters"));
				allFiltersItem.addActionListener(this);
				mouseAdapter.addCustomMenuItem(graphItem);
				mouseAdapter.addCustomMenuItem(allFiltersItem);
			} else if (jdsCanvas != null) {
				if (mouseAdapter != null) {
					mouseAdapter.release();
				}
				mouseAdapter = new VideoMouseAdapter(this, layoutManager, jdsCanvas);
				mouseAdapter.setAdjustCoordinates(true);		
				graphItem = new JMenuItem(ElanLocale.getString("Player.FilterGraph"));
				graphItem.addActionListener(this);
				allFiltersItem = new JMenuItem(ElanLocale.getString("Player.AllFilters"));
				allFiltersItem.addActionListener(this);
				mouseAdapter.addCustomMenuItem(graphItem);
				mouseAdapter.addCustomMenuItem(allFiltersItem);
			}
		}
	}

	@Override
	public void setMediaTime(long time) {
		if (jdsPlayer != null) {			
			//if (jdsPlayer.getState() != JDSPlayer.STATE_PAUSE) {
			if (jdsPlayer.isPlaying()) {
				//jdsPlayer.pause();
				stop();
			}
	        if (time < 0) {
	            time = 0;
	        }
	        if (time > duration) {
	        	time = duration;
	        }
	        if (jdsPlayer.getState() != JDSPlayer.STATE_PAUSE) {
	        	jdsPlayer.pause();
	        }
	        jdsPlayer.setMediaTime(time + offset);
	        setControllersMediaTime(time);
		}

	}

	@Override
	public void setMilliSecondsPerSample(double milliSeconds) {
        if (!frameRateAutoDetected) {
            millisPerSample = milliSeconds;
    		if (layoutManager != null) {
    			layoutManager.setPreference("MediaPlayer.CustomFrameDuration", 
    					Double.valueOf(milliSeconds), 
    					layoutManager.getViewerManager().getTranscription());
    		}
        }
	}

	@Override
	public void setOffset(long offset) {
		long curTime = getMediaTime();
		long diff = /*this.offset - */offset - this.offset;
        this.offset = offset;
        mediaDescriptor.timeOrigin = offset;
        if (jdsPlayer != null) {
        	duration = jdsPlayer.getDuration() - offset;
        }
        stopTime += diff;
        if (stopTime != diff && stopTime != duration) {
        	setStopTime(stopTime);//??
        }
         
        curTime += diff;
        setMediaTime(curTime < 0 ? 0 : curTime);
	}

	@Override
	public void setRate(float rate) {
		if (jdsPlayer != null) {
			jdsPlayer.setRate(rate);
		}
		setControllersRate(rate);
	}



	@Override
	public void setVolume(float level) {
		if (jdsPlayer != null) {
			jdsPlayer.setVolume(level);
		}
	}
	
	void startInterval() {
		if (jdsPlayer != null) {
			if (playing) {
				return;
			}
	        // play at start of media if at end of media
//	        if ((getMediaDuration() - getMediaTime()) < 40) {
//	            setMediaTime(0);
//	        }
			syncLock.lock();
			try {
		        playing = true;
		        playingInterval = true;
		        jdsPlayer.start();
		        startControllers();
		        // create a PlayerStateWatcher thread
		        if (stopThread != null && stopThread.isAlive()) {
		        	stopThread.setStopped();
		        }
		        stopThread = new PlayerStateWatcher();
		        stopThread.start();
			} finally {
	        	syncLock.unlock();
	        }
		}
	}	

	@Override
	public boolean isPlayingInterval() {
		return playingInterval;
	}

	/**
	 * Only to be called if not playing an interval.
	 */
	@Override
	public void start() {
		if (jdsPlayer != null) {
			if (playing) {
				return;
			}
	        // play at start of media if at end of media
	        if ((getMediaDuration() - getMediaTime()) < 40) {
	            setMediaTime(0);
	        }

	        playing = true;
	        jdsPlayer.start();
	        startControllers();
	        if (endThread != null && endThread.isAlive()) {
	        	endThread.setStopped();
	        }
	        // create a PlayerEndWatcher thread
	        endThread = new PlayerEndWatcher();
	        endThread.start();
		}
	}

	@Override
	public void stop() {
		if (jdsPlayer != null) {
			if (!playing) {
				return;
			}

			if (stopThread != null) {
				stopThread.setStopped();
			}
			jdsPlayer.pause();
	        stopControllers();
	        setControllersMediaTime(getMediaTime());
	        
	        playing = false;
	        playingInterval = false;
	        
			// reset stoptime
			if (jdsPlayer.getStopTime() != duration) {
				setStopTime(duration);
			}
		}
	}

	@Override
	public void updateLocale() {
		if (graphItem != null) {
			graphItem.setText(ElanLocale.getString("Player.FilterGraph"));
			allFiltersItem.setText(ElanLocale.getString("Player.AllFilters"));
		}
	    if (mouseAdapter != null) {
	    	mouseAdapter.updateLocale();
	    }
	}

	@Override
	public void controllerUpdate(ControllerEvent event) {
	}

	@Override
	public Image getCurrentFrameImage() {
		return getFrameImageForTime(getMediaTime());
	}

	@Override
	public Image getFrameImageForTime(long time) {
		if (jdsPlayer == null) {
			return null;
		}
		//if (jdsPlayer.getState() != JDSPlayer.STATE_PAUSE) {
		if (jdsPlayer.isPlaying()) {
			stop();
			//jdsPlayer.pause();
		}
		
        if (time != getMediaTime()) {
            setMediaTime(time);
        }
        BufferedImage image = null;
        
        byte[] data = jdsPlayer.getCurrentImageData();
        image = DIBToImage.DIBDataToBufferedImage(data);
		return image;
	}
	
	/**
	 * Returns a {@code byte} array for the specified {@code int}.
	 * 
	 * @param i the {@code int} to convert
	 * @return a {@code byte} array of size {@code 4}
	 */
	public static final byte[] getBytes(int i) {
		return new byte[] { (byte)(i>>24), (byte)(i>>16), (byte)(i>>8), (byte)i };
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == graphItem) {
        	String[] graphText = jdsPlayer.getFiltersInGraph();
        	if (graphText != null) {
        		new TextAreaMessageDlg(jdsPanel, graphText, 
        				ElanLocale.getString("Player.FilterGraph.Title"));
        	} else {
        		new TextAreaMessageDlg(jdsPanel, ElanLocale.getString("Player.Message.NoGraph"), 
        				ElanLocale.getString("Player.FilterGraph.Title"));
        	}
        } else if (e.getSource() == allFiltersItem) {
        	String[] graphText = jdsPlayer.getRegisteredFilters();
        	if (graphText != null) {
        		new TextAreaMessageDlg(jdsPanel, graphText, ElanLocale.getString("Player.AllFilters.Title"));
        	} else {
        		new TextAreaMessageDlg(jdsPanel, ElanLocale.getString("Player.Message.NoFilters"), 
        				ElanLocale.getString("Player.AllFilters.Title"));
        	}
        }
	}
	
	@Override
	public void preferencesChanged() {
    	Boolean val = Preferences.getBool("MediaNavigation.Pre47FrameStepping", null);
    	
    	if (val != null) {
    		pre47FrameStepping = val;
    	} 
    	
    	Boolean boolPref = Preferences.getBool("JDSPlayer.UseNativeStopTime", null);
		if (boolPref != null) {
			useNativeStopTime = boolPref.booleanValue();
		} else {
			String prop = System.getProperty("JDSPlayer.UseNativeStopTime");
			if (prop != null) {
				useNativeStopTime = Boolean.parseBoolean(prop);
			}
		}
	}
	
    /**
     * Temporary class to take care of state changes  after the player finished
     * playing an interval  or reached end of media  As soon as active
     * callback can be handled this class will become obsolete.
     * This method can only be used in combination with codecs that support setStopTime.
     */
    private class PlayerStateWatcher extends Thread {
    	private boolean stopped = false;
    	
    	public void setStopped() {
    		stopped = true;
    	}
    	
        /**
         * Checks the player state.
         */
        @Override
		public void run() {
        	long refTime = stopTime + offset;
        	// depending on file format or codec, the stop time that was set on the native
        	// player is not exactly the same as the passed stop time. E.g. when the value 6820
        	// is set on the native player, it might return a stop time of 6817
        	if (useNativeStopTime && jdsPlayer.getStopTime() < refTime) {
        		refTime = jdsPlayer.getStopTime();
        	}

            while (playing && !stopped && (jdsPlayer.getMediaTime() < refTime)) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                	return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }            
            if (stopped) {
            	return;
            }
            syncLock.lock();
            try {
	            // if at stop time (i.e. not stopped by hand) do some extra stuff
	            if (playing) {
	                //jdsPlayer.stopWhenReady();// needed?
	            	jdsPlayer.pause();
	                stopControllers();
	                
	                // some mpeg2 codecs need an extra set media time to render the correct frame
	                // if needed undo stop time correction, see setStopTime for details
	                /*
	                if (!stopped) {// check again?
		                if (getMediaTime() == (stopTime + 1)) {
		                	System.out.println("State watch ST1: " + stopTime);
		                    setMediaTime(stopTime); // sometimes needed for mpeg2
		                } else {
		                	System.out.println("State watch MT: " + getMediaTime());
		                    setMediaTime(getMediaTime()); // sometimes needed for mpeg2
		                }
	                }
	                */
	                if (!useNativeStopTime && getMediaTime() != refTime) {
	                	setMediaTime(refTime);
	                }
	                setControllersMediaTime(getMediaTime());
	                //jdsPlayer.pause();
	                setStopTime(duration);
	                playing = false;
	                playingInterval = false;
	            }
            } finally {
            	syncLock.unlock();
            }
        }
    }
    
    /**
     * Thread waiting for the player to reach end of media in order to stop controllers. 
     * As soon as active
     * callback can be handled this class will become obsolete.
     */
    private class PlayerEndWatcher extends Thread {
    	private boolean stopped = false;
    	
    	public void setStopped() {
    		stopped = true;
    	}
    	
        /**
         * Waits for the end of media being reached
         */
        @Override
		public void run() {
            while (playing && !stopped && (getMediaTime() < duration)) {
                try {
                    Thread.sleep(300);
                }  catch (InterruptedException ie) {
                    ie.printStackTrace();
                	return;
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
           
            if (stopped) {
            	return;
            }
            // if at stop time (i.e. not stopped by hand) do some extra stuff
            if (playing) {
                //jdsPlayer.stopWhenReady();// needed?
                jdsPlayer.pause();//??
                stopControllers();
                setControllersMediaTime(getMediaTime());
                //System.out.println("Pausing at end of media: " + mediaDescriptor.mediaURL + 
                //		" Time: " + getMediaTime());
                playing = false;
            }
        }
    }

	@Override
	public float getVideoScaleFactor() {
		return jdsPlayer.getVideoScaleFactor();
	}

	@Override
	public void setVideoScaleFactor(float scaleFactor) {
		jdsPlayer.setVideoScaleFactor(scaleFactor);
	}

	@Override
	public void repaintVideo() {
		// stub
	}

	@Override
	public int[] getVideoBounds() {
		return jdsPlayer.getVideoDestinationPos();
	}

	@Override
	public void setVideoBounds(int x, int y, int w, int h) {
		jdsPlayer.setVideoDestinationPos(x, y, w, h);
	}

	@Override
	public void moveVideoPos(int dx, int dy) {
		jdsPlayer.moveVideoPos(dx, dy);
	}

	//
	/*
	 * As of JDK 18 (on Windows 11) the first mpg1 file opened in ELAN does not
	 * appear, but instead a blue/grey/black rectangle is shown. Subsequent 
	 * videos mostly appear normally, as long as the first one remains open.
	 * The effect 'spans' multiple jvm's: if a first video is opened in one JVM
	 * the first video opened in another seems to appear correctly. 
	 * In order to circumvent this problem, the below code creates a small,
	 * almost invisible frame for the first video and keeps it open until all
	 * other JDS video players have been closed.
	 */
	private static int numVideoPlayers = 0;
	private static JFrame hostFrame;
	private static JDSPlayer hiddenPlayer;
	private static Component hostPanel;
	
	private void createGhostFrame(final JDSPlayer jdsPlayer) {
		try {
			if (LOG.isLoggable(Level.INFO)) {
				LOG.log(Level.INFO, "Creating an enabling host video player and window");
			}
			hiddenPlayer = jdsPlayer;
			hostPanel = new JDSPanel(hiddenPlayer);
			hostFrame = new JFrame("Base Video");
			hostFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			hostFrame.setUndecorated(true);
			hostFrame.setBounds(0, 0, 2, 2);
			hostFrame.setVisible(true);
			hostFrame.getContentPane().add(hostPanel);
		} catch (Throwable t) {
			if (LOG.isLoggable(Level.INFO)) {
				LOG.log(Level.INFO, "Failed to create a host video player and window: " + t.getMessage());
			}
		}
	}
	
	private void disposeGhostFrame() {
		if (hostFrame != null) {
			try {
				if (LOG.isLoggable(Level.INFO)) {
					LOG.log(Level.INFO, "Removing the enabling host video player and window");
				}
				if (hiddenPlayer != null) {
					hiddenPlayer.cleanUpOnClose();
					hiddenPlayer = null;
				}
				hostPanel = null;
				hostFrame.setVisible(false);
				hostFrame.dispose();
			} catch (Throwable t) {
				if (LOG.isLoggable(Level.INFO)) {
					LOG.log(Level.INFO, "Failed to remove the host video player and window: " + t.getMessage());
				}
			}
		}
	}
	
	//
}
