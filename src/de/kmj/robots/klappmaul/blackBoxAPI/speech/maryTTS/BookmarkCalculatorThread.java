/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.speech.maryTTS;

import java.io.StringWriter;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import org.w3c.dom.Document;

/**
 *
 * @author Kathrin
 */
public class BookmarkCalculatorThread extends Thread{
    private final Logger cLogger = Logger.getLogger(BookmarkCalculatorThread.class.getName());
    
    private final MaryInterface mMaryInterface;
    private final String mText;
    private final Document mMaryXML;
    private final TreeMap<Integer, Long> mBookmarkMap;
    private final String mBookmarkRegex;
    
    public BookmarkCalculatorThread(final MaryInterface acoustParamsInterface,
                                    final String rawText, final Document maryXML,
                                    final TreeMap<Integer, Long> bookmarkMap,
                                    final String bookmarkPattern)
    {
        mMaryInterface = acoustParamsInterface;
        mText = rawText;
        mMaryXML = maryXML;
        mBookmarkMap = bookmarkMap;
        mBookmarkRegex = bookmarkPattern;
    }
    
    @Override
    public void run()
    {
        Pattern markerPattern = Pattern.compile("((\\w+)\\s)?"+mBookmarkRegex);
        Matcher markerMatch = markerPattern.matcher(mText.replaceAll("'", "")); //remove ' in contractions - they're not included in the acoustparams!
        
        if(markerMatch != null)
        {
            try{
                //get the marker timings
                Document acoustparams;
                acoustparams = mMaryInterface.generateXML(mMaryXML);
                
                //convert XML to String in order to search it
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                StreamResult result = new StreamResult(new StringWriter());
                DOMSource source = new DOMSource(acoustparams);
                transformer.transform(source, result);
                
                String output = result.getWriter().toString();
                
                output = output.replaceAll("\\s", "");
                cLogger.log(Level.INFO, "acoustparams:\n{0}", output);

                int start=0;
                while((start<mText.length()) && markerMatch.find(start))
                {
                    //get the word preceding the current marker
                    String predecessor;
                    if (markerMatch.group(1)!= null)
                        predecessor = markerMatch.group(2);
                    else predecessor = null;

                    //get the marker id
                    String bookID = markerMatch.group(3);

                    //get the marker's timing
                    if(predecessor==null)
                    {
                        cLogger.log(Level.INFO, "found bookmark {0} at the very beginning", bookID);

                        try{
                            long id=Long.parseLong(bookID);
                            mBookmarkMap.put(0, id);
                        }catch(NumberFormatException nfe)
                        {
                            cLogger.log(Level.WARNING, "could not parse bookmark ID {0}"+bookID);
                            //ignore this bookmark
                        }
                    }
                    else
                    {
                        Pattern timePattern = Pattern.compile("<t[^>]+>"+predecessor+"(<syllable[^>]+>(<phd=\\W[.0-9]+\\Wend=\\W(\\d+\\.\\d+)\\W[^>]+>)+</syllable>)+</t>");
                        Matcher timeMatch = timePattern.matcher(output);
                        
                        if(timeMatch.find())
                        {
                            String timeStr = timeMatch.group(3);
                            
                            cLogger.log(Level.INFO, "found bookmark {0} at time {1}", new Object[]{bookID, timeStr});
                            
                            int time = (int)(Double.parseDouble(timeStr)*1000);
                            
                            try{
                                long id=Long.parseLong(bookID);
                                mBookmarkMap.put(time, id);
                            }catch(NumberFormatException nfe)
                            {
                                cLogger.log(Level.WARNING, "could not parse bookmark ID {0}"+bookID);
                                //ignore this bookmark
                            }
                        }
                        else
                           cLogger.log(Level.WARNING, "could not find predecessor {0} in acoustparams output", predecessor);
                                                
                    }
                    start = markerMatch.end();
                }
            }
            catch(SynthesisException e)
            {
                cLogger.log(Level.SEVERE, e.toString());
            } catch (TransformerException ex) {
                cLogger.log(Level.SEVERE, "could not convert ACOUSTPARAMS output to text: {0}", ex.toString());
            }
        }
        else cLogger.log(Level.INFO, "no bookmarks found in TTS command");
    }
    
    
}
