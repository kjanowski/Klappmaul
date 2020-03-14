package de.kmj.robots.klappmaul.blackBoxAPI.body;

import de.kmj.robots.klappmaul.blackBoxAPI.led.LEDService;
import com.sun.javafx.geom.Point2D;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationEventListener;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.AnimationService;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.JointMovement;
import de.kmj.robots.klappmaul.blackBoxAPI.animation.Locomotion;
import de.kmj.robots.klappmaul.blackBoxAPI.led.LEDEventData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.util.Duration;

/**
 *
 * @author Kathrin
 */
public class BodyModel extends Group3D implements AnimationEventListener, EventHandler<ActionEvent>{
    private static final Logger cLogger = Logger.getLogger(BodyModel.class.getName());
    
    private final ColladaImporter mImporter = new ColladaImporter();
    
    private Group3D mHeadGroup;
    private Group3D mJawGroup;
    private Group3D mShoulderGroup;
    
    private TreeMap<String, Group3D> mJoints;
    
    
    private PhongMaterial mHeadMaterial;
    private PhongMaterial mJawMaterial;
    private PhongMaterial mShoulderMaterial;
    private PhongMaterial mAccessoriesMaterial;
    
    private final HashSet<Timeline> mPendingAnimations;
    
    
    
    public BodyModel(Properties config)
    {
        super();
        mJoints = new TreeMap<>();
        mPendingAnimations = new HashSet<>();
        loadModel(config);
    }
    
    public void loadModel(Properties config){
        
        String modelPath = config.getProperty("model.robot.path", "res/model/Klappmaul.dae");
        
        
        mImporter.loadFile(modelPath);
        getChildren().clear();
        
        //----------------------------------------------------------------------
        // find the Head
        //----------------------------------------------------------------------
                
        mHeadGroup = new Group3D();
        mHeadGroup.setDepthTest(DepthTest.ENABLE);

        MeshView headNode = mImporter.getMesh("Head");
        MeshView rightEyeNode = mImporter.getMesh("RightEye");
        MeshView leftEyeNode = mImporter.getMesh("LeftEye");

        ObservableList<Node> headChildren = mHeadGroup.getChildren();
        
        if(headNode!= null)
        {
            mHeadGroup.translate.setX(headNode.getTranslateX());
            mHeadGroup.translate.setY(headNode.getTranslateY());
            mHeadGroup.translate.setZ(headNode.getTranslateZ());

            mHeadMaterial = (PhongMaterial)(headNode.getMaterial());

            if(mHeadMaterial != null)
                mHeadMaterial.setDiffuseColor(Color.LIGHTGRAY);
            else{ mHeadMaterial = new PhongMaterial(Color.LIGHTGRAY);
                ((MeshView)headNode).setMaterial(mHeadMaterial);
            }
            headChildren.add(headNode);
        }
        else cLogger.log(Level.SEVERE, "could not find \"Head\" mesh");
        
        if(rightEyeNode!= null)
            headChildren.add(rightEyeNode);
        else cLogger.log(Level.SEVERE, "could not find \"RightEye\" mesh");
        
        if(leftEyeNode!= null)
            headChildren.add(leftEyeNode);
        else cLogger.log(Level.SEVERE, "could not find \"LeftEye\" mesh");
        
        mJoints.put("Head", mHeadGroup);
        
        
        //----------------------------------------------------------------------
        // find the accessories, if they exist
        //----------------------------------------------------------------------
                
        MeshView accessoriesNode = mImporter.getMesh("Accessories");
        if(accessoriesNode!= null)
        {
            mAccessoriesMaterial = (PhongMaterial)(accessoriesNode.getMaterial());

            if(mAccessoriesMaterial != null)
                mAccessoriesMaterial.setDiffuseColor(Color.LIGHTGRAY);
            else{ mAccessoriesMaterial = new PhongMaterial(Color.LIGHTGRAY);
                ((MeshView)accessoriesNode).setMaterial(mAccessoriesMaterial);
            }
            mHeadGroup.getChildren().add(accessoriesNode);
        }
        else cLogger.log(Level.WARNING, "could not find \"Accessories\" mesh");
        
        //----------------------------------------------------------------------
        // find the Jaw
        //----------------------------------------------------------------------
        
        mJawGroup = new Group3D();
        mJawGroup.setDepthTest(DepthTest.ENABLE);
        
        MeshView jawNode = mImporter.getMesh("Jaw");
        
        if(jawNode != null)
        {
            mJawGroup.translate.setX(jawNode.getTranslateX());
            mJawGroup.translate.setY(jawNode.getTranslateY());
            mJawGroup.translate.setZ(jawNode.getTranslateZ());

            mJawGroup.getChildren().add(jawNode);
        
            
            mJawMaterial = (PhongMaterial)(jawNode.getMaterial());
            
            if(mJawMaterial != null)
                mJawMaterial.setDiffuseColor(Color.GRAY);
            else{
                mJawMaterial = new PhongMaterial(Color.GRAY);
                ((MeshView)jawNode).setMaterial(mJawMaterial);
            }
            
        }else
        {
            cLogger.log(Level.SEVERE, "could not find \"Jaw\" mesh");
            mJawMaterial = null;
        }

        headChildren.add(mJawGroup);

        mJoints.put("Jaw", mJawGroup);

        //----------------------------------------------------------------------
        // find the shoulders, if they exist
        //----------------------------------------------------------------------
                
        mShoulderGroup = new Group3D();
        mShoulderGroup.setDepthTest(DepthTest.ENABLE);

        MeshView shoulderNode = mImporter.getMesh("Shoulders");
        if(shoulderNode!= null)
        {
            mShoulderGroup.translate.setX(shoulderNode.getTranslateX());
            mShoulderGroup.translate.setY(shoulderNode.getTranslateY());
            mShoulderGroup.translate.setZ(shoulderNode.getTranslateZ());

            mShoulderMaterial = (PhongMaterial)(shoulderNode.getMaterial());

            if(mShoulderMaterial != null)
                mShoulderMaterial.setDiffuseColor(Color.LIGHTGRAY);
            else{ mShoulderMaterial = new PhongMaterial(Color.LIGHTGRAY);
                ((MeshView)shoulderNode).setMaterial(mShoulderMaterial);
            }
            mShoulderGroup.getChildren().add(shoulderNode);

            mJoints.put("Shoulders", mShoulderGroup);
        }
        else cLogger.log(Level.WARNING, "could not find \"Shoulders\" mesh");
                

        
        //----------------------------------------------------------------------
        
        reloadTextures(config);
        getChildren().add(mHeadGroup);
        if(mShoulderGroup != null)
            getChildren().add(mShoulderGroup);
    }

    
    public void reloadTextures(Properties config) {
        cLogger.log(Level.INFO, "reloading body textures...");

        reloadTextures(mHeadMaterial, "head", config);
        reloadTextures(mJawMaterial, "jaw", config);
        reloadTextures(mShoulderMaterial, "shoulders", config);
        reloadTextures(mAccessoriesMaterial, "accessories", config);
    }
    
