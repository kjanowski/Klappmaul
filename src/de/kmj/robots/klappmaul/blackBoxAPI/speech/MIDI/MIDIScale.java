package de.kmj.robots.klappmaul.blackBoxAPI.speech.MIDI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kathrin Janowski
 */
public class MIDIScale {

    private static final Logger cLogger = Logger.getLogger(MIDIScale.class.getName());
    
    private final String mBaseToneName;
    private final int mBaseTone;
    private final int mBaseOctave;
    private final boolean mIsMajor;
    
    private static final int[] cMajorOffsets =
            new int[]{ 0, 2, 4, 5, 7, 9, 11 };
    private static final int[] cMinorOffsets =
            new int[]{ 0, 2, 3, 5, 7, 8, 10 };
    
    
    public MIDIScale(String baseToneName, boolean isMajor)
    {
        mBaseToneName = normalizeToneName(baseToneName);
        mBaseOctave = 5;
        mBaseTone = getTone(mBaseToneName, 0);
        mIsMajor = isMajor;
        
        cLogger.setLevel(Level.ALL);
        cLogger.log(Level.INFO,
                    "base tone: {0}/{1}, base octave: {2}, scale is major: {3}",
                    new Object[]{mBaseTone, mBaseToneName, mBaseOctave, isMajor}
                );
    }

    public MIDIScale(int baseTone, boolean isMajor)
    {
        mBaseOctave = baseTone /12;
        mBaseTone = baseTone % 12;
        mBaseToneName = getToneName(mBaseTone);
        mIsMajor = isMajor;
        
        cLogger.setLevel(Level.ALL);
        cLogger.log(Level.INFO,
                    "base tone: {0}/{1}, baseOctave: {2}, scale is major: {3}",
                    new Object[]{mBaseTone, mBaseToneName, mBaseOctave, isMajor}
                );
    }

    
    //--------------------------------------------------------------------------
    // get scale notes
    //--------------------------------------------------------------------------
    /**
     * Gets the indexed tone from the scale.
     * 
     * The base octave (0) covers the MIDI pitch range [60; 71].
     * Note that the highest Octave (+5) ends at the tone G.
     * 
     * @param index the note index within the scale, from range [0; 6]
     * @param octave the relative octave from range [-5; +5] (octave 0 covers range [60; 71]) 
     * @return a MIDI pitch value from range [0; 127] if available, otherwise -1
     */
    public int getScaleTone(int index, int octave)
    {
        if((index <0) || (index>7))
            return -1;
        
        if(mIsMajor)
            return getRelativeTone(cMajorOffsets[index], octave);
        else return getRelativeTone(cMinorOffsets[index], octave);
    }


    /**
     * Gets the base tone (tonic) from the desired octave.
     * 
     * Note that the highest Octave (10) ends at the tone G.
     * 
     * @param absOctave the absolute octave from range [0; 10]
     * @return a MIDI pitch value from range [0; 127]
     */
    public int getBaseTone(int absOctave)
    {
        int result = mBaseTone + 12*absOctave;
        
        if((result <0) || (result > 127)) 
            result = -1;
        
        return result;
    }

    
    
    private int getRelativeTone(int semitoneOffset, int octaveOffset)
    {
        // calculate and cap absolute octave -----------------------------------
        
        int absOctave = mBaseOctave + octaveOffset;
        if(absOctave < 0)
        {
            cLogger.log(Level.WARNING,
                        "requested octave too low: {0} -> cap to 0",
                        absOctave);
            absOctave = 0;
        }
        else if(absOctave > 10)
        {
            cLogger.log(Level.WARNING,
                        "requested octave too high: {0} -> cap to 10",
                        absOctave);
            absOctave = 10;
        }
        
        // calculate requested tone --------------------------------------------
        
        int result = mBaseTone + semitoneOffset + 12*absOctave;
        cLogger.log(Level.INFO,
                    "relative tone: {0} + {1} semitones + {2} octaves -> {3}",
                    new Object[]{mBaseTone, semitoneOffset, absOctave, result}
                );
        
        
        // cap result ----------------------------------------------------------
        
        if(result < 0)
        {
            cLogger.log(Level.WARNING,
                        "requested tone too low: {0} -> cap to 0",
                        result);
            return 0;
        }
        else if(result > 127)
        {
            cLogger.log(Level.WARNING,
                        "requested tone too high: {0} -> cap to 127",
                        result);
            return 127;
        }
        else return result;
    }
    
    
    //--------------------------------------------------------------------------
    // static methods
    //--------------------------------------------------------------------------
    
    public static String normalizeToneName(String toneName)
    {
        //remove whitespace
        String normalized = toneName.replaceAll("\\s", "");
        //convert to lower case
        normalized = normalized.toLowerCase();
        
        return normalized;
    }
    
    
    /**
     * Translates a tone name to the MIDI pitch value from the given octave.
     * 
     * The base octave (0) covers the MIDI pitch range [60; 71].
     * Note that the highest octave (+5) ends at the tone G.
     * 
     * @param toneName consists of a single letter (a-g) and an optional '#'
     * @param octave the relative octave from range [-5; +5]
     * @return a MIDI pitch value from range [0; 127] if available, otherwise -1
     */
    public static int getTone(String toneName, int octave)
    {
        int result = -1;
        
        //normalize: lower case without any whitespace
        toneName = normalizeToneName(toneName);
        
        if(toneName.length()>2)
            return result;
        
        // translate name to tone in center octave
        switch(toneName.toLowerCase())
        {
            case "c":   result = 60;    break;
            case "c#":  result = 61;    break;
            case "d":   result = 62;    break;
            case "d#":  result = 63;    break;
            case "e":   result = 64;    break;
            case "f":   result = 65;    break;
            case "f#":  result = 66;    break;
            case "g":   result = 67;    break;
            case "g#":  result = 68;    break;
            case "a":   result = 69;    break;
            case "a#":  result = 70;    break;
            case "b":   result = 71;    break;
            default:    result = -1;    break;
        }
        
        //was the tone name recognized?
        if(result < 0)
            return -1;
        
        //move to desired octave
        result = result + 12*(5+octave);
        
        //final validation: within MIDI range?
        if((result <0) || (result > 127)) 
            result = -1;
        
        return result;
    }
    
    /**
     * Translates a MIDI pitch to a tone name.
     * 
     * Note that the octave information gets lost in this process.
     * 
     * @param tone a MIDI pitch value from range [0; 127] if available, otherwise -1
     * @return the tone name which consists of a single letter (a-g) and an optional '#'
     */
    public static String getToneName(int tone)
    {
        int normalizedTone = tone % 12; //map to range [0; 11]
        String result;
        
        // translate name to tone in center octave
        switch(normalizedTone)
        {
            case 0: result = "c";    break;
            case 1: result = "c#";   break;
            case 2: result = "d";    break;
            case 3: result = "d#";   break;
            case 4: result = "e";    break;
            case 5: result = "f";    break;
            case 6: result = "f#";   break;
            case 7: result = "g";    break;
            case 8: result = "g#";   break;
            case 9: result = "a";    break;
            case 10:result = "a#";   break;
            case 11:result = "b";    break;
            default:
                //this should not be possible after modulo
                cLogger.log(Level.SEVERE, "error: {0} modulo 12 resulted in value {1}",
                                new Object[]{tone, normalizedTone});
                result = null;
                break;
        }
        
        return result;
    }
    
}
