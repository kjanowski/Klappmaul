/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.speech.MIDI;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

/**
 *
 * @author Kathrin
 */
public class MidiTest {

    private Synthesizer synth;
    private Receiver recv;
    private Sequencer sequ;
    private HashMap<String, String> substitutions;
    private HashMap<Character, Integer> alphabet;
    private HashMap<Integer, Integer> channels;

    public MidiTest() throws MidiUnavailableException {
        synth = MidiSystem.getSynthesizer();
        synth.open();
        recv = synth.getReceiver();
        sequ = MidiSystem.getSequencer();
        sequ.open();
        prepareAlphabet();
    }

    public void exit() {
        if (synth != null) {
            //stop the previous note
            ShortMessage msgOff;
            try {
                msgOff = new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0);
                recv.send(msgOff, -1);
            } catch (InvalidMidiDataException ex) {
                Logger.getLogger(MidiTest.class.getName()).log(Level.SEVERE, null, ex);
            }

            synth.close();
        }
    }

    public void listInstruments() {
        Instrument[] instruments = synth.getAvailableInstruments();

        for (int i = 0; i < 128; i++) {
            System.out.println("instrument " + i + ": "
                    + "name = " + instruments[i].getName());
        }

    }

    public static void main(String[] args) {
        try {
            MidiTest test = new MidiTest();

            //test.listInstruments();

            boolean testing = true;
            Scanner scanner = new Scanner(System.in);
            while (testing) {
                System.out.println("enter a word or phrase: ");
                String input = scanner.nextLine();
                switch (input) {
                    case "exit":
                        testing = false;
                        break;
                    case "alphabet":
                        test.prepareAlphabet();
                        break;
                    case "channels":
                        test.testChannels();
                        break;
                    case "text":
                        test.mimicText(input);
                        break;
                    case "instrument":
                        System.out.println("enter a number (0-127)");
                        input = scanner.nextLine();
                    default:
                        test.testInstrument(input);
                        break;
                }
            }

            test.exit();
            System.out.println("testing finished.");
        } catch (MidiUnavailableException ex) {
            Logger.getLogger(MidiTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void testInstrument(String input) {
        Instrument[] instruments= synth.getLoadedInstruments();
        try {
            int index = Integer.parseInt(input);

            if ((index >= 0) && (index < instruments.length)) {
                Patch patch = instruments[index].getPatch();
                
                //assign instrument
                synth.getChannels()[0].programChange(patch.getBank(), patch.getProgram());

                //select a note to play
                int pitch = 60;
                int volume = 100;
         
                //stop the previous note
                ShortMessage msgOff = new ShortMessage(ShortMessage.NOTE_OFF, 0, pitch, volume);
                recv.send(msgOff, -1);
                
                //play the current note
                ShortMessage msgOn = new ShortMessage(ShortMessage.NOTE_ON, 0, pitch, volume);
                recv.send(msgOn, -1);

            } else {
                System.out.println("Please enter a number from the given range.");
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Please enter a number from the given range.");
        } catch (InvalidMidiDataException imde) {
            System.err.println(imde.toString());
        }
    }

    private void testChannels()
    {
        Instrument[] instruments = synth.getLoadedInstruments();

        for(int i=0; i<instruments.length; i++)
        {
            System.out.println("loaded Instrument "+i+": "+instruments[i].toString());
        }
        MidiChannel[] chs = synth.getChannels();
        for(int i=0; i<chs.length; i++)
        {
            int program = chs[i].getProgram();
            System.out.println("channel "+i+": program "+program);            
        }
    }
    
    
    public void prepareAlphabet()
    {
        substitutions = new HashMap<>();
        alphabet = new HashMap<>();
        
        alphabet.put('a', 50);
        substitutions.put("ä", "e");
        alphabet.put('b', 25);
        substitutions.put("c", "ts");
        substitutions.put("ch", "h");
        substitutions.put("ck", "k");
        substitutions.put("d", "t");
        alphabet.put('e', 20);
        alphabet.put('f', 121);
        substitutions.put("g", "k");
        alphabet.put('h', 121);
        substitutions.put("i", "e");
        alphabet.put('j', 120);
        alphabet.put('k', 13);
        alphabet.put('l', 112);
        alphabet.put('m', 23);
        substitutions.put("n", "m");
        alphabet.put('o', 52);
        substitutions.put("ö", "e");
        substitutions.put("p", "b");
        substitutions.put("qu", "kw");
        alphabet.put('r', 125);
        alphabet.put('s', 103);
        substitutions.put("ß", "s");
        alphabet.put('t', 115);
        alphabet.put('u', 72);
        substitutions.put("ü", "e");
        substitutions.put("v", "f");
        substitutions.put("w", "b");
        substitutions.put("x", "ks");
        substitutions.put("y", "i");
        substitutions.put("z", "ts");
        
        channels= new HashMap<>();
        int i=0;
        Instrument[] instruments = synth.getLoadedInstruments();
        MidiChannel[] ch = synth.getChannels();
        for(Integer instrIndex: alphabet.values())
        {
            if((instrIndex > 0)&&(instrIndex<instruments.length))
            {
                channels.put(instrIndex, i);
                Patch patch = instruments[instrIndex].getPatch();

                try {
                    ShortMessage instrMsg = new ShortMessage(ShortMessage.PROGRAM_CHANGE,
                            i, patch.getBank(), patch.getProgram());
                    recv.send(instrMsg, 0);
                    System.out.println("instrument "+instrIndex+" on channel "+i);
                } catch (InvalidMidiDataException ex) {
                    Logger.getLogger(MidiTest.class.getName()).log(Level.SEVERE, null, ex);
                }

                //ch[i].programChange(patch.getBank(), patch.getProgram());
            }else System.err.println("instrument index out of range: "+instrIndex);
            i++;
        }
    }
    
    
    public void mimicText(String text)
    {
        text = text.toLowerCase();
        
        //----------------------------------------------------------------------
        // substitutions
        //----------------------------------------------------------------------
        
        //all whitespaces reduced to a single space
        text = text.replaceAll("\\s+", " ");
        
        //replace numbers (TODO: translate to words)
        text = text.replaceAll("\\d", "");
        
        //reduce alphabet to 16 distinct letters
        for(Entry<String, String> substitution: substitutions.entrySet())
            text = text.replace(substitution.getKey(), substitution.getValue());
        
        System.out.println("translated text: \""+text+"\"");
        
        //create the sequence
        try{

            //100 ticks per second -> 1 tick = 10s
            Sequence phrase = new Sequence(Sequence.SMPTE_25, 4, alphabet.size());
            Track[] track = phrase.getTracks();
            
            int consonantLength = 1;
            int vocalLength = 25;
            int whiteSpaceLength = 10;
        
            int pitch = 60;
            int volume = 100;
            
            char[] characters = text.toCharArray();
            int cursorPos=0;
            Integer channel;
            int noteLength;
            for(char c: characters)
            {
                switch(c)
                {
                    case 'a': case 'e': case 'o': case 'u':
                    {
                        //vocal
                        noteLength = vocalLength;
                        break;
                    }
                    case ' ':
                    {
                        //whitespace
                        noteLength = whiteSpaceLength;
                        break;
                    }
                    default:
                    {
                        //treat everything else as consonants
                        noteLength = consonantLength;
                        break;
                    }
                }//switch
                
                //get the proper instrument + channel
                ShortMessage msgStart;
                ShortMessage msgEnd;
                Integer instrument = alphabet.get(c);
                if(instrument != null)
                {
                    //append sound
                    channel = channels.get(instrument);
                    System.out.println("letter \'"+c+"\': instrument "+instrument+" on channel "+channel);
                
                    msgStart = new ShortMessage(ShortMessage.NOTE_ON, channel, pitch, volume);
                    msgEnd = new ShortMessage(ShortMessage.NOTE_OFF, channel, 0, 0);
                    
                    track[channel].add(new MidiEvent(msgStart, cursorPos));
                    cursorPos += noteLength;
                    track[channel].add(new MidiEvent(msgEnd, cursorPos));
                }else
                {
                    //insert pause
                    cursorPos += noteLength;
                    System.out.println("character \'"+c+"\': pause for "+noteLength+" ticks");
                }
            }    
                
            //play the sequence
            sequ.setSequence(phrase);
            sequ.start();
        }
        catch(InvalidMidiDataException imde)
        {
            System.err.println(imde.toString());
        }
    }
    
}
