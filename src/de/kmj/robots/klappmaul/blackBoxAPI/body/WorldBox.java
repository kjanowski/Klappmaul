/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.body;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 *
 * @author Kathrin Janowski
 */
public class WorldBox extends Group {

    private Image mNearImage;
    private Image mFarImage;
    private Image mLeftImage;
    private Image mRightImage;
    private Image mTopImage;
    private Image mBottomImage;

    private final ImageView mNearPlaneView;
    private final ImageView mFarPlaneView;
    private final ImageView mRightPlaneView;
    private final ImageView mLeftPlaneView;
    private final ImageView mTopPlaneView;
    private final ImageView mBottomPlaneView;

    
    public WorldBox()
    {
        this("file:res/img/near.png", "file:res/img/far.png",
             "file:res/img/left.png", "file:res/img/right.png",
             "file:res/img/top.png", "file:res/img/bottom.png");
    }
    
    public WorldBox(String nearPlaneURL, String farPlaneURL,
                    String leftPlaneURL, String rightPlaneURL,
                    String topPlaneURL, String bottomPlaneURL) {
        super();

        loadImages(
                nearPlaneURL, farPlaneURL,
                leftPlaneURL, rightPlaneURL,
                topPlaneURL, bottomPlaneURL
        );

        //----------------------------------------------------------------------
        mFarPlaneView = new ImageView(mFarImage);
        mNearPlaneView = new ImageView(mNearImage);
                
        Group3D yzPlaneGroup = new Group3D();
        yzPlaneGroup.getChildren().add(mFarPlaneView);
        yzPlaneGroup.getChildren().add(mNearPlaneView);

        mFarPlaneView.setTranslateX(-250);
        mFarPlaneView.setTranslateY(-250);
        mNearPlaneView.setTranslateX(-250);
        mNearPlaneView.setTranslateY(-250);

        yzPlaneGroup.rotX.setAngle(-90);
        yzPlaneGroup.rotZ.setAngle(90);

        mFarPlaneView.setTranslateZ(250);
        mNearPlaneView.setTranslateZ(-250);

        getChildren().add(yzPlaneGroup);

        //----------------------------------------------------------------------
        mRightPlaneView = new ImageView(mRightImage);
        mLeftPlaneView = new ImageView(mLeftImage);
        
        Group3D xzPlaneGroup = new Group3D();
        xzPlaneGroup.getChildren().add(mRightPlaneView);
        xzPlaneGroup.getChildren().add(mLeftPlaneView);

        mRightPlaneView.setTranslateX(-250);
        mRightPlaneView.setTranslateY(-250);
        mLeftPlaneView.setTranslateX(-250);
        mLeftPlaneView.setTranslateY(-250);

        xzPlaneGroup.rotX.setAngle(-90);

        mRightPlaneView.setTranslateZ(250);
        mLeftPlaneView.setTranslateZ(-250);

        getChildren().add(xzPlaneGroup);

        //------------------------------------------------------------------
        mTopPlaneView = new ImageView(mTopImage);
        mBottomPlaneView = new ImageView(mBottomImage);

        
        Group3D xyPlaneGroup = new Group3D();
        xyPlaneGroup.getChildren().add(mTopPlaneView);
        xyPlaneGroup.getChildren().add(mBottomPlaneView);

        mTopPlaneView.setTranslateX(-250);
        mTopPlaneView.setTranslateY(-250);
        mBottomPlaneView.setTranslateX(-250);
        mBottomPlaneView.setTranslateY(-250);

        xyPlaneGroup.rotX.setAngle(180);

        mTopPlaneView.setTranslateZ(-250);
        mBottomPlaneView.setTranslateZ(250);

        getChildren().add(xyPlaneGroup);
    }
    
    private void loadImages(String nearPlaneURL, String farPlaneURL,
                    String leftPlaneURL, String rightPlaneURL,
                    String topPlaneURL, String bottomPlaneURL)
    {
        Logger logger = Logger.getLogger(WorldBox.class.getName());
        try{
            mNearImage = new Image(nearPlaneURL);
        }
        catch(Exception e)
        {
            logger.log(Level.WARNING, "could not load near plane image: {0}",
                       e.toString());
            mNearImage = new Image("file:fakeurl.png");
        }
        
        try{
            mFarImage = new Image(farPlaneURL);
        }
        catch(Exception e)
        {   
            logger.log(Level.WARNING, "could not load far plane image: {0}",
                       e.toString());
            mFarImage = new Image("file:fakeurl.png");
        }
        
        try{
            mLeftImage = new Image(leftPlaneURL);
        }
        catch(Exception e)
        {   
            logger.log(Level.WARNING, "could not load left plane image: {0}",
                       e.toString());
            mLeftImage = new Image("file:fakeurl.png");
        }
        
        try{
            mRightImage = new Image(rightPlaneURL);
        }
        catch(Exception e)
        {   
            logger.log(Level.WARNING, "could not load right plane image: {0}",
                       e.toString());
            mRightImage = new Image("file:fakeurl.png");
        }
        
        try{
            mTopImage = new Image(topPlaneURL);
        }
        catch(Exception e)
        {   
            logger.log(Level.WARNING, "could not load top plane image: {0}",
                       e.toString());
            mTopImage = new Image("file:fakeurl.png");
        }
        
        try{
            mBottomImage = new Image(bottomPlaneURL);
        }
        catch(Exception e)
        {   
            logger.log(Level.WARNING, "could not load bottom plane image: {0}",
                       e.toString());
            mBottomImage = new Image("file:fakeurl.png");
        }
        
        logger.log(Level.INFO,
            "images loaded:"
            +"\n\tnear:   {0} ({1} x {2})"
            +"\n\tfar:    {3} ({4} x {5})"
            +"\n\tleft:   {6} ({7} x {8})"
            +"\n\tright:  {9} ({10} x {11})"
            +"\n\ttop:    {12} ({13} x {14})"
            +"\n\tbottom: {15} ({16} x {17})",

            new Object[]{
                nearPlaneURL,   mNearImage.getWidth(),   mNearImage.getHeight(),
                farPlaneURL,    mFarImage.getWidth(),    mFarImage.getHeight(),
                leftPlaneURL,   mLeftImage.getWidth(),   mLeftImage.getHeight(),
                rightPlaneURL,  mRightImage.getWidth(),  mRightImage.getHeight(),
                topPlaneURL,    mTopImage.getWidth(),    mTopImage.getHeight(),
                bottomPlaneURL, mBottomImage.getWidth(), mBottomImage.getHeight()
            }
        );
        
        
    }
    
    public void reloadTextures(Properties config)
    {
        String nearPlaneURL = config.getProperty("textures.world.near");
        String farPlaneURL = config.getProperty("textures.world.far");
        String leftPlaneURL = config.getProperty("textures.world.left");
        String rightPlaneURL = config.getProperty("textures.world.right");
        String topPlaneURL = config.getProperty("textures.world.top");
        String bottomPlaneURL = config.getProperty("textures.world.bottom");

        loadImages(nearPlaneURL, farPlaneURL, leftPlaneURL, rightPlaneURL, topPlaneURL, bottomPlaneURL);
        
        mNearPlaneView.setImage(mNearImage);
        mFarPlaneView.imageProperty().set(mFarImage);
        mLeftPlaneView.imageProperty().setValue(mLeftImage);
        mRightPlaneView.setImage(mRightImage);
        mTopPlaneView.setImage(mTopImage);
        mBottomPlaneView.setImage(mBottomImage);
    }
    
    
    public void setScale(double scale)
    {
        setScaleX(scale);
        setScaleY(scale);
        setScaleZ(scale);
    }
}
