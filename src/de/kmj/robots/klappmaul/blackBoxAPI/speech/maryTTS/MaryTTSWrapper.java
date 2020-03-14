package de.kmj.robots.klappmaul.blackBoxAPI.speech.maryTTS;


import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEngine;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechJob;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.maryTTS.AudioRetrieverThread;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.maryTTS.BookmarkCalculatorThread;
import java.io.IOException;
import java.util.Locale;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import marytts.MaryInterface;
import marytts.LocalMaryInterface;
import marytts.client.RemoteMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.util.data.audio.AudioPlayer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides TTS on the Klappmaul, powered by
 * <a href="http://mary.dfki.de/">MaryTTS</a>.
 * </br>
 * Note that the SpeechEngine here COULD report which speech task is currently
 * being processed, but intentionally refuses to do so. This mimics the problems
 * encountered with the other robots:
 * <ul>
 * <li>RoboKind R-50: None of the speech events contain any information about
 * the associated SpeechJob, even though the API suggests so. Since the R-50
 * libraries are scarcely documented and no longer being updated, a solution is
 * unlikely.</li>
 * <li>Robopec Reeti V1/V2: The TTS does not associate any job ID with the
 * speech commands.</li>
 * </ul>
 *
 * @author Kathrin Janowski
 */
public class MaryTTSWrapper extends SpeechEngine implements LineListener {

    private final Logger cLogger;
    
    /**
     * The MaryTTS interface.
     */
    private final MaryInterface mAcoustParamsInterface;
    private final MaryInterface mAudioInterface;
    
    private final DocumentBuilderFactory mDocBuilderFactory;
    private final DocumentBuilder mDocBuilder;

    
    /**
     * The current locale setting.
     */
    private String mLocale;

    /**
     * The current voice.
     */
    private String mVoice;

    /**
     * The current list of bookmarks indexed by the time of occurrence.
     */
    private final TreeMap<Integer, Long> mBookmarks;

    /**
     * The generated speech output.
     */
    private AudioInputStream mAudioStream;

    /**
     * The audio player used for speech output.
     */
    private AudioPlayer mAudioPlayer;

    
    //--------------------------------------------------------------------------
    // voice parameters
    //--------------------------------------------------------------------------
        
    private int mChannel;
    private final int cDefaultChannel = AudioPlayer.STEREO;
    
    private float mVolume;
    private final float cDefaultVolume = 1.0f;
    
    private float mRate;
    private final float cDefaultRate = 1.0f;
    
    private float mRange;
    private final float cDefaultF0Scale = 1.0f;

    private float mF0Add;
    private final float cDefaultF0Add = 0.0f;

    private String mPitch;
    private final String cDefaultPitch = "normal";
    
    private float mRobotise;
    private final float cDefaultRobotise = 0.0f;

    
    //==========================================================================
    // initialization and running
    //==========================================================================
    public MaryTTSWrapper() throws MaryConfigurationException, ParserConfigurationException {
        super();
        
        cLogger = Logger.getLogger(MaryTTSWrapper.class.getName());
        mDocBuilderFactory = DocumentBuilderFactory.newInstance();
        mDocBuilder = mDocBuilderFactory.newDocumentBuilder();

        mBookmarks = new TreeMap<>();

        //default parameters
        mLocale = "de";
        mVoice = "bits3-hsmm";

        mAcoustParamsInterface = new LocalMaryInterface();
        mAcoustParamsInterface.setInputType("RAWMARYXML");
        mAcoustParamsInterface.setOutputType("REALISED_ACOUSTPARAMS");
        
        mAudioInterface = new LocalMaryInterface();
        mAudioInterface.setInputType("RAWMARYXML");
        mAudioInterface.setOutputType("AUDIO");
        
        mAudioStream = null;
        mAudioPlayer = null;
    }

