package de.kmj.robots.klappmaul.blackBoxAPI;

import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationService;
import de.kmj.robots.klappmaul.blackBoxAPI.led.LEDService;
import de.kmj.robots.klappmaul.blackBoxAPI.body.BodyApp;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.MIDI.MIDIVoiceEngine;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.maryTTS.MaryTTSWrapper;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEngine;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.TextOutputEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;

/**
 * Simulates a running robot with services for its functionality.
 *
 * @author Kathrin Janowski
 */
public class Klappmaul extends Thread implements EventHandler<WindowEvent>{

    private final Logger cLogger;
    private String mName;
    private SpeechEngine mSpeechEngine;

    private LEDService mLEDService;
    private AnimationService mAnimService;
    private BodyApp mBody;

    private boolean mPoweredOn;

    private String mConfigPath;
    private Properties mConfig;

    public Klappmaul() {
        super("Klappmaul");

        mPoweredOn = false;
        mConfig = null;

        //init the virtual body
        mLEDService = new LEDService();
        mAnimService = new AnimationService();
        mName = "Klappmaul";

        cLogger = Logger.getLogger(Klappmaul.class.getName() + " " + mName);
    }

    public Klappmaul(String configPath) {
        this();

        mConfigPath = configPath;
        if (configPath != null) {
            File configFile = new File(configPath);
            try {
                FileInputStream fis = new FileInputStream(configFile);
                mConfig = new Properties();
                mConfig.load(fis);
                mName = mConfig.getProperty("name", "Klappmaul");
                //rename Thread
                this.setName(getName() + " "+ mName);
            } catch (IOException ex) {
                cLogger.log(Level.SEVERE, "could not load robot config: {0}",
                        ex.toString());
                mConfig = null;
            }
        }
    }
    
    public void run()
    {
        //start the JavaFX window
        Thread bodyThread = new Thread(){
          @Override
          public void run(){
                BodyApp.launch(BodyApp.class, mConfigPath);
          }
        };
        bodyThread.start();
        
        boolean running = true;
        
        while(running && (mBody == null))
        try{
            cLogger.log(Level.INFO, "waiting until body exists...");
            sleep(1000);
            mBody = BodyApp.getInstance();
            cLogger.log(Level.FINE, "mBody = {0}", mBody);
        }
        catch(InterruptedException ie)
        {
            cLogger.log(Level.FINE, "interrupted while waiting for body");
            return;
        }
        
        if(mBody == null)
            return;
        
        mBody.setName(mName);
            
        boolean bodyReady = false;
        while(running && (!bodyReady))
        try{
            cLogger.log(Level.INFO, "waiting until body is ready...");
            sleep(1000);
            bodyReady = mBody.isReady();
            cLogger.log(Level.FINE, "mBody.isReady = {0}", bodyReady);
        }
        catch(InterruptedException ie)
        {
            cLogger.log(Level.FINE, "interrupted while waiting for body");
            return;
        }
        
        //mBody.reloadTextures(mConfig);
        
 
        if(mConfig != null)
        {            
            //check: is the engine type specified?
            String engineType = mConfig.getProperty("speech.engine", "text").trim().toLowerCase();
            
            switch (engineType) {
                case "midi":
                    try{
                        mSpeechEngine = new MIDIVoiceEngine(mConfig);
                    }
                    catch(Exception e)
                    {
                        cLogger.log(Level.SEVERE,
                                "could not initialize MIDI engine: {0}\n-> using text output instead",
                                e.toString());
                        mSpeechEngine = new TextOutputEngine(mConfig);
                    }   break;
                case "marytts":
                    try{
                        mSpeechEngine = new MaryTTSWrapper(mConfig);
                    }
                    catch(Exception e)
                    {
                        cLogger.log(Level.SEVERE,
                                "could not initialize MaryTTS engine: {0}\n-> using text output instead",
                                e.toString());
                        mSpeechEngine = new TextOutputEngine(mConfig);
                    }   break;
                default:
                    mSpeechEngine = new TextOutputEngine(mConfig);
                    break;
            }
        }
        else
        {
            mSpeechEngine = new TextOutputEngine(mConfig);  
        }  
        
        if(mSpeechEngine instanceof MaryTTSWrapper)
            mBody.setVoicePaneVisible(false);
        else
        {
            mBody.setVoicePaneVisible(true);
            mBody.configureTextOutput(mConfig);
        }

        cLogger.log(Level.FINE,
                    "speech engine: {0}",
                    mSpeechEngine.getClass().getSimpleName());
        

        
        mSpeechEngine.start();
        mBody.connect(mName, mSpeechEngine, mLEDService, mAnimService, this);
    }

