/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.animation;

/**
 *
 * @author Kathrin Janowski
 */
public class JointMovement {
    
    public final String joint;
    public final double oldAngle;
    public final double newAngle;
    public final int duration;

    public JointMovement(String joint, double oldAngle, double newAngle, int duration)
    {
        this.joint = joint;
        this.oldAngle = oldAngle;
        this.newAngle = newAngle;
        this.duration = duration;
    }
    
    @Override
    public String toString()
    {
        return "[joint: "+joint+", oldAngle: "+oldAngle+", newAngle: "+newAngle+", duration: "+duration+"]";
    }
    
}
