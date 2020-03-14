/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.led;

import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationEvent.AnimationEventType;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationEventListener;
import de.kmj.robots.klappmaul.blackBoxAPI.led.LEDEventData;
import javafx.scene.paint.Color;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Contains color parameters for the drawing of certain body parts.
 * @author Kathrin
 */
public class LEDService {
    
    private TreeMap<String, Color> mDefaultColors;
    private TreeMap<String, Color> mCurrentColors;
    /** The set of event listeners. */
    private final HashSet<AnimationEventListener> mListeners;
    
    public LEDService()
    {
        mDefaultColors = new TreeMap<>();
        mCurrentColors = new TreeMap<>();
        
        
        mDefaultColors.put("Jaw", Color.DARKGRAY);
        mCurrentColors.put("Jaw", Color.DARKGRAY);
        
        mListeners = new HashSet<>();
    }
    
    public Color getColor(String id)
    {
        Color c = Color.BLACK;
        
        if(mCurrentColors != null)
            c = mCurrentColors.get(id);
        
        return c;
    }
    
    public void setColor(String id, int r, int g, int b)
    {
        Color color = Color.rgb(r, g, b);
        setColor(id, color);
    }
    
    public void setColor(String id, Color color)
    {
        if(mCurrentColors != null)
        {
            mCurrentColors.replace(id, color);
            LEDEventData data = new LEDEventData(id, color);
            notifyListeners(new AnimationEvent(this, AnimationEventType.CHANGED, data));
        }
    }

    public void resetColor(String id)
    {
        Color defColor = mDefaultColors.get(id);
        mCurrentColors.replace(id, defColor);
        LEDEventData data = new LEDEventData(id, defColor);
        notifyListeners(new AnimationEvent(this, AnimationEventType.CHANGED, data));
    }
    
    //==========================================================================
    // messaging
    //==========================================================================
    
    public void addAnimationEventListener(AnimationEventListener listener)
    {
        mListeners.add(listener);
    }
    
    
    public void notifyListeners(AnimationEvent event)
    {
        if((mListeners== null) || (mListeners.isEmpty()))
            return;
        
        for(AnimationEventListener listener: mListeners)
            {
                listener.onAnimationEvent(event);
            }
    
    }
}