    protected void reloadTextures(PhongMaterial material, String meshID, Properties config)
    {
        if(material == null)
            return;
        
        String diffuseTextureURL = config.getProperty("model.robot."+meshID+".texture.diffuse");
        
        Color baseColor;
        Image diffuseTexture;
        try{
            diffuseTexture = new Image(diffuseTextureURL);
            baseColor=Color.WHITE;
        }
        catch(Exception e)
        {   
            cLogger.log(Level.WARNING, "could not load texture: {0}", e.toString());
            diffuseTexture = null;
            baseColor=Color.GRAY;
        }
        
        material.setDiffuseColor(baseColor);
        material.setDiffuseMap(diffuseTexture);
        
        //is there a gloss map?
        String glossTextureURL = config.getProperty("model.robot."+meshID+".texture.specular");
        
        Image glossTexture;
        try{
            glossTexture = new Image(glossTextureURL);
        }
        catch(Exception e)
        {
            cLogger.log(Level.WARNING, "could not load texture: {0}", e.toString());
            glossTexture = null;
        }
        
        if(glossTexture != null){
            material.setSpecularMap(glossTexture);
        }

        
        //is there a bump map?
        String bumpTextureURL = config.getProperty("model.robot."+meshID+".texture.normals");
        
        Image bumpTexture;
        try{
            bumpTexture = new Image(bumpTextureURL);
        }
        catch(Exception e)
        {
            cLogger.log(Level.WARNING, "could not load texture: {0}", e.toString());
            bumpTexture = null;
        }
        
        if(bumpTexture != null)
            material.setBumpMap(bumpTexture);    
    }
    
    @Override
    public void onAnimationEvent(AnimationEvent event) {
        AnimationEvent.AnimationEventType type = event.getType();
        Object source = event.getSource();
        
        cLogger.log(Level.FINE, "received anim event: source = {0}, type = {1}, data = {2}",
                    new Object[]{source.toString(), type.toString(), event.getData()});

        if (type == AnimationEvent.AnimationEventType.CHANGED) {
            if (source instanceof AnimationService) {
                Object data = event.getData();
                
                if(data instanceof JointMovement[])
                    moveJoints((JointMovement[])data);
                else if(data instanceof Locomotion)
                    moveTo((Locomotion)data);
                else cLogger.log(Level.WARNING, "unknown animation type");
            } else if (source instanceof LEDService) {
                //color the jaw
                LEDEventData data = (LEDEventData)event.getData();
                if(data.ledID.equals("Jaw"))
                {
                    mJawMaterial.setDiffuseColor(data.currentColor);
                }
                else cLogger.log(Level.WARNING, "no LED named \"{0}\"", data.ledID);
            }
        }
    }