    public MaryTTSWrapper(Properties config) throws MaryConfigurationException, IOException, ParserConfigurationException{
        super(config);
        
        cLogger = Logger.getLogger(MaryTTSWrapper.class.getName());
        mDocBuilderFactory = DocumentBuilderFactory.newInstance();
        mDocBuilder = mDocBuilderFactory.newDocumentBuilder();

        mBookmarks = new TreeMap<>();

        //read config
        mLocale = config.getProperty("speech.locale", "de");
        mVoice = config.getProperty("speech.voice", "bits3-hsmm");
                
        boolean isRemote = Boolean.parseBoolean(config.getProperty("speech.remote", "false"));
        if(isRemote)
        {
            String serverHost = config.getProperty("speech.serverHost", "127.0.0.1").trim();
            String serverPortStr = config.getProperty("speech.serverPort", "59125").trim();
        
            int serverPort = Integer.parseInt(serverPortStr);
            
            mAcoustParamsInterface = new RemoteMaryInterface(serverHost, serverPort);
            mAudioInterface = new RemoteMaryInterface(serverHost, serverPort);
        }
        else
        {
            mAcoustParamsInterface = new LocalMaryInterface();
            mAudioInterface = new LocalMaryInterface();
        }

        mAcoustParamsInterface.setInputType("RAWMARYXML");
        mAcoustParamsInterface.setOutputType("REALISED_ACOUSTPARAMS");
        //mMaryInterface.setInputType("TEXT");
        mAcoustParamsInterface.setLocale(Locale.forLanguageTag(mLocale));
        mAcoustParamsInterface.setVoice(mVoice);
                
        mAudioInterface.setInputType("RAWMARYXML");
        mAudioInterface.setOutputType("AUDIO");
        mAudioInterface.setLocale(Locale.forLanguageTag(mLocale));
        mAudioInterface.setVoice(mVoice);
        
        configureVoice(config);

        mAudioStream = null;
        mAudioPlayer = null;
    }

    private void configureVoice(Properties config)
    {
        String volStr = null;
        try{
            volStr = config.getProperty("speech.volume", ""+cDefaultVolume);
            mVolume = Float.parseFloat(volStr);
        }
        catch(NumberFormatException nfe)
        {
            cLogger.log(Level.WARNING,
                    "invalid parameter for speech.volume: {0}\n\t-> default to {1}",
                    new Object[]{volStr, cDefaultVolume});
            mVolume = cDefaultVolume;
        }
        
        
        String rateStr = null;
        try{
            rateStr = config.getProperty("speech.rate", ""+cDefaultRate);
            mRate = Float.parseFloat(rateStr);
        }
        catch(NumberFormatException nfe)
        {
            cLogger.log(Level.WARNING,
                    "invalid parameter for speech.rate: {0}\n\t-> default to {1}",
                    new Object[]{rateStr, cDefaultRate});
            mRate = cDefaultRate;
        }

        String f0ScaleStr = config.getProperty("speech.F0Scale", ""+cDefaultF0Scale);
        try{
            mRange = Float.parseFloat(f0ScaleStr);
        }
        catch(NumberFormatException nfe)
        {
            cLogger.log(Level.WARNING,
                    "invalid parameter for speech.F0Scale: {0}\n\t-> default to {1}",
                    new Object[]{f0ScaleStr, cDefaultF0Scale});
            mRange = cDefaultF0Scale;
        }

        String f0AddStr = config.getProperty("speech.F0Add", ""+cDefaultF0Add);
        try{
            mF0Add = Float.parseFloat(f0AddStr);
        }
        catch(NumberFormatException nfe)
        {
            cLogger.log(Level.WARNING,
                    "invalid parameter for speech.F0Add: {0}\n\t-> default to {1}",
                    new Object[]{f0AddStr, cDefaultF0Add});
            mF0Add = cDefaultF0Add;
        }
        
        
        mPitch = config.getProperty("speech.pitch", cDefaultPitch);
      
        String robotiseStr = null;
        try{
            robotiseStr = config.getProperty("speech.robotise", ""+cDefaultRobotise);
            mRobotise = Float.parseFloat(robotiseStr);
        }
        catch(NumberFormatException nfe)
        {
            cLogger.log(Level.WARNING,
                    "invalid parameter for speech.robotise: {0}\n\t-> default to {1}",
                    new Object[]{robotiseStr, cDefaultRobotise});
            mRobotise = cDefaultRobotise;
        }

        updateVoiceEffects();
        
        String outputStr = null;
        try{
            outputStr = config.getProperty("speech.outputChannel", ""+cDefaultChannel);
            switch(outputStr.toLowerCase())
            {
                case "mono": mChannel = AudioPlayer.MONO; break;
                case "stereo": mChannel = AudioPlayer.STEREO; break;
                case "left": mChannel = AudioPlayer.LEFT_ONLY; break;
                case "right": mChannel = AudioPlayer.RIGHT_ONLY; break;
                default:
                    cLogger.log(Level.WARNING,
                    "unexpected parameter for speech.outputMode: {0}\n\t-> default to {1}",
                    new Object[]{outputStr, cDefaultChannel});
                    mChannel = cDefaultChannel;
                    break;
            }
        }
        catch(NullPointerException ne)
        {
            cLogger.log(Level.WARNING,
                    "invalid parameter for speech.outputMode: {0}\n\t-> default to {1}",
                    new Object[]{outputStr, cDefaultChannel});
            mChannel = cDefaultChannel;
        }

    }

