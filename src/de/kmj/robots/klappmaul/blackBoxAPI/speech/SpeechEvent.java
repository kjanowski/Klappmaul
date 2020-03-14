/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.speech;

import java.util.EventObject;

/**
 *
 * @author Kathrin Janowski
 */
public class SpeechEvent extends EventObject{

    /**The different event types raised by the Klappmaul speech engine. */
    public enum SpeechEventType {
        /** Started speaking. *//** Started speaking. *//** Started speaking. *//** Started speaking. */
        STARTED,
        /** Next word spoken. */
        WORD_STARTED,
        /** Reached a bookmark. */
        BOOKMARK,
        /** Speaking finished. */
        STOPPED,
        /** Speaking failed. */
        FAILED
    };

    private final SpeechEventType mType;
    private final Object mData;
    
    public SpeechEvent(Object source, SpeechEventType type) {
        super(source);
        mType = type;
        mData = null;
    }
    
    public SpeechEvent(Object source, SpeechEventType type, Object data) {
        super(source);
        mType = type;
        mData = data;
    }
    
    public SpeechEventType getType(){return mType;}
    public Object getData(){return mData;}
}
