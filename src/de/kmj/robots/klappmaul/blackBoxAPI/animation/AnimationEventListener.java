/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.animation;

import java.util.EventListener;

/**
 *
 * @author Kathrin
 */
public interface AnimationEventListener extends EventListener{
    public void onAnimationEvent(AnimationEvent event);
}