    @Override
    public boolean setVoice(TreeMap<String,String> params)
    {
        String newLanguage = params.get("language");
        if(newLanguage != null)
        {
            try{
                Locale locale = Locale.forLanguageTag(newLanguage);
                
                mAcoustParamsInterface.setLocale(locale);
                mAudioInterface.setLocale(locale);
                mLocale = locale.toLanguageTag();
            }catch(IllegalArgumentException iae)
            {
                cLogger.log(Level.SEVERE, "could not set language {0}", newLanguage);
                return false;
            }
        }
        
        String newVoice = params.get("name");
        if(newVoice != null)
        {
            try{
                mAcoustParamsInterface.setVoice(newVoice);
                mAudioInterface.setVoice(newVoice);
                mVoice = newVoice;
            }catch(IllegalArgumentException iae)
            {
                cLogger.log(Level.SEVERE, "could not set voice {0}", newVoice);
                return false;
            }
        }

        
        String newChannel = params.get("channel");
        if(newChannel == null)
            newChannel = params.get("channel");
        if(newChannel != null)
        {
            try{
                switch(newChannel.toLowerCase())
                {
                    case "mono": mChannel = AudioPlayer.MONO; break;
                    case "stereo": mChannel = AudioPlayer.STEREO; break;
                    case "left": mChannel = AudioPlayer.LEFT_ONLY; break;
                    case "right": mChannel = AudioPlayer.RIGHT_ONLY; break;
                    default:
                        cLogger.log(Level.WARNING,
                        "unexpected parameter for channel: {0}\n\t-> default to {1}",
                        new Object[]{newChannel, cDefaultChannel});
                        mChannel = cDefaultChannel;
                        break;
                }
            }
            catch(NullPointerException ne)
            {
                cLogger.log(Level.WARNING,
                        "invalid parameter for channel: {0}\n\t-> default to {1}",
                        new Object[]{newChannel, cDefaultChannel});
                mChannel = cDefaultChannel;
            }
        }
        
        //----------------------------------------------------------------------
        // effects
        //----------------------------------------------------------------------
        
        String newVolume = params.get("volume");
        if(newVolume != null)
            try{
                float volume = Float.parseFloat(newVolume);
                mVolume = volume;
            }
            catch(NumberFormatException nfe)
            {
                cLogger.log(Level.SEVERE,
                        "could not change volume: {0} is not a float value",
                        newVolume);
                return false;
            }
        
        String newRange = params.get("range");
        if(newRange != null)
            try{
                float range = Float.parseFloat(newRange);
                mRange = range*5.0f; //scale by maximum F0 scale parameter
            }
            catch(NumberFormatException nfe)
            {
                cLogger.log(Level.SEVERE,
                        "could not change pitch range: {0} is not a float value",
                        newRange);
                return false;
            }
        
        String newF0Add = params.get("pitchAdd");
        if(newF0Add ==  null)
            newF0Add = params.get("pitchadd");
        if(newF0Add != null)
            try{
                float f0Add = Float.parseFloat(newF0Add);
                mF0Add = f0Add;
            }
            catch(NumberFormatException nfe)
            {
                cLogger.log(Level.SEVERE,
                        "could not change F0 add: {0} is not a float value",
                        newF0Add);
                return false;
            }
        
        
        String newRobotise = params.get("robotise");
        if(newRobotise != null)
            try{
                float robotise = Float.parseFloat(newRobotise);
                mRobotise = robotise * 100.0f; //scale by maximum Robot parameter
            }
            catch(NumberFormatException nfe)
            {
                cLogger.log(Level.SEVERE,
                        "could not change robotise amount: {0} is not a float value",
                        newRobotise);
                return false;
            }
        
        
        updateVoiceEffects();
        
        
        //----------------------------------------------------------------------
        // prosody tag
        //----------------------------------------------------------------------
        String newPitch = params.get("pitch");
        if(newPitch != null)
            mPitch = newPitch;
        
        
        String newRate = params.get("rate");
        if(newRate != null)
            try{
                float rate = Float.parseFloat(newRate);
                mRate = rate;
            }
            catch(NumberFormatException nfe)
            {
                cLogger.log(Level.SEVERE,
                        "could not change speech rate: {0} is not a float value",
                        newRate);
                return false;
            }
        
        
        
        
        return true;
    }

