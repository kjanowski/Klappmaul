package de.kmj.robots.klappmaul;

import de.kmj.robots.klappmaul.blackBoxAPI.Klappmaul;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationService;
import de.kmj.robots.klappmaul.blackBoxAPI.led.LEDService;

//RobotEngine dependencies
import de.kmj.robots.RobotEngine;
import de.kmj.robots.messaging.CommandMessage;
import de.kmj.robots.messaging.StatusMessage;
import de.kmj.robots.util.BasicLogFormatter;

// generic dependecies
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;

/**
 * An engine for a simulated robot ("Klappmaul") which can be used for testing
 * purposes.
 *
 * @author Kathrin Janowski
 */
public class KlappmaulEngine extends RobotEngine {

    private final Logger cLogger;
    private Klappmaul mRobot;
    private SpeechWrapper mSpeechWrapper;
    private AnimationService mAnimService;
    private LEDService mLEDService;

    public KlappmaulEngine() {
        //configure global logging
        setGlobalLogFormatter(new BasicLogFormatter());
        setGlobalLogLevel(Level.FINE);
        cLogger = Logger.getLogger(KlappmaulEngine.class.getName());
        
        cLogger.log(Level.INFO, "created");
    }

    //==========================================================================
    // starting and stopping the engine
    //==========================================================================
    @Override
    public void start(String configPath) {
        loadConfig(configPath);

        //----------------------------------------------------------------------
        // connect to the Klappmaul
        //----------------------------------------------------------------------
        String robotConfig = mEngineConfig.getProperty("robot.config");

        mRobot = new Klappmaul(robotConfig);
        boolean success = mRobot.powerOn();

        if (success) {
            boolean fifoSpeechDebug = Boolean.parseBoolean(mEngineConfig.getProperty("enableFIFOSpeechDebug"));

            mSpeechWrapper = new SpeechWrapper(this, mRobot, fifoSpeechDebug);
            mSpeechWrapper.start();

            mAnimService = mRobot.connectToAnimation();

            mLEDService = mRobot.connectToLEDs();

            try {
                int windowX = Integer.parseInt(mEngineConfig.getProperty("window.x", "0"));
                int windowY = Integer.parseInt(mEngineConfig.getProperty("window.y", "0"));

                mRobot.setWindowPosition(windowX, windowY);
            } catch (NumberFormatException nfe) {
                //ignore
            }

            try {
                double width = Double.parseDouble(mEngineConfig.getProperty("window.width", "270"));
                double height = Double.parseDouble(mEngineConfig.getProperty("window.height", "350"));

                mRobot.setWindowSize(width, height);
            } catch (NumberFormatException nfe) {
                //default size
                mRobot.setWindowSize(270, 350);
            }

            
            try {
                double camPitch = Double.parseDouble(mEngineConfig.getProperty("camera.pitch", "0.0"));
                double camYaw = Double.parseDouble(mEngineConfig.getProperty("camera.yaw", "0.0"));
                double camDistance = Double.parseDouble(mEngineConfig.getProperty("camera.distance", "250.0"));

                cLogger.log(Level.INFO, "setting camera position: pitch = {0}, yaw = {1}, distance = {2}",
                        new Object[]{camPitch, camYaw, camDistance});
                mRobot.setCameraPosition(camPitch, camYaw, camDistance);
            } catch (NumberFormatException nfe) {
                //ignore
            }

            try {
                double camSpeedMove = Double.parseDouble(mEngineConfig.getProperty("camera.speed.move", "10.0"));
                double camSpeedRot = Double.parseDouble(mEngineConfig.getProperty("camera.speed.rotate", "10.0"));

                cLogger.log(Level.INFO, "setting camera speed: movement = {0}, rotation = {1}",
                        new Object[]{camSpeedMove, camSpeedRot});
                mRobot.setCameraSpeed(camSpeedMove, camSpeedRot);
            } catch (NumberFormatException nfe) {
                //ignore
            }
            
            
            cLogger.log(Level.INFO, "connected to Klappmaul");
        } else {
            cLogger.log(Level.SEVERE, "failed to start Klappmaul");
        }
    }

    @Override
    public void stop() {
        //disconnect
        mRobot.powerOff();
    }

