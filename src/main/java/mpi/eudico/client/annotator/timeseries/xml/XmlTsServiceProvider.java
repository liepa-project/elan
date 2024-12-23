package mpi.eudico.client.annotator.timeseries.xml;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import mpi.eudico.client.annotator.recognizer.io.XmlTimeSeriesReader;
import mpi.eudico.client.annotator.timeseries.AbstractTSTrack;
import mpi.eudico.client.annotator.timeseries.TimeSeriesConstants;
import mpi.eudico.client.annotator.timeseries.TimeSeriesTrack;
import mpi.eudico.client.annotator.timeseries.config.TSSourceConfiguration;
import mpi.eudico.client.annotator.timeseries.config.TSTrackConfiguration;
import mpi.eudico.client.annotator.timeseries.spi.TSConfigPanel;
import mpi.eudico.client.annotator.timeseries.spi.TSServiceProvider;
import static mpi.eudico.client.annotator.util.ClientLogger.LOG;

import org.xml.sax.SAXException;

/**
 * A service provider for handling timeseries in an XML file, 
 * more specifically the AVATecH TIMESERIES file format.
 * 
 * @author Han Sloetjes
 */
public class XmlTsServiceProvider implements TSServiceProvider {

	
	/**
	 * Constructor.
	 */
	public XmlTsServiceProvider() {
		super();
	}

    /**
     * Creates tracks without user configuration.
     *
     * @param config a configuration object containing information on the
     *        source
     */
	@Override
	public void autoCreateTracks(TSSourceConfiguration config) {
		if (config != null) {
			String source = config.getSource();
			if (source.startsWith("file:")) {
				source = source.substring(5);
			}
			File sourceFile = new File(source);
			// check the ProviderClassName ?
			XmlTimeSeriesReader xmlReader = new XmlTimeSeriesReader(sourceFile);
			
			try {
				List<Object> tracks = xmlReader.parse();
				// the tracks are already configured
				if (tracks != null) {
					AbstractTSTrack tst;
					for (Object trObj : tracks) {
						if (trObj instanceof TimeSeriesTrack) {
							tst = (AbstractTSTrack) trObj;
							tst.setTimeOffset(config.getTimeOrigin());
							
				            // create a track config object for each track
				            TSTrackConfiguration trconf = new TSTrackConfiguration(tst.getName(),
				                    tst);
				            config.putObject(tst.getName(), trconf);
						}
					}
				}
			} catch (IOException ioe) {
                LOG.severe(
                        "Could not read data from the timeseries file " +
                        config.getSource() + ": " + ioe.getMessage());
			} catch (SAXException sax) {
                LOG.severe(
                        "Could not read data from the timeseries file " +
                        config.getSource() + ": " + sax.getMessage());
			}
			
		}

	}