    public void moveJoints(JointMovement[] movements)
    {
        //create the animation
        Timeline anim = new Timeline();
        
        for(JointMovement movement: movements)
        {
            KeyValue startValue = null;
            KeyValue endValue = null;
            switch(movement.joint)
            {
                case "JawPitch":
                {
                    startValue = new KeyValue(mJawGroup.rotY.angleProperty(), movement.oldAngle);
                    endValue = new KeyValue(mJawGroup.rotY.angleProperty(), movement.newAngle);
                    break;
                }
                case "HeadYaw":
                {
                    startValue = new KeyValue(mHeadGroup.rotZ.angleProperty(), movement.oldAngle);
                    endValue = new KeyValue(mHeadGroup.rotZ.angleProperty(), movement.newAngle);
                    break;
                }
                case "HeadPitch":
                {
                    startValue = new KeyValue(mHeadGroup.rotY.angleProperty(), movement.oldAngle);
                    endValue = new KeyValue(mHeadGroup.rotY.angleProperty(), movement.newAngle);
                    break;
                }
                case "HeadRoll":
                {
                    startValue = new KeyValue(mHeadGroup.rotX.angleProperty(), movement.oldAngle);
                    endValue = new KeyValue(mHeadGroup.rotX.angleProperty(), movement.newAngle);
                    break;
                }
                default:
                {
                    cLogger.log(Level.WARNING, "there is no joint named {0}",
                            movement.joint);
                }
            }

            if((startValue != null)&&(endValue!=null))
                anim.getKeyFrames().addAll(new KeyFrame(Duration.ZERO, startValue),
                                           new KeyFrame(Duration.millis(movement.duration), endValue));
            
        }
        
        if(anim.getKeyFrames().isEmpty())
            cLogger.log(Level.WARNING, "animation has no keyframes");
        else{
            bufferAnim(anim);
            waitForAnim(anim);
        }
        
    }
    
    
    public void moveTo(Locomotion motion)
    {
        //scale movement to match model
        double dx=motion.deltaX*200;
        double dy=motion.deltaY*200;
                
        //rotate movement to match the local orientation
        double currAngle = this.rotZ.getAngle();
        double angleRad = currAngle*Math.PI/180.0;
        double deltaX = Math.cos(angleRad)*dx - Math.sin(angleRad)*dy;
        double deltaY = Math.sin(angleRad)*dx + Math.cos(angleRad)*dy;
        
  
        Timeline anim = new Timeline();
        KeyValue startAngle, endAngle, startX, endX, startY, endY;
     
        startAngle = new KeyValue(this.rotZ.angleProperty(), currAngle);
        endAngle = new KeyValue(this.rotZ.angleProperty(), currAngle+motion.angle);
        startX = new KeyValue(this.translate.xProperty(), this.translate.getX());
        endX = new KeyValue(this.translate.xProperty(), this.translate.getX()+deltaX);
        startY = new KeyValue(this.translate.yProperty(), this.translate.getY());
        endY = new KeyValue(this.translate.yProperty(), this.translate.getY()+deltaY);
        
        anim.getKeyFrames().addAll(
                                new KeyFrame(Duration.ZERO, startAngle),
                                new KeyFrame(Duration.ZERO, startX),
                                new KeyFrame(Duration.ZERO, startY),
                                new KeyFrame(Duration.millis(motion.duration), endAngle),
                                new KeyFrame(Duration.millis(motion.duration), endX),
                                new KeyFrame(Duration.millis(motion.duration), endY)
        );
        
        
        bufferAnim(anim);            
        waitForAnim(anim);
    }

    public void bufferAnim(Timeline anim)
    {
        anim.setOnFinished(this);
        final Timeline pendingAnim = anim;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                pendingAnim.play();
            }
        });

        synchronized(mPendingAnimations){
            mPendingAnimations.add(anim);
        }
    }
    
    public void waitForAnim(Timeline anim)
    {
        synchronized(mPendingAnimations){
            try {
                while(mPendingAnimations.contains(anim))
                    mPendingAnimations.wait();
            } catch (InterruptedException ex) {
            }
        }
    }
    
    
    @Override
    public void handle(ActionEvent event) {
        if(event.getSource() instanceof Timeline)
        {
            Timeline source = (Timeline)event.getSource();
            synchronized(mPendingAnimations)
            {
                mPendingAnimations.remove(source);
                mPendingAnimations.notifyAll();
            }
        }
    }
}
