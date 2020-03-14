/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.speech;

/**
 * A simple container for a speech job.
 * @author Kathrin Janowski
 */
public class SpeechJob {
    
    private String mJobID;
    private String mText;
    
    public SpeechJob(String jobID, String text)
    {
        mJobID = jobID;
        mText = text;
    }

    public String getJobID() {
        return mJobID;
    }

    public void setJobID(String jobID) {
        mJobID = jobID;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }
    
    
}
