/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.animation;

import java.util.EventObject;

/**
 *
 * @author Kathrin
 */
public class AnimationEvent extends EventObject{
    
    /**The different event types raised by the Klappmaul speech engine. */
    public enum AnimationEventType {
        /** Started moving. */
        STARTED,
        /** Position changed. Event data contains the JointMovement which needs to be displayed.*/
        CHANGED,
        /** Movement finished. */
        STOPPED
    };

    private final AnimationEventType mType;
    private final Object mData;
    
    public AnimationEvent(Object source, AnimationEventType type) {
        super(source);
        mType = type;
        mData = null;
    }
    
    public AnimationEvent(Object source, AnimationEventType type, Object data) {
        super(source);
        mType = type;
        mData = data;
    }
    
    public AnimationEventType getType(){return mType;}
    public Object getData(){return mData;}
}
