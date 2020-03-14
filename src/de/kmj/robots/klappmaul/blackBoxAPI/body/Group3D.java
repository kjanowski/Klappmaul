/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.body;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;


/**
 * A Group object with 3 rotation axes.
 * 
 * Based on the Xform sample code at <url>http://docs.oracle.com/javafx/8/3d_graphics/overview.htm</url>.
 * @author Kathrin
 */
public class Group3D extends Group{
    
    public Translate translate; 
    public Translate pivot;
    public Translate invPivot;
    
    public Rotate rotX;
    public Rotate rotY;
    public Rotate rotZ;
    
    public Scale scale;
    
    
    public Group3D()
    {
        super();
        
        translate = new Translate(0.0, 0.0);
        pivot = new Translate(0.0, 0.0);
        invPivot = new Translate(0.0, 0.0);
        
        rotX = new Rotate(0.0);
        rotX.setAxis(Rotate.X_AXIS);
        
        rotY = new Rotate(0.0);
        rotY.setAxis(Rotate.Y_AXIS);
        
        rotZ = new Rotate(0.0);
        rotZ.setAxis(Rotate.Z_AXIS);
        
        scale= new Scale(1.0, 1.0, 1.0);
        
        getTransforms().addAll(translate, pivot, rotZ, rotY, rotX, scale, invPivot);
    }
    
    
}
