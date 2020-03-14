package de.kmj.robots.klappmaul.blackBoxAPI.speech.MIDI;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kathrin Janowski
 */
public class MIDIVoiceConfig {
    private static final Logger cLogger =
            Logger.getLogger(MIDIVoiceConfig.class.getName());
    
    
    /** The time (in milliseconds) required for speaking a vowel. */
    public int vowelLength;

    /** The time (in milliseconds) to wait between words. */
    public int pauseLength;

    /** The "key down" velocity for starting a note. */
    public int downVelocity;

    /** The "key up" velocity for ending a note. */
    public int upVelocity;
    
    /** The base pitch of the voice. */
    public int basePitch;

    /** The scale based on the base pitch. */
    public MIDIScale pitchScale;

    
    /** The pitch range of the voice. */
    public int pitchRange;

    /** The bank of the MIDI instrument for vowel sounds. */
    public int vowelBank;
    
    /** The program of the MIDI instrument for vowel sounds. */
    public int vowelProgram;

    /** The bank of the MIDI instrument for "m" and "n" sounds. */
    public int humBank;
    
    /** The program of the MIDI instrument for "m" and "n" sounds. */
    public int humProgram;
    
    
    //==========================================================================
    
    public MIDIVoiceConfig()
    {
        vowelLength = 250;
        pauseLength = 300;
        downVelocity = 100;
        upVelocity = 0;
        basePitch = 60;
        pitchScale = new MIDIScale(basePitch, true);
        pitchRange = 6;
        vowelBank = 0;
        vowelProgram = 54;
        humBank = 2048;
        humProgram = 63;
    }
    
    
    
    public MIDIVoiceConfig(Properties config)
    {
        String vowelLengthStr = config.getProperty("speech.vowelLength");        
        try {
            vowelLength = Integer.parseInt(vowelLengthStr);
        } catch (NullPointerException | NumberFormatException ne) {
            vowelLength = 250;
            cLogger.log(Level.WARNING, "invalid vowelLength parameter: {0}\n\t-> using default {1}",
                    new Object[]{vowelLengthStr, vowelLength});
        }
        
        String pauseLengthStr = config.getProperty("speech.pauseLength");        
        try {
            pauseLength = Integer.parseInt(pauseLengthStr);
        } catch (NullPointerException | NumberFormatException ne) {
            pauseLength = 300;
            cLogger.log(Level.WARNING, "invalid pauseLength parameter: {0}\n\t-> using default {1}",
                    new Object[]{pauseLengthStr, pauseLength});
        }
        
        String downVelStr = config.getProperty("speech.velocity.down");        
        try {
            downVelocity = Integer.parseInt(downVelStr);
        } catch (NullPointerException | NumberFormatException ne) {
            downVelocity = 100;
            cLogger.log(Level.WARNING, "invalid downward velocity parameter: {0}\n\t-> using default {1}",
                    new Object[]{downVelStr, downVelocity});
        }
        
        String upVelStr = config.getProperty("speech.velocity.up");        
        try {
            upVelocity = Integer.parseInt(upVelStr);
        } catch (NullPointerException | NumberFormatException ne) {
            upVelocity = 0;
            cLogger.log(Level.WARNING, "invalid upward velocity parameter: {0}\n\t-> using default {1}",
                    new Object[]{upVelStr, upVelocity});
        }
        
        String basePitchStr = config.getProperty("speech.pitch.base");        
        try {
            basePitch = Integer.parseInt(basePitchStr) % 128;
        } catch (NullPointerException | NumberFormatException ne) {
            //try to interpret it as a tone name
            int namedPitch = MIDIScale.getTone(basePitchStr, 0);
            if (namedPitch > -1) {
                basePitch = namedPitch;
            } else {
                basePitch = 60;
                cLogger.log(Level.WARNING, "invalid base pitch parameter: {0}\n\t-> using default {1}",
                        new Object[]{basePitchStr, basePitch});
            }
        }
        pitchScale = new MIDIScale(basePitch, true);
        
        String pitchRangeStr = config.getProperty("speech.pitch.range");        
        try {
            pitchRange = Integer.parseInt(pitchRangeStr);
        } catch (NullPointerException | NumberFormatException ne) {
            pitchRange = 6;
            cLogger.log(Level.WARNING, "invalid pitch range parameter: {0}\n\t-> using default {1}",
                    new Object[]{pitchRangeStr, pitchRange});
        }
        
        String vowelBankStr = config.getProperty("speech.instrument.vowel.bank");        
        try {
            vowelBank = Integer.parseInt(vowelBankStr);
        } catch (NullPointerException | NumberFormatException ne) {
            vowelBank = 0;
            cLogger.log(Level.WARNING, "invalid vowel bank parameter: {0}\n\t-> using default {1}",
                    new Object[]{vowelBankStr, vowelBank});
        }
        
        String vowelProgramStr = config.getProperty("speech.instrument.vowel.program");        
        try {
            vowelProgram = Integer.parseInt(vowelProgramStr);
        } catch (NullPointerException | NumberFormatException ne) {
            vowelProgram = 54;
            cLogger.log(Level.WARNING, "invalid vowel program parameter: {0}\n\t-> using default {1}",
                    new Object[]{vowelProgramStr, vowelProgram});
        }

        String humBankStr = config.getProperty("speech.instrument.hum.bank");        
        try {
            humBank = Integer.parseInt(humBankStr);
        } catch (NullPointerException | NumberFormatException ne) {
            humBank = 2048;
            cLogger.log(Level.WARNING, "invalid hum bank parameter: {0}\n\t-> using default {1}",
                    new Object[]{humBankStr, humBank});
        }
        
        String humProgramStr = config.getProperty("speech.instrument.hum.program");        
        try {
            humProgram = Integer.parseInt(humProgramStr);
        } catch (NullPointerException | NumberFormatException ne) {
            humProgram = 63;
            cLogger.log(Level.WARNING, "invalid hum program parameter: {0}\n\t-> using default {1}",
                    new Object[]{humProgramStr, humProgram});
        }
    }
    
    
    
}
