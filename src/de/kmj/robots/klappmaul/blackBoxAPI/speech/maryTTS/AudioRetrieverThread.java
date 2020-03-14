/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.speech.maryTTS;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineListener;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;
import org.w3c.dom.Document;

/**
 *
 * @author Kathrin
 */
public class AudioRetrieverThread extends Thread{
    private final Logger cLogger = Logger.getLogger(AudioRetrieverThread.class.getName());
    
    private final MaryInterface mMaryInterface;
    private final String mText;
    private final Document mMaryXML;
    private final int mOutputMode;
    private final LineListener mListener;
    
    private AudioInputStream mAudioStream;
    private AudioPlayer mAudioPlayer;
    
    public AudioRetrieverThread(final MaryInterface audioInterface,
                                final String text, final Document maryXML,
                                final int outputMode,
                                final LineListener listener)
    {
        mMaryInterface = audioInterface;
        mText = text;
        mMaryXML = maryXML;
        mOutputMode = outputMode;
        mListener = listener;
        
        mAudioStream = null;
        mAudioPlayer = null;        
    }
    
    @Override
    public void run(){
        try {
            mAudioStream = mMaryInterface.generateAudio(mMaryXML);
            mAudioPlayer = new AudioPlayer(mAudioStream, null, mListener, mOutputMode);
        } catch (SynthesisException ex) {
            cLogger.log(Level.SEVERE, "could not generate audio output: {0}",
                    ex.toString());
            mAudioStream = null;
            mAudioPlayer = null;
        }
    }
    
    public AudioPlayer getAudioPlayer(){
        return mAudioPlayer;
    }
}
