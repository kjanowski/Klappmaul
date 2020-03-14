package de.kmj.robots.klappmaul.blackBoxAPI.speech.MIDI;

import java.util.Arrays;
import java.util.logging.Level;

/**
 *
 * @author Kathrin Janowski
 */
public class SpeechFragment {
    
    public String text;
    public long markerID;
    public char[] soundsCode;
    public int pitch;
    
    //==========================================================================
    // sound type codes
    //--------------------------------------------------------------------------
    //
    // Note:
    // These codes are assigned completely arbitrarily.
    // All that matters is that they are unique single characters.
    //
    // For easier debugging, I chose characters from the matching category
    // and sorted them alphabetically.
    //==========================================================================
    
    public static final char VOWEL_SHORT = 'A';
    public static final char VOWEL_MEDIUM = 'E';
    public static final char VOWEL_LONG = 'I';
    
    public static final char CONSONANT_SOFT_SHORT = 'L';
    public static final char CONSONANT_SOFT_MEDIUM = 'M';
    public static final char CONSONANT_SOFT_LONG = 'N';
    
    //public static final char CONSONANT_HARD_SHORT = 'K';
    
    //public static final char CONSONANT_HISSED_SHORT = 'S';
    //public static final char CONSONANT_HISSED_MEDIUM = 'X';
    //public static final char CONSONANT_HISSED_LONG = 'Z';
    
    //--------------------------------------------------------------------------
    
    public static final String NON_CODE_CHARS = "[^"
                +VOWEL_SHORT+VOWEL_MEDIUM+VOWEL_LONG
                +CONSONANT_SOFT_SHORT+CONSONANT_SOFT_MEDIUM+CONSONANT_SOFT_LONG                
                +"]";

    //==========================================================================
    //
    //==========================================================================
    
    public SpeechFragment(String text, long marker)
    {
        this.text = text;
        this.markerID = marker;
        this.pitch = -1;
        extractSounds(text);
    }
    
    
     /**
     *
     * @param fragment the original text
     * @return a String with the soundss encoded according to the following
     * pattern:
     * <ul><li>A: stretched sound</li><li>B: normal sound</li><li>C: shortened
     * sound</li></ul>
     */
    private void extractSounds(String fragment) {
        if (fragment == null) {
            this.soundsCode = new char[0];
            return;
        }
        
        String sounds = fragment.toLowerCase();
        sounds = sounds.replaceAll("[aeiouäöü]h", ""+VOWEL_LONG);  //vowels stretched by "h"
        sounds = sounds.replaceAll("(ai|au|ei|eu|ie|oi|ui)", ""+VOWEL_MEDIUM); //vowel pairs commonly mapped to one sound
        sounds = sounds.replaceAll("(aa|ee|ii|oo|uu|ää|öö|üü)", ""+VOWEL_LONG);  //vowels stretched by doubling
        sounds = sounds.replaceAll("[aeiouäöüy]", ""+VOWEL_MEDIUM); //all remaining vowels mapped to one sound
        sounds = sounds.replaceAll("B(cc|ck|dd|dt|ff|gg|kk|ll|mm|nn|pp|rr|ss|tt|xx|zz)", VOWEL_SHORT+"$1"); //shortened vowels
        
        sounds = sounds.replaceAll("mmm|nnn", ""+CONSONANT_SOFT_LONG); //soft consonants
        sounds = sounds.replaceAll("mm|nn|m|n", ""+CONSONANT_SOFT_SHORT); //soft consonants
        
        
        sounds = sounds.replaceAll(NON_CODE_CHARS, ""); //ignore everything else

        this.soundsCode = sounds.toCharArray();
    }
    
    
    
    @Override
    public String toString()
    {
        String result =
            String.format("[text:\"%s\", markerID:\"%s\", soundsCode=\"%s\", pitch=%d]",
                            text, markerID, Arrays.toString(soundsCode), pitch);
        return result;
    }
}
