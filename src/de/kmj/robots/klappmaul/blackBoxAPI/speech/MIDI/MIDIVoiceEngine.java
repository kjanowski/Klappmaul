package de.kmj.robots.klappmaul.blackBoxAPI.speech.MIDI;


import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEngine;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechJob;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

/**
 * Simulates a TTS engine on the Klappmaul using MIDI sounds.
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
public class MIDIVoiceEngine extends SpeechEngine{
    
    private static final Logger cLogger =
            Logger.getLogger(MIDIVoiceEngine.class.getName());
    
    private static final Random sRandom = new Random();
    
    //pattern syntax notes:
    //- "." loses special meaning inside [] -> must not be escaped
    //- "-" gains special meaning inside [] -> must be escaped
    private static final String cPunctuationRegex = ",;:\\-.?!";
    private static final Pattern cPhrasePattern = Pattern.compile("([^"+cPunctuationRegex+"]+)(["+cPunctuationRegex+"]+)\\s*");
    
    private Synthesizer mSynthesizer;
    private Receiver mReceiver;
    private ShortMessage mNoteOnMessage;
    private ShortMessage mNoteOffMessage;
    
    private MIDIVoiceConfig mVoiceConfig;
    
    //--------------------------------------------------------------------------
    // constructors
    //
    // <editor-fold desc="// ..." defaultstate="collapsed">
    public MIDIVoiceEngine() throws MidiUnavailableException, InvalidMidiDataException {
        super();
        
        cLogger.setLevel(Level.INFO);
        mVoiceConfig = new MIDIVoiceConfig();
        
        initMIDI();
    }
    
    public MIDIVoiceEngine(Properties config) throws MidiUnavailableException, InvalidMidiDataException {
        super(config);
        
        cLogger.setLevel(Level.INFO);
        
        mVoiceConfig = new MIDIVoiceConfig(config);
        
        initMIDI();
    }
    
    private void initMIDI() throws MidiUnavailableException, InvalidMidiDataException {
        mSynthesizer = MidiSystem.getSynthesizer();
        mSynthesizer.open();
        mReceiver = mSynthesizer.getReceiver();

        //setting the instrument which is used for the voice
        MidiChannel[] channels = mSynthesizer.getChannels();
        channels[0].programChange(mVoiceConfig.vowelBank, mVoiceConfig.vowelProgram);
        channels[1].programChange(mVoiceConfig.humBank, mVoiceConfig.humProgram);
    }

    // </editor-fold>
    //--------------------------------------------------------------------------
    
    @Override
    protected void processJob(SpeechJob job)
    {
        int duration;
        String fullText = job.getText();
        
        String[] phrases = getPhrases(fullText);

        int phraseIdx = 0;
        int fragmentIdx;

        //----------------------------------------------------------------------
        // start speaking
        //----------------------------------------------------------------------
        boolean speechRunning = true;
        setSpeaking(true);
        SpeechEvent startEvent
                = new SpeechEvent(job.getJobID(), SpeechEvent.SpeechEventType.STARTED);
        notifyListeners(startEvent);

        
        while(speechRunning && (phraseIdx < phrases.length))
        {
            SpeechFragment[] fragments = calculateProsody(phrases[phraseIdx]);
            
            fragmentIdx = 0;
            while(speechRunning && (fragmentIdx < fragments.length))
            {
                SpeechFragment fragment = fragments[fragmentIdx];
                
                if(fragment.text != null)
                {
                    //send word event
                    SpeechEvent wordEvent
                        = new SpeechEvent(job.getJobID(),
                                          SpeechEvent.SpeechEventType.WORD_STARTED,
                                          fragment.text);
                    notifyListeners(wordEvent);

                    if((fragment.soundsCode != null) && (fragment.pitch > -1))
                    {
                        //play a note for each vowel
                        int count = fragment.soundsCode.length;

                        long onTime, offTime;
                        //long seed = fragments.length*duration;
                        //        sRandom.setSeed(seed);

                        cLogger.log(Level.FINER, "playing syllables: {0}",
                                fragment.soundsCode);
                        int oldChannelNumber = -1;
                        
                        for(int i=0; i<count; i++)
                        {
                            int syllableLength;
                            int channelNumber;

                            char syllable = fragment.soundsCode[i]; 
                            switch(syllable){
                                case SpeechFragment.VOWEL_SHORT:
                                    syllableLength = (int)(mVoiceConfig.vowelLength *0.75f);
                                    channelNumber = 0;
                                    break;
                                case SpeechFragment.VOWEL_MEDIUM:
                                    syllableLength = mVoiceConfig.vowelLength;
                                    channelNumber = 0;
                                    break;
                                case SpeechFragment.VOWEL_LONG:
                                    syllableLength = mVoiceConfig.vowelLength *2;
                                    channelNumber = 0;
                                    break;
                                
                                case SpeechFragment.CONSONANT_SOFT_SHORT:
                                    syllableLength = (int)(mVoiceConfig.vowelLength *0.75f);
                                    channelNumber = 1;
                                    break;
                                case SpeechFragment.CONSONANT_SOFT_MEDIUM:
                                    syllableLength = mVoiceConfig.vowelLength;
                                    channelNumber = 1;
                                    break;
                                case SpeechFragment.CONSONANT_SOFT_LONG:
                                    syllableLength = mVoiceConfig.vowelLength *2;
                                    channelNumber = 1;
                                    break;
                                    
                                default:
                                    syllableLength = 0;
                                    channelNumber = -1;
                                    break;
                            }
                            cLogger.log(Level.FINEST, "{0} -> duration: {1}",
                                        new Object[]{syllable, syllableLength});
                            
                            //--------------------------------------------------
                            
//                            if(oldChannelNumber > -1)
//                            {
//                                try{
//                                    //TODO stop old?
//                                    offTime = mSynthesizer.getMicrosecondPosition();
//                                    mNoteOffMessage = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 123, mVoiceConfig.downVelocity);
//                                    mReceiver.send(mNoteOffMessage, offTime);
//                                }catch(InvalidMidiDataException e)
//                                {
//                                    cLogger.log(Level.WARNING, "failed to stop old note: {0}", e.toString());
//                                }
//                            }
                            oldChannelNumber = channelNumber;
                            
                            //--------------------------------------------------
                            
                            if(channelNumber > -1)
                            {
                                //TODO add a bit of variation to the syllable pitch?
                                //- noise?
                                //- emphasis?
                                int finalPitch = fragment.pitch;

                                try {
                                    mNoteOnMessage = new ShortMessage(ShortMessage.NOTE_ON, channelNumber, finalPitch, mVoiceConfig.downVelocity);
                                    onTime = mSynthesizer.getMicrosecondPosition();
                                    mReceiver.send(mNoteOnMessage, onTime);
                                    sleep(syllableLength);
                                } catch (InvalidMidiDataException ex) {
                                    cLogger.log(Level.SEVERE, "could not play note: {0}", ex.toString());
                                } catch (InterruptedException ex) {
                                    cLogger.log(Level.FINE, "interrupted while speaking");
                                    setSpeaking(false);
                                }
                            }
                        }
                        
                        try{
                            //end all notes after the fragment
                            offTime = mSynthesizer.getMicrosecondPosition();
                            mNoteOffMessage = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 123, mVoiceConfig.downVelocity);
                            mReceiver.send(mNoteOffMessage, offTime);
                            mNoteOffMessage = new ShortMessage(ShortMessage.CONTROL_CHANGE, 1, 123, mVoiceConfig.downVelocity);
                            mReceiver.send(mNoteOffMessage, offTime);
                        } catch (InvalidMidiDataException ex) {
                            Logger.getLogger(MIDIVoiceEngine.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        //leave a little pause before the next fragement or sentence
                        try {
                            sleep(mVoiceConfig.pauseLength);
                        }
                        catch (InterruptedException ex) {
                            cLogger.log(Level.FINE, "interrupted while pausing after the fragment");
                            setSpeaking(false);
                        }
                    }//sounds played
                }//text processed
                
                if(fragment.markerID > -1)
                {
                    //send bookmark event
                    SpeechEvent bookEvent
                        = new SpeechEvent(job.getJobID(),
                                          SpeechEvent.SpeechEventType.BOOKMARK,
                                          fragment.markerID);
                    notifyListeners(bookEvent);
                }
                
                fragmentIdx++;
                speechRunning = isSpeaking();
            }
            
            phraseIdx++;
            speechRunning = isSpeaking();
        }
        
        
        //----------------------------------------------------------------------
        // speech task finished
        //----------------------------------------------------------------------

        setSpeaking(false);
        SpeechEvent stopEvent
                = new SpeechEvent(job.getJobID(),
                                  SpeechEvent.SpeechEventType.STOPPED);
        notifyListeners(stopEvent);
    }

    
    //==========================================================================
    // analyzing the speech command
    //
    // <editor-fold desc="// ..." defaultstate="collapsed">
    private String[] getPhrases(String text) {        
        ArrayList<String> phrases = new ArrayList<>();

        //----------------------------------------------------------------------
        // separate by punctuation
        //----------------------------------------------------------------------
        Matcher phraseMatcher = cPhrasePattern.matcher(text);
        boolean hasNextPhrase = phraseMatcher.find(0);
        int end = 0;
        while (hasNextPhrase) {
            String next = phraseMatcher.group();
            end = phraseMatcher.end();
            cLogger.log(Level.FINE, "next phrase: {0}", next);
            phrases.add(next);
            
            hasNextPhrase = phraseMatcher.find();
            
        }
        
        if (end<text.length()) {
            cLogger.log(Level.FINER, "text length: {0}, end of last match: {1}",
                    new Object[]{text.length(), end});
            if ((end > 0) && (end < text.length())) {
                String rest = text.substring(end).trim();
                phrases.add(rest);
                cLogger.log(Level.FINE, "rest phrase: {0}", rest);
            }
        }
        String[] result = new String[phrases.size()]; 
        result = phrases.toArray(result);
        return result;
    }    
    
    
    private SpeechFragment[] calculateProsody(String phrase) {
        cLogger.log(Level.FINE, "analyzing phrase \"{0}\"", phrase);

        phrase = normalizeText(phrase);
        //------------------------------------------------------------------
        // identify punctuation
        //------------------------------------------------------------------
        int fullStop = 0;
        int question = 0;
        int exclamation = 0;

        //extract all punctuation marks
        Matcher punctMatcher = cPhrasePattern.matcher(phrase);
        
        String punctuation;
        if(punctMatcher.matches()) {
            phrase = punctMatcher.group(1);
            punctuation = punctMatcher.group(2);
        } else {
            punctuation = "";
        }
        cLogger.log(Level.FINE,
            "phrase: \"{0}\" punctuation: \"{1}\"",
            new Object[]{phrase, punctuation});
        
        
        if (punctuation.length() > 0) {
            int count = punctuation.length();
            
            for (int p = 0; p < count; p++) {
                char punctChar = punctuation.charAt(p);
                switch (punctChar) {
                    case '.':
                        fullStop++;
                        break;
                    case '?':
                        question++;
                        break;
                    case '!':
                        exclamation++;
                        break;
                    default:
                        //ignore
                        break;
                }
            }
            
            cLogger.log(Level.FINER, "punctuation for phrase \"{0}\":"
                    + "\n\tfullStop: {1}, question: {2}, exclamation: {2}",
                    new Object[]{phrase, fullStop, question, exclamation});
        }
        else cLogger.log(Level.FINER, "no punctuation marks detected.");

        //--------------------------------------------------------------
        // default: minimal rise to indicate incompleteness
        float prosodyScore = 0.2f;
        
        int punctSum = fullStop + question + exclamation;
        if (punctSum > 0) {
            //overwrite according to punctuation types
            prosodyScore = -1.0f * fullStop + 0.5f * question + 1.0f * exclamation;
        }
        
        cLogger.log(Level.FINE, "prosody score: {0}", prosodyScore);

        //--------------------------------------------------------------
        // extract the fragments from this phrase
        //--------------------------------------------------------------
        ArrayList<SpeechFragment> fragments = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(phrase);
        
        while (tokenizer.hasMoreTokens()) {
            String token  = tokenizer.nextToken();
            String text = null;
            
            long bookId = getBookmarkID(token);
            if(bookId == -1)
            {
                text = token;
            }
            
            if((text != null)||(bookId>-1))
            {
                SpeechFragment fragment
                    = new SpeechFragment(text, bookId);
                fragments.add(fragment);
            }
        }

        //----------------------------------------------------------------------
        // calculate prosody for each fragment
        //----------------------------------------------------------------------
        int numFragments = fragments.size();
        
        int i = 0;
        float bendDurationFl = Math.round(numFragments * 0.25f);
        int bendDuration = Integer.max(1, (int) (bendDurationFl));
        int bendStart = numFragments - bendDuration;
        
        cLogger.log(Level.FINE,
                "numFragments = {0}, bendDuration = {1} ({2}), bendStart = {3}",
                new Object[]{numFragments, bendDuration, bendDurationFl, bendStart});
        
        while (i < bendStart) {
            fragments.get(i).pitch = mVoiceConfig.basePitch;
            i++;
        }
        
        float timeFactor;
        float fragmentProsody;
        int sign;
        int index;
        int octave;
        while (i < numFragments) {
            timeFactor = (i - bendStart + 1) / (float) bendDuration;
            fragmentProsody = Math.round(prosodyScore * timeFactor);
            
            sign = (int) Math.signum(fragmentProsody);
            index = (int) Math.abs(fragmentProsody);
            octave = index / 8 * sign;
            if (sign < 0) {
                octave--;
                index = 7 - (index % 8);
            }
            index = index % 8;
            
            cLogger.log(Level.FINE,
                    "base pitch = {0} ({1}), index = {2}, octave = {3}",
                    new Object[]{
                        mVoiceConfig.basePitch, mVoiceConfig.pitchScale.getScaleTone(0, 0),
                        index, octave
                    }
            );
            
            SpeechFragment fragment = fragments.get(i);
            fragment.pitch = mVoiceConfig.pitchScale.getScaleTone(index, octave);
            cLogger.log(Level.FINE,
                    "timeFactor = {0}, prosodyScore = {1} => scale tone = {2} {3}, fragments[{4}].pitch = {5}",
                    new Object[]{
                        timeFactor, prosodyScore,
                        MIDIScale.getToneName(fragment.pitch), octave,
                        i, fragment.pitch
                    }
            );
            i++;
        }

        //----------------------------------------------------------------------
        
        if(punctuation.length() > 0)
        {
            //re-append punctuation to final text fragment
            int lastIndex = fragments.size()-1;
            if(lastIndex > -1)
            {
                SpeechFragment last = fragments.get(lastIndex);
                while ((last.text == null) && (lastIndex > 0))
                {
                    lastIndex--;
                    last = fragments.get(lastIndex);
                }
                last.text = last.text + punctuation;
            }
            else{
                fragments.add(new SpeechFragment(punctuation, -1));
            }
        }
        
        SpeechFragment[] result = new SpeechFragment[fragments.size()];
        result = fragments.toArray(result);
        
        //cLogger.log(Level.FINE, "fragments: {0}", Arrays.toString(result));
        return result;
    }

    /**
     * Ensures that the text can be properly converted to its audio representation.
     * @param text the original text
     * @return 
     */
    private String normalizeText(String text)
    {
        //----------------------------------------------------------------------
        // general formatting
        //----------------------------------------------------------------------
        
        //remove whitespace outside the text and reduce remaining whitespace to one single space
        text = text.trim().replaceAll("\\s+", " ");
        cLogger.log(Level.FINER, "removed unnecessary whitespace: \"{0}\"", text);
        
        //normalize markers: ensure that they contain no whitespace
        //and are separated from the surrounding fragments.
        //(if the markerID itself contains whitespaces,
        //everything after the first whitespace is discarded)
        text = text.replaceAll("\\s?\\\\\\s?mrk\\s?=\\s?(\\S+)\\s?\\\\\\s?", " \\\\mrk=$1\\\\ ");
        cLogger.log(Level.FINER, "normalized markers: \"{0}\"", text);

        //remove whitespace between punctuation and preceding fragment
        text = text.replaceAll("\\s?([" + cPunctuationRegex + "])", "$1");
        cLogger.log(Level.FINER, "removed whitespace preceding punctuation: \"{0}\"", text);

        //----------------------------------------------------------------------
        // prosody-specific
        //----------------------------------------------------------------------
        
        //ellipse: "..." -> "…"
        text = text.replaceAll("\\.{3,}", "…");

        
        //----------------------------------------------------------------------
        return text;
    }
    
    
   

    // </editor-fold>
    //==========================================================================
    
    
    //==========================================================================
    // sound configuration
    //
    // <editor-fold desc="// ..." defaultstate="collapsed">
    /**
     * Adds random noise to the given pitch with a probability of 40%.
     */
    private int getNoisyPitch(double basePitch) {
        int finalPitch;

        //add or substract noise?
        int sign;
        float vary = sRandom.nextFloat();
        if (vary < 0.2f) {
            sign = -1;
        } else if (vary < 0.8f) {
            sign = 0;
        } else {
            sign = 1;
        }

        //add some slight noise
        float noise = sign * sRandom.nextFloat() * mVoiceConfig.pitchRange;
        
        finalPitch = (int) Math.round(basePitch + noise);
        cLogger.log(Level.FINE,
                "pitch = {0}, vary = {1}, noise = {2}, finalPitch = {3}",
                new Object[]{basePitch, vary, noise, finalPitch}
        );
        
        return finalPitch;
    }
    
    @Override
    public boolean setVoice(TreeMap<String, String> params) {
        String newPitch = params.get("pitch");
        if (newPitch != null) {
            try {
                int basePitch = Integer.parseInt(newPitch);
                mVoiceConfig.basePitch = basePitch % 128;
                mVoiceConfig.pitchScale = new MIDIScale(basePitch, true);
                cLogger.log(Level.INFO, "new base pitch: {0}", mVoiceConfig.basePitch);
            } catch (NumberFormatException nfe) {
                //try to interpret it as a tone name
                int namedPitch = MIDIScale.getTone(newPitch, 0);
                if (namedPitch > -1) {
                    mVoiceConfig.basePitch = namedPitch;
                    mVoiceConfig.pitchScale = new MIDIScale(namedPitch, true);
                } else {
                    cLogger.log(Level.SEVERE,
                            "invalid parameter for base pitch: {0} is neither an integer nor a note name",
                            newPitch);
                    return false;
                }
            }
        }
        
        String newPitchRange = params.get("pitchRange");
        if (newPitchRange == null) {
            newPitchRange = params.get("range");
        }
        if (newPitchRange != null) {
            try {
                int pitchRange = Integer.parseInt(newPitchRange);
                mVoiceConfig.pitchRange = pitchRange;
                cLogger.log(Level.INFO, "new pitch range: {0}", mVoiceConfig.pitchRange);
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for pitch range: {0} is not an integer",
                        newPitchRange);
                return false;
            }
        }
        
        String newDownVelocity = params.get("downVelocity");
        if (newDownVelocity == null) {
            newDownVelocity = params.get("downVel");
        }
        if (newDownVelocity != null) {
            try {
                int downVelocity = Integer.parseInt(newDownVelocity);
                mVoiceConfig.downVelocity = downVelocity;
                cLogger.log(Level.INFO, "new key down velocity: {0}", mVoiceConfig.downVelocity);
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for key down velocity: {0} is not an integer",
                        newDownVelocity);
                return false;
            }
        }
        
        String newUpVelocity = params.get("upVelocity");
        if (newUpVelocity == null) {
            newUpVelocity = params.get("upVel");
        }
        if (newUpVelocity != null) {
            try {
                int upVelocity = Integer.parseInt(newUpVelocity);
                mVoiceConfig.upVelocity = upVelocity;
                cLogger.log(Level.INFO, "new key up velocity: {0}", mVoiceConfig.upVelocity);
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for key up velocity: {0} is not an integer",
                        newUpVelocity);
                return false;
            }
        }
        
        boolean changeInstrument = false;
        
        String newBank = params.get("vowelBank");
        if (newBank != null) {
            try {
                int bank = Integer.parseInt(newBank);
                mVoiceConfig.vowelBank = bank;
                cLogger.log(Level.INFO, "new instrument bank: {0}", mVoiceConfig.vowelBank);
                changeInstrument = true;
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for vowel bank: {0} is not an integer",
                        newBank);
                return false;
            }
        }
        
        String newProgram = params.get("vowelProgram");
        if (newProgram != null) {
            try {
                int program = Integer.parseInt(newProgram);
                mVoiceConfig.vowelProgram = program;
                cLogger.log(Level.INFO, "new vowel program: {0}", mVoiceConfig.vowelProgram);
                changeInstrument = true;
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for vowel program: {0} is not an integer",
                        newProgram);
                return false;
            }
        }

        String newHumBank = params.get("humBank");
        if (newHumBank != null) {
            try {
                int bank = Integer.parseInt(newHumBank);
                mVoiceConfig.humBank = bank;
                cLogger.log(Level.INFO, "new hum bank: {0}", mVoiceConfig.humBank);
                changeInstrument = true;
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for hum bank: {0} is not an integer",
                        newHumBank);
                return false;
            }
        }
        
        String newHumProgram = params.get("humProgram");
        if (newHumProgram != null) {
            try {
                int program = Integer.parseInt(newHumProgram);
                mVoiceConfig.humProgram = program;
                cLogger.log(Level.INFO, "new hum program: {0}", mVoiceConfig.humProgram);
                changeInstrument = true;
            } catch (NumberFormatException nfe) {
                cLogger.log(Level.SEVERE,
                        "invalid parameter for hum program: {0} is not an integer",
                        newHumProgram);
                return false;
            }
        }


        
        if (changeInstrument) {
            cLogger.log(Level.INFO, "changing vowel instrument: bank {0}, program {1}",
                    new Object[]{mVoiceConfig.vowelBank, mVoiceConfig.vowelProgram});
            mSynthesizer.getChannels()[0].programChange(mVoiceConfig.vowelBank, mVoiceConfig.vowelProgram);

            cLogger.log(Level.INFO, "changing hum instrument: bank {0}, program {1}",
                    new Object[]{mVoiceConfig.humBank, mVoiceConfig.humProgram});
            mSynthesizer.getChannels()[1].programChange(mVoiceConfig.humBank, mVoiceConfig.humProgram);            
        }
        
        return true;
    }

    // </editor-fold>
    //==========================================================================

    @Override
    protected void cleanup() {
        //nothing to do
    }
}
