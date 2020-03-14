package de.kmj.robots.klappmaul.blackBoxAPI.led;

import javafx.scene.paint.Color;

/**
 *
 * @author Kathrin Janowski
 */
public class LEDEventData {
    public final String ledID;
    public final Color currentColor;
    
    public LEDEventData(String ledID, Color currentColor)
    {
        this.ledID = ledID;
        this.currentColor = currentColor;
    }
}
