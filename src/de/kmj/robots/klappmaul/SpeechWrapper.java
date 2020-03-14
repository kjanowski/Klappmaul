package de.kmj.robots.klappmaul;

// RobotEngine dependencies
import de.kmj.robots.messaging.StatusMessage;
import de.kmj.robots.util.SpeechTask;
import de.kmj.robots.util.FIFOSpeechScheduler;

// Klappmaul dependencies
import de.kmj.robots.klappmaul.blackBoxAPI.Klappmaul;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEngine;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEvent.SpeechEventType;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEventListener;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the speech functionality and related event handling.
 *
 * @author Kathrin Janowski
 */
public class SpeechWrapper extends FIFOSpeechScheduler
        implements SpeechEventListener {

    private final Logger cLogger;

    /**
     * For accessing the Klappmaul's speech.
     */
    private final SpeechEngine mSpeechEngine;
    
    private String mLastKnownTaskID;

    /**
     * Constructor.
     *
     * @param engine the surrounding KlappmaulEngine instance
     * @param pseudoBot for accessing the Klappmaul robot
     * @param fifoDebug true for enabling debug output of the underlying
     * FIFOSpeechScheduler
     */
    public SpeechWrapper(KlappmaulEngine engine, Klappmaul pseudoBot, boolean fifoDebug) {
        super(engine, fifoDebug);

        cLogger = Logger.getLogger(SpeechWrapper.class.getName());

        //connect to the Klappmaul's speech
        mSpeechEngine = pseudoBot.connectToSpeech();
        mSpeechEngine.addListener(this);

        cLogger.log(Level.INFO, "ready");
    }

    public SpeechWrapper(KlappmaulEngine engine, Klappmaul pseudoBot) {
        this(engine, pseudoBot, false);
    }

    //==========================================================================
    // event handling
    //==========================================================================
    /**
     * Handles the Klappmaul-specific speech events.
     *
     * @param event the speech event
     */
    @Override
    public void onSpeechEvent(SpeechEvent event) {
        SpeechEventType type = event.getType();

        if (type == SpeechEventType.BOOKMARK) {
            long bookID = (long) event.getData();
            cLogger.log(Level.INFO, "tts bookmark reached: {0}", bookID);

            //send status update
            String task = getCurrentTask();

            if (task != null) {
                StatusMessage message = new StatusMessage(task, "bookmark");
                message.addDetail("id", Long.toString(bookID));
                mEngine.sendStatusMessage(message);
            } else {
                cLogger.log(Level.WARNING,
                            "no active speech task for bookmark \"{0}\"",
                            bookID);
                StatusMessage message = new StatusMessage(mLastKnownTaskID, "bookmark");
                message.addDetail("id", Long.toString(bookID));
                mEngine.sendStatusMessage(message);
            }

        }else if (type == SpeechEventType.STARTED) {
            cLogger.log(Level.INFO, "started speaking");
            updateSpeechState(true);
        } else if (type == SpeechEventType.STOPPED) {
            cLogger.log(Level.INFO, "finished speaking");
            updateSpeechState(false);
        }
        else if (type == SpeechEventType.FAILED) {
            cLogger.log(Level.INFO, "speech failed");
            updateSpeechState(false);
        }
    }

    //==========================================================================
    // speech functionality
    //==========================================================================
    /**
     * Buffers the next speech task.
     *
     * @param taskID the task ID
     * @param text the TTS command for this task
     */
    public void say(String taskID, String text) {
        bufferTask(new SpeechTask(taskID, text));
    }

    /**
     * Stops the current speech activity.
     *
     * @param taskID the task ID of the stop command
     */
    public void stopSpeech(String taskID) {
        final String task = taskID;

        clearBuffer();
        mSpeechEngine.cancelCurrentSpeech();
        cLogger.log(Level.INFO, "speech canceled");

        StatusMessage message = new StatusMessage(task, "finished");
        mEngine.sendStatusMessage(message);
    }

    /**
     * Sets the voice.
     *
     * @param taskID the task ID of the setVoice command
     * @param params the necessary voice parameters (depending on the engine
     * which is used)
     */
    public void setVoice(String taskID, TreeMap<String, String> params) {
        final String task = taskID;
        StatusMessage message;

        boolean success = mSpeechEngine.setVoice(params);
        if (success) {
            message = new StatusMessage(task, "finished");
        } else {
            message = new StatusMessage(task, "rejected");
        }
        mEngine.sendStatusMessage(message);
    }

    //==========================================================================
    // accessing the robot's TTS
    //==========================================================================
    @Override
    protected void sendToTTS(SpeechTask task) {
        mSpeechEngine.say(task.getTTSCommand());
        mLastKnownTaskID = task.getTaskID();
    }

}