    /**
     * Starts the virtual robot.
     * 
     * @return true on success, otherwise false
     */ 
    public boolean powerOn() {
        start();
        try {
            join();
        } catch (InterruptedException ex) {
            cLogger.log(Level.FINE, "interrupted while powering on");
            return false;
        }
        
        mPoweredOn = (mBody != null);
        
        return mPoweredOn;
    }

    /**
     * Shuts the virtual robot down.
     * 
     * @return true on success, otherwise false
     */
    public boolean powerOff() {
        if(!mPoweredOn)
        {
            cLogger.log(Level.WARNING, "already powered off");
            return false;
        }
        
        mPoweredOn = false;

        cLogger.log(Level.INFO, "stopping speech engine...");
        if (mSpeechEngine != null) {
            mSpeechEngine.interrupt();

            try {
                mSpeechEngine.join();
            } catch (InterruptedException ex) {
                cLogger.log(Level.FINE, "interrupted while stopping speech engine");
            }
        }
        
        try {
            mBody.stop();
        } catch (Exception ex) {
            cLogger.log(Level.SEVERE, "failed to stop Body3D: {0}", ex.toString());
        }
        
        Platform.exit();
        
        cLogger.log(Level.INFO, "powered off");
        
        return true;
    }

    //==========================================================================
    // access the "services"
    //==========================================================================
    /**
     * Provides a reference to the Klappmaul's simulated speech service.
     *
     * For comparison: - The NAO would give you an ALTextToSpeechProxy. - The
     * RoboKind R-50 would give you a RemoteSpeechServiceClient.
     *
     * @return the Klappmaul's speech engine
     */
    public SpeechEngine connectToSpeech() {
        if (mPoweredOn) {
            return mSpeechEngine;
        } else {
            cLogger.log(Level.WARNING, "can't access speech service: powered off");
            return null;
        }
    }

    /**
     * Provides a reference to the Klappmaul's simulated animation service.
     *
     * @return the Klappmaul's animation service
     */
    public AnimationService connectToAnimation() {
        if (mPoweredOn) {
            return mAnimService;
        } else {
            cLogger.log(Level.WARNING, "can't access animation service: powered off");
            return null;
        }
    }

    /**
     * Provides access to the simulated LEDs.
     *
     * @return the Klappmaul's LED service
     */
    public LEDService connectToLEDs() {
        if (mPoweredOn) {
            return mLEDService;
        } else {
            cLogger.log(Level.WARNING, "can't access LED service: powered off");
            return null;
        }
    }

    /**
     * Positions the frame which shows the Klappmaul's body.
     *
     * @param windowX horizontal coordinates
     * @param windowY vertical coordinates
     */
    public void setWindowPosition(int windowX, int windowY) {
        if (mBody != null) {
            mBody.setWindowPosition(windowX, windowY);
        }
    }

    /**
     * Resizes the frame which shows the Klappmaul's body.
     * @param width
     * @param height
     */
    public void setWindowSize(double width, double height) {
        if (mBody != null) {
            mBody.setWindowSize(width, height);
        }
    }

    
    /**
     * Positions the camera in the 3D scene which contains the Klappmaul's body
     *
     * @param pitch the vertical rotation angle
     * @param yaw the horizontal rotation angle
     * @param distance the camera distance
     */
    public void setCameraPosition(double pitch, double yaw, double distance) {
        if (mBody != null) {
            mBody.setCameraPosition(pitch, yaw, distance);
        }
    }

    /**
     *
     * @param moveSpeed distance to move towards/away from the Klappmaul with
     * each step
     * @param rotSpeed angle to rotate the camera around the Klappmaul with each
     * step
     */
    public void setCameraSpeed(double moveSpeed, double rotSpeed) {
        if (mBody != null) {
            mBody.setCameraSpeed(moveSpeed, rotSpeed);
        }
    }

    
    public void setVoicePaneVisible(boolean visible)
    {
        if (mBody != null) {
            mBody.setVoicePaneVisible(visible);
        }
    }

    
    public void setAlwaysOnTop(boolean onTop)
    {
        if (mBody != null) {
            mBody.setAlwaysOnTop(onTop);
        }
    }
    
    
    //--------------------------------------------------------------------------
    // event handling
    //--------------------------------------------------------------------------

    @Override
    public void handle(WindowEvent event) {
        cLogger.log(Level.INFO, "body window closed -> power off");
        boolean success = powerOff();
        if(success)
            System.exit(0);
    }
    
    
}
