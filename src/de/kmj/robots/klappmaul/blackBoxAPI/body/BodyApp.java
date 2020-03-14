package de.kmj.robots.klappmaul.blackBoxAPI.body;

import de.kmj.robots.klappmaul.blackBoxAPI.led.LEDService;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEngine;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationService;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.MouthAnimThread;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEventListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javax.swing.ImageIcon;

/**
 * A graphical window which displays the Klappmaul behavior.
 *
 * @author Kathrin Janowski
 */
public class BodyApp extends Application implements SpeechEventListener, EventHandler<KeyEvent> {

    private final Logger cLogger;
    
    private static BodyApp sInstance = null;

    private Properties mConfig;
    
    private Stage mMainStage;
    private SubScene mBodyScene;

    private VoicePane mVoicePane;

    private Group3D mCamGroup;
    private PerspectiveCamera mCamera;
    private double mCamDistance;
    private double mCamMovementSpeed;
    private double mCamRotationSpeed;
    private final double cCamMinDistance = 150.0;
    private final double cCamMaxDistance = 350.0;
    
    private MouthAnimThread mMouthAnimThread;

    private LEDService mLEDService;
    private AnimationService mAnimService;

    //the scene ----------------------------------------------------------------
    private WorldBox mWorldBox;
    private BodyModel mKlappmaulBody;
    
    private static final double MIN_WIDTH=100.0;
    private static final double MIN_HEIGHT=100.0;
        
    
    //==========================================================================
    // initialization
    //==========================================================================
    
    public static void main(String[] args) {
        BodyApp.launch(BodyApp.class, args);
    }

    public BodyApp() {
        super();
        mBodyScene = null;
        cLogger = Logger.getLogger(BodyApp.class.getName());
    }

    public void configureTextOutput(Properties config)
    {
        String fontFamily = config.getProperty("text.font.family");
        String fontSize = config.getProperty("text.font.size");
        String fontStyle = config.getProperty("text.font.style", "plain");
        mVoicePane.setFont(fontFamily, fontSize, fontStyle);
    }
    
    public void reloadTextures(Properties config)
    {
        mWorldBox.reloadTextures(config);
        mKlappmaulBody.reloadTextures(config);
    }
    
    public void connect(String name, SpeechEngine speechEngine, LEDService led, AnimationService anim, EventHandler<WindowEvent> closeHandler) {
        speechEngine.addListener(this);
        speechEngine.addListener(mVoicePane);
        mLEDService = led;
        mAnimService = anim;
        anim.addAnimationEventListener(mKlappmaulBody);
        led.addAnimationEventListener(mKlappmaulBody);

        mMouthAnimThread = new MouthAnimThread(anim);
        mMouthAnimThread.setPaused(true);
        mMouthAnimThread.start();
        
        mMainStage.setOnCloseRequest(closeHandler);
    }

    public static BodyApp getInstance() {
        return sInstance;
    }