    private void updateVoiceEffects()
    {        
        //set effects on the MaryInterface:
        String maryEffects =
                  "Volume(amount:"+mVolume+";)"
                + ",F0Scale(f0Scale:"+mRange+";)"
                + ",F0Add(f0Add:"+mF0Add+";)"
                + ",Robot(amount:"+mRobotise+";)";
                
        mAcoustParamsInterface.setAudioEffects(maryEffects);
        mAudioInterface.setAudioEffects(maryEffects);
    }
    

    private Document createMaryXML(String text)
    {
        cLogger.log(Level.FINE, "creating MaryXML for text \"{0}\"", text);
        
        try{
            Document doc = mDocBuilder.newDocument();

            //------------------------------------------------------------------
            Element root = doc.createElement("maryxml");
            doc.appendChild(root);
            
            Attr versionAttr = doc.createAttribute("version");
            versionAttr.setValue("0.4");
            root.setAttributeNode(versionAttr);
            
            Attr nsAttr = doc.createAttribute("xmlns");
            nsAttr.setValue("http://mary.dfki.de/2002/MaryXML");
            root.setAttributeNode(nsAttr);
            
            Attr nsxsiAttr = doc.createAttribute("xmlns:xsi");
            nsxsiAttr.setValue("http://www.w3.org/2001/XMLSchema-instance");
            root.setAttributeNode(nsxsiAttr);
            
            Attr langAttr = doc.createAttribute("xml:lang");
            langAttr.setValue(mLocale);
            root.setAttributeNode(langAttr);

            //------------------------------------------------------------------
            
            Element prosody = doc.createElement("prosody");
            root.appendChild(prosody);

            Attr rateAttr = doc.createAttribute("rate");
            rateAttr.setValue(""+mRate);
            prosody.setAttributeNode(rateAttr);

            Attr pitchAttr = doc.createAttribute("pitch");
            pitchAttr.setValue(mPitch);
            prosody.setAttributeNode(pitchAttr);

            
            prosody.setTextContent(text);
            
            //------------------------------------------------------------------
            // DEBUG
            //------------------------------------------------------------------
            
            /*
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            
            // write the content into xml file
            StreamResult result = new StreamResult(new File("./log/debug_"+System.currentTimeMillis()+".xml"));
            transformer.transform(source, result);
         
            // Output to console for testing
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
            */
            
            return doc;
        }
        catch(Exception e)
        {
            cLogger.log(Level.SEVERE, "could not create XML document: {0}",
                        e.toString());
            return null;
        }
    }
    
    
    /**
     * Analyzes the input, sends it to the TTS and generates the necessary events.
     * 
     * NOTE: Currently, the bookmark events are generated based on the assumption that the word preceding the bookmark is the first occurrence of the word.
     * @param job 
     */
    @Override
    protected void processJob(SpeechJob job) {
        if (mAcoustParamsInterface == null) {
            return;
        }

        long processingStartTime = System.currentTimeMillis();
        String text = job.getText();
        
        
        //----------------------------------------------------------------------
        // extract the bookmarks
        // NOTE: This only works on the assumption that the marker's predecessor is
        // the first occurrence of that word!
        //----------------------------------------------------------------------
        mBookmarks.clear();
        boolean stillSpeaking = true;

        //extract bookmarks
        String cleanText;
        cleanText = text.replaceAll(mBookmarkPattern.pattern(), "");
        Document maryXML = createMaryXML(cleanText);
        
        BookmarkCalculatorThread bookmarkThread =
                new BookmarkCalculatorThread(mAcoustParamsInterface,
                        text, maryXML, mBookmarks, mBookmarkPattern.pattern());
        
        AudioRetrieverThread audioThread =
                new AudioRetrieverThread(mAudioInterface, cleanText, maryXML,
                        mChannel, this);
        
        
        bookmarkThread.start();
        audioThread.start();
        
        try{
            bookmarkThread.join();
            audioThread.join();
        } catch (InterruptedException ex) {
            cLogger.log(Level.WARNING, "interrupted while waiting for TTS generation",
                    ex.toString());
        }

        mAudioPlayer = audioThread.getAudioPlayer();
        if(mAudioPlayer != null)
            mAudioPlayer.start();
        else
        {
            cLogger.log(Level.SEVERE, "could not generate TTS output");
            setSpeaking(false);                
            SpeechEvent evt = new SpeechEvent(this, SpeechEvent.SpeechEventType.FAILED);
            notifyListeners(evt);
            return;
        }
        
        long processingReadyTime = System.currentTimeMillis() - processingStartTime;
        cLogger.log(Level.INFO, "speech job ready after {0} ms", processingReadyTime);
        
        //----------------------------------------------------------------------
        // report bookmarks at the precalculated time
        //----------------------------------------------------------------------
        Set<Entry<Integer, Long>> entries = mBookmarks.entrySet();

        //wait for start
        boolean speaking = false;
        synchronized(mSpeakingLock){
            speaking = isSpeaking();
            if(!speaking)
            {
                try{
                    cLogger.log(Level.INFO, "waiting for speech to start");
                    mSpeakingLock.wait(10000);
                    speaking = isSpeaking();

                    if(!speaking)
                    {
                        cLogger.log(Level.SEVERE, "failed to start speech (time-out)");

                        //abort current command
                        mAudioPlayer.cancel();
                        setSpeaking(false);                
                        SpeechEvent evt = new SpeechEvent(this, SpeechEvent.SpeechEventType.FAILED);
                        notifyListeners(evt);
                        return;
                    }
                } catch (InterruptedException ex) {
                    cLogger.log(Level.WARNING, "interrupted while waiting for speech start");
                    return;
                }
            }
        }

        long startTime = System.currentTimeMillis();

        long waitTime = 0;

        for (Entry<Integer, Long> entry : entries) {
            waitTime = entry.getKey() - (System.currentTimeMillis() - startTime);
            //DEBUG
            cLogger.log(Level.INFO, "waiting for bookmark {0}: {1}",
                    new Object[]{entry.getValue(), waitTime});

            try {
                if (stillSpeaking && (waitTime > 0)) {
                    sleep(waitTime);
                }

                //check: has the speech task been stopped in the meantime?
                stillSpeaking = isSpeaking();

                if (stillSpeaking) {
                    cLogger.log(Level.INFO, "reached bookmark {0}", entry.getValue());
                    //send the bookmark signal now
                    SpeechEvent bookEvent = new SpeechEvent(job.getJobID(), SpeechEvent.SpeechEventType.BOOKMARK, entry.getValue());
                    notifyListeners(bookEvent);
                } else {
                    //DEBUG
                    long now = System.currentTimeMillis() - startTime;
                    cLogger.log(Level.WARNING, "reached end at time {0}, before all bookmarks were sent", now);
                    break;
                }
            } catch (InterruptedException ex) {
                cLogger.log(Level.WARNING, "interrupted during speech");
            }
        }
        
    }

