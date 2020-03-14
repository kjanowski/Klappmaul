package de.kmj.robots.klappmaul.blackBoxAPI.speech;


import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for a TTS engine on the Klappmaul.
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
public abstract class SpeechEngine extends Thread{
    /** The set of event listeners. */
    protected final HashSet<SpeechEventListener> mListeners;
    
    /** The lock which secures the listener set. */
    protected final Object mListenersLock = new Object();

    /** The buffer which contains the requested utterances. */
    protected final LinkedBlockingQueue<SpeechJob> mSpeechJobBuffer;
    
    /** The lock which secures the buffer. */
    protected final Object mBufferLock = new Object();

    /** The flag which indicates that the speech is active. */
    protected boolean mSpeaking;
    
    /** The lock which secures the activity flag. */
    protected final Object mSpeakingLock = new Object();
    
    /** Counter for assigning speech job IDs. */ 
    protected int mJobCounter;
    
    /** Regex pattern for parsing bookmarks. */
    protected Pattern mBookmarkPattern;
    
    protected String mLogName;
    
    
    
    public SpeechEngine()
    {
        mSpeechJobBuffer = new LinkedBlockingQueue<>();
        mListeners = new HashSet<>();
        mJobCounter=0;
        mLogName="[Klappmaul Speech]";
        
        setSpeaking(false);
    }
    
    public SpeechEngine(Properties config)
    {
        this();
        
        //get the robot name
        mLogName = config.getProperty("name", "Klappmaul");
        mLogName = "["+mLogName+" Speech]";
        
        //get the bookmark pattern
        String bookmarkPattern = config.getProperty("speech.bookmarkPattern", "\\$(\\d+)");
        mBookmarkPattern = Pattern.compile(bookmarkPattern);
    }
    
    @Override
    public void run()
    {
        boolean running = true;
        SpeechJob currentJob=null;
        
        while(running)
        {
            //------------------------------------------------------------------
            // get the oldest speech job from the buffer
            //------------------------------------------------------------------
            synchronized(mBufferLock)
            {
                //wait until there are actions i the buffer
                while(running && mSpeechJobBuffer.isEmpty())
                {
                    try {
                        mBufferLock.wait();
                    }
                    catch (InterruptedException ex) {
                        //Thread was interrupted
                        running = false;
                    }
                }

                try{ currentJob = mSpeechJobBuffer.remove(); }
                catch(NoSuchElementException nsee)
                {   currentJob = null;  }
            }
            
            //------------------------------------------------------------------
            // process speech job
            //------------------------------------------------------------------
            
            if(currentJob!=null)
            {
                processJob(currentJob);
            }
        }
        
        cleanup();
    }
    
    protected abstract void cleanup();
    
    protected abstract void processJob(SpeechJob job);
            
    protected void setSpeaking(boolean speaking)
    {
        synchronized(mSpeakingLock)
        {
            mSpeaking = speaking;
            mSpeakingLock.notify();
        }
    }
    
    protected boolean isSpeaking()
    {
        synchronized(mSpeakingLock)
        {
            return mSpeaking;
        }
    }
    
    //==========================================================================
    // public interface
    //==========================================================================

    public void say(String text)
    {
        String jobID = "tts"+mJobCounter;
        mJobCounter++;
        SpeechJob job = new SpeechJob(jobID, text);
        
        synchronized(mBufferLock)
        {
            mSpeechJobBuffer.add(job);
            mBufferLock.notify();
        }
        
    }
    
    /**
     * Tries to extract a bookmark ID from the given text.
     * 
     * @param text the text to check for the bookmark pattern
     * @return the bookmark ID if the given text matches the regex pattern, -1 otherwise
     */
    protected long getBookmarkID(String text)
    {
        Matcher matcher = mBookmarkPattern.matcher(text);
        if(matcher!=null)
        {
            if(matcher.matches())
                try{
                    long id = Long.parseLong(matcher.group(1));
                    return id;
                }
                catch(NumberFormatException e)
                {
                    //TODO log debug message
                }
        }
        return -1;
    }
    
    public void cancelCurrentSpeech()
    {
        setSpeaking(false);
    }

    public void cancelAllSpeech()
    {
        synchronized(mBufferLock)
        {
            mSpeechJobBuffer.clear();
            mBufferLock.notify();
        }
        
        setSpeaking(false);
    }
    
    public abstract boolean setVoice(TreeMap<String, String> params);
    
    
    public void addListener(SpeechEventListener listener)
    {
        synchronized(mListenersLock)
        {
            mListeners.add(listener);
        }
    }

    public void removeListener(SpeechEventListener listener)
    {
        synchronized(mListenersLock)
        {
            mListeners.remove(listener);
        }
    }

    protected void notifyListeners(SpeechEvent event)
    {
        synchronized(mListenersLock)
        {
            for(SpeechEventListener listener: mListeners)
            {
                listener.onSpeechEvent(event);
            }
        }
    }
    
}
