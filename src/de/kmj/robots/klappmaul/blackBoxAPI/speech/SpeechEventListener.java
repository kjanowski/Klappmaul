package de.kmj.robots.klappmaul.blackBoxAPI.speech;

import java.util.EventListener;

/**
 *
 * @author Kathrin Janowski
 */
public interface SpeechEventListener extends EventListener{
    public void onSpeechEvent(SpeechEvent event);
}
