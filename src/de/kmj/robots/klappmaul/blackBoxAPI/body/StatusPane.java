package de.kmj.robots.klappmaul.blackBoxAPI.body;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 *
 * @author Kathrin Janowski
 */
public class StatusPane extends FlowPane{
    
    private final Text mStatusText;
    private final SimpleStringProperty mStatusProperty;
    
    public enum StatusLevel {
        NORMAL,
        WARNING,
        ERROR
    };
    
    public StatusPane()
    {
        super(Orientation.HORIZONTAL);
        
        BorderStroke borderStroke = new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT);
        setBorder(new Border(borderStroke));
        
        
        Label statusLabel = new Label("status: ");
        getChildren().add(statusLabel);
        
        mStatusProperty = new SimpleStringProperty();
        mStatusText = new Text();
        mStatusText.setWrappingWidth(200);
        mStatusText.textProperty().bind(mStatusProperty);
        mStatusProperty.set("");
        
        getChildren().add(mStatusText);
    }
    
    public void setStatus(String text)
    {
        if(text!=null)
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    mStatusProperty.set(text);
                }
            });
    }
    
    public void setStatus(String text, StatusLevel level)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mStatusProperty.set(text);
                switch(level)
                {
                    case NORMAL: mStatusText.setFill(Color.BLACK); break;
                    case WARNING: mStatusText.setFill(Color.GOLD); break;
                    case ERROR: mStatusText.setFill(Color.RED); break;
                    default: mStatusText.setFill(Color.BLACK); break;
                }
            }
        });
    }
}
