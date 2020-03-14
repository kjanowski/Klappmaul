package de.kmj.robots.klappmaul.blackBoxAPI.body;

import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEvent;
import de.kmj.robots.klappmaul.blackBoxAPI.speech.SpeechEventListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;

import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


/**
 * Simulates the Klappmaul voice.
 * 
 * @author Kathrin Janowski
 */
public class VoicePane extends BorderPane implements SpeechEventListener{
    private final Text mSpeechText;
    private final Text mBookmarkText;
    private Font mFont;
    
    private final SimpleStringProperty mSpeechProperty;
    private final SimpleStringProperty mBookmarkProperty;
    
    public VoicePane()
    {
        super();
                
        BorderStroke borderStroke = new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT);
        setBorder(new Border(borderStroke));

        // captions ------------------------------------------------------------
        GridPane speechCaptionsPane = new GridPane();
        
        Label speechCaption = new Label("speaking: ");
        Label bookCaption = new Label("bookmark: ");
        speechCaptionsPane.add(speechCaption, 0, 0);
        speechCaptionsPane.add(bookCaption, 0, 1);
        
        setLeft(speechCaptionsPane);
        
        // content -------------------------------------------------------------
        GridPane speechContentPane = new GridPane();
        
        mSpeechProperty = new SimpleStringProperty("");
        mSpeechText = new Text();
        mSpeechText.textProperty().bind(mSpeechProperty);
        mSpeechText.setWrappingWidth(200);
        speechContentPane.add(mSpeechText, 0, 0);
        mFont = mSpeechText.getFont();
        
        mBookmarkProperty = new SimpleStringProperty("");
        mBookmarkText = new Text();
        mBookmarkText.textProperty().bind(mBookmarkProperty);
        mBookmarkText.setFill(Color.RED);
        speechContentPane.add(mBookmarkText, 0, 1);
                
        setCenter(speechContentPane);
        
        setRight(null);
    }
    
    //==========================================================================
    // configuration
    //==========================================================================
    
    
    public void setFont(String family, String size, String style)
    {
        if (family == null)
            family = mFont.getFamily();

        double parsedSize;
        try{
            parsedSize = Double.parseDouble(size);
        }catch(NumberFormatException | NullPointerException ne)
        {
            parsedSize = mFont.getSize();
        }
        
        //-------------------------------------
        
        style = style.toLowerCase();
        
        FontPosture posture;
        if(style.contains("italic"))
            posture = FontPosture.ITALIC;
        else posture = FontPosture.REGULAR;
        
        FontWeight weight;
        if(style.contains("bold"))
            weight = FontWeight.BOLD;
        else weight = FontWeight.NORMAL;
        
        //----------------------------------------------------------------------
        Font f = Font.font(family, weight, posture, parsedSize);
        if(f != null)
        {
            System.out.println("new font: "+f.toString());
            mFont = f;
            mSpeechText.setFont(mFont);        
        }
    }


    //==========================================================================
    // event handling
    //==========================================================================
    
    
    @Override
    public void onSpeechEvent(SpeechEvent event) {
        SpeechEvent.SpeechEventType type = event.getType();
        
        switch (type) {
            case WORD_STARTED:
                String word = (String)event.getData();
                if(word != null)
                {                
                    javafx.application.Platform.runLater(new Runnable(){
                        @Override
                        public void run(){
                            mSpeechProperty.set(word);
                        }
                    });
                }
                else javafx.application.Platform.runLater(new Runnable(){
                        @Override
                        public void run(){
                          mSpeechProperty.set("");
                        }
                    });
                break;
            case BOOKMARK:
                long bookID = (long)event.getData();
                if(bookID >-1)
                    javafx.application.Platform.runLater(new Runnable(){
                        @Override
                        public void run(){
                            mBookmarkProperty.set(Long.toString(bookID));
                        }
                    });
                else javafx.application.Platform.runLater(new Runnable(){
                        @Override
                        public void run(){
                            mBookmarkProperty.set("");
                        }
                    });
                break;
            case STOPPED:
                javafx.application.Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        mSpeechProperty.set("");
                            mBookmarkProperty.set("");
                    }
                });
                break;
            default:
                break;
        }
    }
}
