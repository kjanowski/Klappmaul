/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.animation;

import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationEvent.AnimationEventType;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.logging.Level;
        

/**
 *
 * @author Kathrin
 */
public class AnimationService {
    
    private static final Logger cLogger = Logger.getLogger(AnimationService.class.getName());
    
    /** The set of event listeners. */
    private final HashSet<AnimationEventListener> mListeners;
    
    private final long cSecondsPerMeter = 5000;
    
    private final TreeMap<String, Double> mMinAngles;
    private final TreeMap<String, Double> mMaxAngles;
    private final TreeMap<String, Double> mDefAngles;
    private final TreeMap<String, Double> mCurrAngles;
    
    
    private double mAU25;
    
    
    
    public AnimationService() {
        mAU25 = 0.0;
                
        mMinAngles = new TreeMap<>();
        mMaxAngles = new TreeMap<>();
        mDefAngles = new TreeMap<>();
        mCurrAngles = new TreeMap<>();

        mListeners = new HashSet<>();
        
        initJointAngles();
    }
    
    
    //==========================================================================
    // 
    //==========================================================================
    
    
    
    public final TreeMap<String, Double> getMinAngles()
    {
        return mMinAngles;
    }

    public final TreeMap<String, Double> getMaxAngles()
    {
        return mMaxAngles;
    }
    
    public final TreeMap<String, Double> getDefaultAngles()
    {
        return mDefAngles;
    }

    public final TreeMap<String, Double> getCurrentAngles()
    {
        return mCurrAngles;
    }
    
    
    //==========================================================================
    // 
    //==========================================================================

    private void initJointAngles(){
        
        //access AnimationService data for initialization
        mMinAngles.clear();
        mMaxAngles.clear();
        mDefAngles.clear();
        mCurrAngles.clear();

        //----------------------------------------------------------------------
        
        mMinAngles.put("HeadYaw", -90.0);
        mMaxAngles.put("HeadYaw", 90.0);
        mDefAngles.put("HeadYaw", 0.0);
        mCurrAngles.put("HeadYaw", 0.0);

        mMinAngles.put("HeadPitch", -60.0);
        mMaxAngles.put("HeadPitch", 35.0);
        mDefAngles.put("HeadPitch", 0.0);
        mCurrAngles.put("HeadPitch", 0.0);

        mMinAngles.put("HeadRoll", -45.0);
        mMaxAngles.put("HeadRoll", 45.0);
        mDefAngles.put("HeadRoll", 0.0);
        mCurrAngles.put("HeadRoll", 0.0);
        
        mMinAngles.put("JawPitch", 0.0);
        mMaxAngles.put("JawPitch", 81.0);
        mDefAngles.put("JawPitch", 0.0);
        mCurrAngles.put("JawPitch", 0.0);
    }



    
    public void setDefaultAngles(int duration)
    {
        for(Entry<String, Double> currAngle: mCurrAngles.entrySet())
        {
            String joint = currAngle.getKey();
            double oldAngle = currAngle.getValue();
            double newAngle = mDefAngles.get(joint);
            
            JointMovement movement = new JointMovement(joint, oldAngle, newAngle, duration);
            AnimationEvent animEvt = new AnimationEvent(this, AnimationEventType.CHANGED, new JointMovement[]{movement});
            notifyListeners(animEvt);
        }
    }
    