    /**
     * Handles events from the AudioPlayer and translates them to SpeechEvents.
     *
     * @param event
     */
    @Override
    public void update(LineEvent event) {

        SpeechEvent speechEvent = null;

        //----------------------------------------------------------------------
        // audio started/stopped
        //----------------------------------------------------------------------
        if (event.getType() == LineEvent.Type.START) {
            cLogger.log(Level.INFO, "audio started playing");
            speechEvent = new SpeechEvent(this, SpeechEvent.SpeechEventType.STARTED);
            setSpeaking(true);
        } else if (event.getType() == LineEvent.Type.STOP) {
            cLogger.log(Level.INFO, "audio stopped playing");
            speechEvent = new SpeechEvent(this, SpeechEvent.SpeechEventType.STOPPED);
            setSpeaking(false);
        }
        //----------------------------------------------------------------------
        // line open/closed
        //----------------------------------------------------------------------
        /*
        else if (event.getType() == LineEvent.Type.OPEN) {
            cLogger.log(Level.FINER, "audio line opened");
        } else if (event.getType() == LineEvent.Type.CLOSE) {
            cLogger.log(Level.FINER, "audio line closed");
        }
        */

        //----------------------------------------------------------------------
        if (speechEvent != null) {
            notifyListeners(speechEvent);
        }
    }

    @Override
    public void cancelCurrentSpeech() {
        super.cancelCurrentSpeech();
        if (mAudioPlayer != null) {
            mAudioPlayer.cancel();
        }
    }

    @Override
    public void cancelAllSpeech() {
        super.cancelAllSpeech();
        if (mAudioPlayer != null) {
            mAudioPlayer.cancel();
        }
    }

    @Override
    protected void cleanup() {
        //nothing to do yet
    }

}