	/**
	 * Checks whether the file can be handled by this provider.
	 * 
	 * @param filePath the path to the file
	 * @return {@code true} if it appears to be a (AVATecH) XML timeseries file.
	 */
	@Override
	public boolean canHandle(String filePath) {
        LOG.info("Polling: " + filePath);

        if ((filePath == null) || (filePath.length() < 5)) {
            return false;
        }
        
        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".xml")){
        	if (filePath.startsWith("file:")) {
        		filePath = filePath.substring(5); 
        	}
        	File f = new File(filePath);
        	
        	try (BufferedReader bufRead = new BufferedReader(
        			new FileReader(f, StandardCharsets.UTF_8))) {
    			String line = null;
    			int numLine = 0;
    			boolean canHandle = false;
    			
    			while ((line = bufRead.readLine()) != null) {
    				if (line.length() == 0) {
    					continue;
    				}
    				if (numLine == 0) {
    					if (!line.trim().startsWith("<?xml")) {
    						LOG.info("Not a valid XML file or a file without <?xml ...?> declaration.");
    						break;
    					}
    				}
    				if (numLine >= 1) {
    					String trimmed = line.trim();
    					if (trimmed.startsWith("<")) {
    						if (trimmed.startsWith("<TIMESERIES")) {
    							canHandle = true;
    						} // else false
    						break;
    					}
    				}
    				numLine++;
    			}
    			
    			return canHandle;
        	} catch (IOException ioe) {
        		LOG.warning("Error reading file: " + ioe.getMessage());
            }
        }
		return false;
	}

	/**
	 * Creates and configures tracks from stored configurations.
	 * 
	 * @param config the source configuration
	 */
	@Override
	public void createTracksFromConfiguration(TSSourceConfiguration config) {
		if (config == null) {
			return;
		}
		String source = config.getSource();
		if (source.startsWith("file:")) {
			source = source.substring(5);
		}
		File sourceFile = new File(source);
		// check the ProviderClassName ?
		XmlTimeSeriesReader xmlReader = new XmlTimeSeriesReader(sourceFile);
		
		try {
			List<Object> tracks = xmlReader.parse();
			// the tracks are already configured
			if (tracks == null) {
				return;
			}
			
	        Iterator<Object> trIt = config.objectKeySet().iterator();
	        while (trIt.hasNext()) {
	            Object key = trIt.next();
	            Object val = config.getObject(key);

	            if (!(val instanceof TSTrackConfiguration)) {
	                continue;
	            }

	            TSTrackConfiguration trconf = (TSTrackConfiguration) val;
	            
				for (Object trObj : tracks) {
					if (trObj instanceof TimeSeriesTrack) {
						AbstractTSTrack track = (AbstractTSTrack) trObj;
						// compare based on name??
						if (trconf.getTrackName().equals(track.getName())) {
							track.setTimeOffset(config.getTimeOrigin());
							track.setDescription(trconf.getProperty(TimeSeriesConstants.DESC));
				            track.setUnitString(trconf.getProperty(TimeSeriesConstants.UNITS));
				            
				            float min = Float.NaN;
				            float max = Float.NaN;

				            try {
				                min = Float.parseFloat(trconf.getProperty(
				                            TimeSeriesConstants.MIN));
				            } catch (NumberFormatException nfe) {
				            }

				            try {
				                max = Float.parseFloat(trconf.getProperty(
				                            TimeSeriesConstants.MAX));
				            } catch (NumberFormatException nfe) {
				            }
				            if (! (Float.isNaN(min) || Float.isNaN(max)))  {
				            	track.setRange(new float[] { min, max });
				            }
				            
				            Color c = parseColor(trconf.getProperty(TimeSeriesConstants.COLOR));
				            if (c != null) {
				            	track.setColor(c);
				            }

				            trconf.putObject(track.getName(), track);
				            
				            trconf.removeProperty(TimeSeriesConstants.DERIVATION);
				            trconf.removeProperty(TimeSeriesConstants.DESC);
				            trconf.removeProperty(TimeSeriesConstants.UNITS);
				            trconf.removeProperty(TimeSeriesConstants.MIN);
				            trconf.removeProperty(TimeSeriesConstants.MAX);
				            trconf.removeProperty(TimeSeriesConstants.COLOR);
				            break;
						}
					}
				} // end tracks loop
	        }// end track config iteration
					

		} catch (IOException ioe) {
            LOG.severe(
                    "Could not read data from the timeseries file " +
                    config.getSource() + ": " + ioe.getMessage());
		} catch (SAXException sax) {
            LOG.severe(
                    "Could not read data from the timeseries file " +
                    config.getSource() + ": " + sax.getMessage());
		}
	}
	
    /**
     * Creates a Color object from a comma separated string. Should move to a
     * utility class.
     *
     * @param rgb the string
     *
     * @return the color
     */
    private Color parseColor(String rgb) {
        if (rgb == null) {
            return null;
        }
        // could be replaced by String.split(",") etc.
        int index = rgb.indexOf(',');
        int index2 = rgb.lastIndexOf(',');

        if ((index > -1) && (index2 > -1)) {
        	try {
	            int r = Integer.parseInt(rgb.substring(0, index));
	            int g = Integer.parseInt(rgb.substring(index + 1, index2));
	            int b = Integer.parseInt(rgb.substring(index2 + 1));
	
	            return new Color(r, g, b);
        	} catch (NumberFormatException nfe) {
        		return null;
        	} catch (IllegalArgumentException iae) {
        		return null;
        	}
        } else {
            return null;
        }
    }

	/**
	 * Returns false, no user interface is provided for configuring tracks.
	 */
	@Override
	public TSConfigPanel getConfigPanel(TSSourceConfiguration config) {
		return null;
	}

	/**
	 * Returns false for now. No user interface is provided for configuring tracks.
	 */
	@Override
	public boolean isConfigurable() {
		return false;
	}

}