    /**
     * 
     * @param name the name of the joint to move
     * @param angle the angle in degrees 
     * @param duration the duration of the movement
     */
    public void setJointAngle(String name, double angle, int duration)
    {
        if(Double.isNaN(angle))
        {
            cLogger.log(Level.WARNING,
                        "requested angle for \"{0}\" is NaN -> ignore movement command",
                        name);
            return;
        }
        
        Double oldAngle = mCurrAngles.get(name);

        if(oldAngle == null)
            oldAngle = 0.0;
        
        
        //check joint limits ---------------------------------------------------
        
        Double minAngle = mMinAngles.get(name);
        if(angle < minAngle)
        {
            cLogger.log(Level.WARNING, "requested angle for \"{0}\" below minimum -> cap {1}° to {2}°",
                        new Object[]{name, angle, minAngle});
            angle = minAngle;
        }
        
        Double maxAngle = mMaxAngles.get(name);
        if(angle > maxAngle)
        {
            cLogger.log(Level.WARNING, "requested angle for \"{0}\" above maximum -> cap {1}° to {2}°",
                        new Object[]{name, angle, maxAngle});
            angle = maxAngle;
        }
        
        //execute movement -----------------------------------------------------    
            
        mCurrAngles.replace(name, angle);

        JointMovement movement = new JointMovement(name, oldAngle, angle, duration);
        AnimationEvent animEvt = new AnimationEvent(this, AnimationEventType.CHANGED, new JointMovement[]{movement});
        notifyListeners(animEvt);
    }
    
    
    /**
     * 
     * @param names the names of the joints to move
     * @param angles the angles in degrees 
     * @param duration the duration of the movement
     */
    public void setJointAngles(String[] names, double[] angles, int duration)
    {
        JointMovement[] movements = new JointMovement[names.length];
        
        for(int i=0; i<names.length; i++)
        {    
            Double oldAngle = mCurrAngles.get(names[i]);

            if(oldAngle == null)
                oldAngle = 0.0;

            
            if(Double.isNaN(angles[i]))
            {
                cLogger.log(Level.WARNING,
                            "requested angle for \"{0}\" is NaN -> ignore movement command",
                            names[i]);
                
                movements[i] = new JointMovement(names[i], oldAngle, oldAngle, duration);
            }
            else
            {
                //check joint limits ---------------------------------------------------

                Double minAngle = mMinAngles.get(names[i]);
                if(angles[i] < minAngle)
                {
                    cLogger.log(Level.WARNING, "requested angle for \"{0}\" below minimum -> cap {1}° to {2}°",
                                new Object[]{names[i], angles[i], minAngle});
                    angles[i] = minAngle;
                }

                Double maxAngle = mMaxAngles.get(names[i]);
                if(angles[i] > maxAngle)
                {
                    cLogger.log(Level.WARNING, "requested angle for \"{0}\" above maximum -> cap {1}° to {2}°",
                                new Object[]{names[i], angles[i], maxAngle});
                    angles[i] = maxAngle;
                }

                //execute movement -----------------------------------------------------    

                mCurrAngles.replace(names[i], angles[i]);
                movements[i] = new JointMovement(names[i], oldAngle, angles[i], duration);
            }
        }
        
        AnimationEvent animEvt = new AnimationEvent(this, AnimationEventType.CHANGED, movements);
        notifyListeners(animEvt);
    }
    
    
    
    
    
    
    public void setMouthPos(double mouthPos, int duration)
    {
        mAU25 = mouthPos;
        
        double oldPitch = mCurrAngles.get("JawPitch");
        double minPitch = mMinAngles.get("JawPitch");
        double pitchRange = mMaxAngles.get("JawPitch") - minPitch;
        double newPitch = minPitch + pitchRange * mAU25;
        
        mCurrAngles.replace("JawPitch", newPitch);
        
        JointMovement movement = new JointMovement("JawPitch", oldPitch, newPitch, duration);
        AnimationEvent animEvt = new AnimationEvent(this, AnimationEventType.CHANGED, new JointMovement[]{movement});
        notifyListeners(animEvt);
    }
    
    
    
    public void gazeAt(double x, double y, double z, int time) {
        
        double yaw, pitch, c;
        
        yaw = Math.toDegrees(Math.atan(y/x));
        
        c = Math.sqrt(x*x+y*y);
        pitch = Math.toDegrees(Math.atan(-z/c));
        
        cLogger.log(Level.FINE, "calculated head yaw: {0}°", yaw);
        cLogger.log(Level.FINE, "calculated head pitch: {0}°", pitch);
        setJointAngles(new String[]{"HeadYaw", "HeadPitch"},
                       new double[]{yaw, pitch},
                       time);
    }
    
    
    public void moveTo(double dx, double dy, double angle)
    {
        //limit speed (TODO also limit angle speed)
        double dist = Math.sqrt(dx*dx+dy*dy);
        long time = (long)(cSecondsPerMeter*dist);
            
        Locomotion motion = new Locomotion(dx, dy, angle, time);
        AnimationEvent anim = new AnimationEvent(this, AnimationEventType.CHANGED, motion);
        notifyListeners(anim);
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