    /**
     * Fill the main window.
     *
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        sInstance = this;

        List<String> params = getParameters().getUnnamed();
        if(!params.isEmpty())
        {
            File configFile = new File(params.get(0));
            try {
                FileInputStream fis = new FileInputStream(configFile);
                mConfig = new Properties();
                mConfig.load(fis);
            }catch (IOException ex) {
                cLogger.log(Level.SEVERE, "could not load robot config: {0}",
                        ex.toString());
                mConfig = null;
            }
        }
        else mConfig= new Properties();
        
        mMainStage = primaryStage;

        mMainStage.setTitle("Klappmaul");
        
        //icon
        Image icon = new Image(getClass().getResourceAsStream("/icons/Klappmaul_500p.png"));
        mMainStage.getIcons().add(icon);
        
        mVoicePane = new VoicePane();
        mVoicePane.setMinWidth(MIN_WIDTH);
        
        Group3D root3D = new Group3D();
        mBodyScene = create3DScene(root3D, (int)MIN_WIDTH, (int)MIN_HEIGHT);
        
        root3D.translate.setX(0.0);
        root3D.translate.setY(0.0);
        root3D.translate.setZ(-20);

        AnchorPane bodyPane = new AnchorPane(mBodyScene);
        bodyPane.setMinWidth(MIN_WIDTH);
        bodyPane.setMinHeight(MIN_HEIGHT);
        mBodyScene.widthProperty().bind(bodyPane.widthProperty());
        mBodyScene.heightProperty().bind(bodyPane.heightProperty());

        BorderPane pane = new BorderPane();
        pane.setTop(mVoicePane);
        pane.setCenter(bodyPane);
        
        Scene rootScene = new Scene(pane);
        rootScene.setOnKeyPressed(this);

        mMainStage.setScene(rootScene);
        mMainStage.setResizable(true);
        mMainStage.sizeToScene();
        
        mMainStage.initStyle(StageStyle.DECORATED);

        mMainStage.show();
    }

        
    
    public void setWindowPosition(int windowX, int windowY) {
        mMainStage.setX(windowX);
        mMainStage.setY(windowY);
    }

    public void setWindowSize(double width, double height) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //mBodyScene.getParent().resize(width, height);
                //mVoicePane.resize(width, mVoicePane.heightProperty().get());
                //mMainStage.sizeToScene();
                
                mMainStage.setWidth(width);
                mMainStage.setHeight(height);
            }
        });
    }


    public void setCameraPosition(double pitch, double yaw, double distance)
    {
        if(!isReady()) return;
        
        mCamDistance = distance;
        if(mCamDistance > cCamMaxDistance)
            mCamDistance = cCamMaxDistance;
        else if(mCamDistance < cCamMinDistance)
            mCamDistance = cCamMinDistance;
        mCamera.setTranslateZ(-mCamDistance);
        
        mCamGroup.rotZ.setAngle(yaw + 90.0);
        
        //flip sign of pitch to make it more intuitive
        //=> negative pitch rotates downwards, positive pitch rotates upwards
        mCamGroup.rotX.setAngle(-pitch -90.0);
    }
    
    /**
     * 
     * @param moveSpeed distance to move towards/away from the Klappmaul with each step
     * @param rotSpeed angle to rotate the camera around the Klappmaul with each step
     */
    public void setCameraSpeed(double moveSpeed, double rotSpeed)
    {
        mCamMovementSpeed = moveSpeed;
        mCamRotationSpeed = rotSpeed;
    }

    
    public void setName(String name)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mMainStage.setTitle(name);
            }
        });
    }
    
    public void setVoicePaneVisible(boolean visible)
    {
        mVoicePane.visibleProperty().set(visible);
    }

    
    public void setAlwaysOnTop(boolean onTop)
    {
        mMainStage.setAlwaysOnTop(onTop);
    }
    
    public boolean isReady() {
        return (mKlappmaulBody != null);
    }

    private SubScene create3DScene(Group3D root, int width, int height) {
        //root = new Group3D();
        root.setDepthTest(DepthTest.ENABLE);

        SubScene subScene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.LIGHTGRAY);
        
        try{
            mWorldBox = new WorldBox();

            mWorldBox.setScale(1.4);
            //mWorldBox.setOpacity(0.3);            
            mWorldBox.reloadTextures(mConfig);
            
            root.getChildren().add(mWorldBox);
        }
        catch(Exception e)
        {
            Logger.getLogger(BodyApp.class.getName()).
                    log(Level.SEVERE, "could not create axis box: {0}",
                            e.toString());
        }
        
        
        //----------------------------------------------------------------------
        // Camera
        //----------------------------------------------------------------------

        mCamera = new PerspectiveCamera(true);
        mCamera.setNearClip(10.0);
        mCamera.setFarClip(1000.0);
        mCamera.setFieldOfView(60.0);
        
        subScene.setCamera(mCamera);

        mCamGroup = new Group3D();
        mCamGroup.getChildren().add(mCamera);

        mCamGroup.rotX.setAngle(-90.0); //now X is pointing right, Y is pointing away, Z is pointing up
        mCamGroup.rotZ.setAngle(90.0); //now X is pointing to the camera, Y is pointing right, Z is pointing up

        mCamDistance = 250.0;
        mCamera.setTranslateZ(-mCamDistance);

        mCamMovementSpeed = 10.0;
        mCamRotationSpeed = 10.0;
    
        root.getChildren().add(mCamGroup);

        //----------------------------------------------------------------------
        // import the Klappmaul body
        //----------------------------------------------------------------------
        
        mKlappmaulBody = new BodyModel(mConfig);
        
        root.getChildren().add(mKlappmaulBody);

                
        return subScene;
    }
    

    //==========================================================================
    // event handling
    //==========================================================================
    @Override
    public void onSpeechEvent(SpeechEvent event) {
        SpeechEvent.SpeechEventType type = event.getType();
        Logger.getLogger(BodyApp.class.getName()).
              log(Level.FINE, "received speech event: {0}", type.toString());
        if (type == SpeechEvent.SpeechEventType.STARTED) {
            //start speech animation
            mMouthAnimThread.setPaused(false);
        } else if (type == SpeechEvent.SpeechEventType.STOPPED) {
            //stop speech animation
            mMouthAnimThread.setPaused(true);
        }
    }


    @Override
    public void handle(KeyEvent event) {
        KeyCode code = event.getCode();
        Logger.getLogger(BodyApp.class.getName()).
            log(Level.FINE, "received key event with code = {0}",
                event.getCode().toString());

        switch (code) {
            case S: {
                mCamDistance += mCamMovementSpeed;
                if(mCamDistance > cCamMaxDistance)
                    mCamDistance = cCamMaxDistance;
                mCamera.setTranslateZ(-mCamDistance);
                break;
            }
            case W: {
                mCamDistance -= mCamMovementSpeed;
                if(mCamDistance < cCamMinDistance)
                    mCamDistance = cCamMinDistance;
                mCamera.setTranslateZ(-mCamDistance);
                break;
            }
            case A: {
                double oldRotZ = mCamGroup.rotZ.getAngle();
                mCamGroup.rotZ.setAngle(oldRotZ - mCamRotationSpeed);
                break;
            }
            case D: {
                double oldRotZ = mCamGroup.rotZ.getAngle();
                mCamGroup.rotZ.setAngle(oldRotZ + mCamRotationSpeed);
                break;
            }

            case UP: {
                double oldRotX = mCamGroup.rotX.getAngle();
                mCamGroup.rotX.setAngle(oldRotX - mCamRotationSpeed);
                break;
            }
            case DOWN: {
                double oldRotX = mCamGroup.rotX.getAngle();
                mCamGroup.rotX.setAngle(oldRotX + mCamRotationSpeed);
                break;
            }
            
            default: {
                break;
            }
        }

    }

}