    //==========================================================================
    // command execution
    //==========================================================================
    /**
     * Executes a command from an external application.
     *
     * If the command is supported and valid, the appropriate method is called.
     * Otherwise, the command is rejected and a warning is printed to the
     * console.
     *
     * @param cmd the raw command message
     */
    @Override
    public void executeCommand(CommandMessage cmd) {
        String taskID = cmd.getTaskID();

        String type = cmd.getCommandType();
        switch (type) {
            case "speech": {
                String text = cmd.getParam("text");
                if (text == null) {
                    cLogger.log(Level.WARNING, "no \"text\" attribute for speech command");
                    rejectCommand(taskID, "missing parameter: text");
                } else {
                    mSpeechWrapper.say(taskID, text);
                }
                break;
            }
            case "stopSpeech": {
                mSpeechWrapper.stopSpeech(taskID);
                break;
            }
            case "led": {
                String ledID = "Jaw";

                //look for the color
                String color = cmd.getParam("color");

                if (color != null) {
                    setLEDColor(taskID, ledID, color);
                } else {
                    //is it an RGB vector?
                    String red = cmd.getParam("red");
                    String green = cmd.getParam("green");
                    String blue = cmd.getParam("blue");

                    if ((red == null) || (green == null) || (blue == null)) {
                        cLogger.log(Level.WARNING, "found neither color name nor RGB vector for LED");
                        rejectCommand(taskID, "missing parameter(s): either color or red, green and blue");
                    } else {
                        setLEDColor(taskID, ledID, red, green, blue);
                    }
                }
                break;
            }
            case "gaze": {
                
                try {
                    double x = Double.parseDouble(cmd.getParam("x"));
                    double y = Double.parseDouble(cmd.getParam("y"));
                    double z = Double.parseDouble(cmd.getParam("z"));
                    
                    int time = 200;
                    String timeStr = cmd.getParam("time");
                    if(timeStr != null) time= (int)Double.parseDouble(timeStr);

                    mAnimService.gazeAt(x, y, z, time);
                    StatusMessage status = new StatusMessage(taskID, "finished");
                    sendStatusMessage(status);
                } catch (NumberFormatException nfe) {
                    rejectCommand(taskID, "invalid double value(s): "+ nfe.getMessage());
                } catch (NullPointerException ne) {
                    rejectCommand(taskID, "missing value(s): x, y or z");
                }
                break;
            }
            case "move": {
                try {
                    double x = Double.parseDouble(cmd.getParam("x"));
                    double y = Double.parseDouble(cmd.getParam("y"));
                    
                    String angleStr = cmd.getParam("angle");
                    double angle = 0.0;
                    if(angleStr != null)
                        angle = Double.parseDouble(cmd.getParam("angle"));
                    
                    mAnimService.moveTo(x, y, angle);
                    StatusMessage status = new StatusMessage(taskID, "finished");
                    sendStatusMessage(status);
                } catch (NumberFormatException nfe) {
                    rejectCommand(taskID, "invalid double value(s): x, y or z");
                } catch (NullPointerException ne) {
                    rejectCommand(taskID, "missing coordinate(s): x, y or z");
                }
                break;
            }
            case "setVoice": {
                TreeMap <String, String> params = cmd.getCommandParams();
                if (params == null) {
                    cLogger.log(Level.WARNING, "no parameters found for setVoice command");
                    rejectCommand(taskID, "no parameters found");
                } else {
                    mSpeechWrapper.setVoice(taskID, params);
                }
                break;
            }
            default: {
                cLogger.log(Level.WARNING, "unsupported command type \"{0}\"",
                            type);
                rejectCommand(taskID, "unsupported command type: "+type);
            }
        }
    }

    //--------------------------------------------------------------------------
    // command handling
    //--------------------------------------------------------------------------
    private void setLEDColor(String taskID, String ledID, String colorName) {
        colorName = colorName.toLowerCase();
        Color color;
        try{
            color = Color.valueOf(colorName);
        }catch(Exception e)
        {
            color = null;
        }

        if (color != null) {
            mLEDService.setColor(ledID, color);

            StatusMessage status = new StatusMessage(taskID, "finished");
            sendStatusMessage(status);
        } else {
            mLEDService.resetColor(ledID);
            rejectCommand(taskID, "unknown color: " + colorName);
        }

    }

    private void setLEDColor(String taskID, String ledID, String red, String green, String blue) {
        //----------------------------------------------------------------------
        // parse the color vector
        //----------------------------------------------------------------------

        //map from [0.0, 0.1] to [0, 1000]
        int r, g, b;
        try {
            r = (int) (Double.parseDouble(red) * 255);
            g = (int) (Double.parseDouble(green) * 255);
            b = (int) (Double.parseDouble(blue) * 255);
        } catch (NumberFormatException nfe) {
            cLogger.log(Level.SEVERE, "could not parse color vector: {0}",
                    nfe.toString());
            
            rejectCommand(taskID, "invalid double value(s): red, green or blue");
            return;
        }

        //cap ranges
        if (r > 255) {
            cLogger.log(Level.WARNING, "red value out of range");
            r = 255;
        }
        if (g > 255) {
            cLogger.log(Level.WARNING, "green value out of range");
            g = 255;
        }
        if (b > 255) {
            cLogger.log(Level.WARNING, "blue value out of range");
            b = 255;
        }

        //set the LED color
        mLEDService.setColor(ledID, r, g, b);
        StatusMessage status = new StatusMessage(taskID, "finished");
        sendStatusMessage(status);
    }    
    
}
