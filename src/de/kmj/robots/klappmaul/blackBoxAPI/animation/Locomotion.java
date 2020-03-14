/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.animation;

/**
 *
 * @author Kathrin
 */
public class Locomotion {
    
    public final double deltaX;
    public final double deltaY;
    public final double angle;
    public final long duration;
    
    public Locomotion(double dx, double dy, double angle, long time)
    {
        this.deltaX = dx;
        this.deltaY = dy;
        this.angle = angle;
        this.duration = time;
    }
}
