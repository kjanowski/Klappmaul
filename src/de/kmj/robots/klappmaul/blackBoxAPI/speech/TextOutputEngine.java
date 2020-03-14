package de.kmj.robots.klappmaul.blackBoxAPI.speech;


import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simulates a TTS engine on the Klappmaul using only text display.
 * 
 * </br>
 * Note that the SpeechEngine here COULD report which speech task
 * is currently being processed, but intentionally refuses to do so.
 * This mimics the problems encountered with the other robots:
 * <ul>
 * <li>RoboKind R-50: None of the speech events contain any information
 * about the associated SpeechJob, even though the API suggests so.
 * Since the R-50 libraries are scarcely documented and no longer being updated,
 * a solution is unlikely.</li>
 * <li>Robopec Reeti V1/V2: The TTS does not associate any job ID
 * with the speech commands.</li>
 * </ul>
 * 
 * @author Kathrin Janowski
 */
public class TextOutputEngine extends SpeechEngine{
    /** The time (in milliseconds) required for speaking a vowel. */
    private int mVowelLength;
    
    
    public TextOutputEngine()
    {
        super();
        mVowelLength = 250;
    }

    public TextOutputEngine(Properties config)
    {
        super(config);        
        
        String vowelLengthStr = config.getProperty("speech.vowelLength", "250");
        
        try{
            mVowelLength = Integer.parseInt(vowelLengthStr);
        }
        catch(NumberFormatException nfe)
        {
            mVowelLength = 250;
            Logger.getLogger(TextOutputEngine.class.getName()).
                    log(Level.WARNING, "invalid vowelLength parameter: {0}\n\t-> using default {1}",
                            new Object[]{vowelLengthStr, mVowelLength});
        }
    }
    
    @Override
    protected void processJob(SpeechJob job)
    {
        int duration;
        
        StringTokenizer words = new StringTokenizer(job.getText(), " ");
        boolean speechRunning = true;

        setSpeaking(true);
        SpeechEvent startEvent
                = new SpeechEvent(job.getJobID(), SpeechEvent.SpeechEventType.STARTED);
        notifyListeners(startEvent);

        long bookID;
        while (speechRunning && (words.hasMoreTokens())) {
            String fragment = words.nextToken();

            bookID = getBookmarkID(fragment);
            if (bookID>-1) {
                SpeechEvent bookEvent
                        = new SpeechEvent(job.getJobID(), SpeechEvent.SpeechEventType.BOOKMARK, bookID);
                notifyListeners(bookEvent);
            } else {
                SpeechEvent wordEvent
                        = new SpeechEvent(job.getJobID(), SpeechEvent.SpeechEventType.WORD_STARTED, fragment);
                notifyListeners(wordEvent);

                String vowels = fragment.toLowerCase().replaceAll("[^aeiouäöüy]", "");
                duration = vowels.length() * mVowelLength;

                try {
                    sleep(duration);
                } catch (InterruptedException ex) {
                    setSpeaking(false);
                }
            }

            //check: has the speech job been stopped in the meantime?
            speechRunning = isSpeaking();
        }

        setSpeaking(false);
        SpeechEvent stopEvent
                = new SpeechEvent(job.getJobID(), SpeechEvent.SpeechEventType.STOPPED);
        notifyListeners(stopEvent);
    }
    
    @Override
    public boolean setVoice(TreeMap<String, String> params)
    {
        Logger.getLogger(TextOutputEngine.class.getName()).
            log(Level.INFO,
                "if there was a voice, it would be set to {0}",
                params);
        return true;
    }

    @Override
    protected void cleanup() {
        //nothing to do
    }
}
