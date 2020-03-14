package de.kmj.robots.klappmaul.blackBoxAPI.animation;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Periodically triggers opening and closing of the mouth.
 * @author Kathrin
 */
public class MouthAnimThread extends Thread{
    
    private final AnimationService mAnimService;

    private final int mPause = 100; // opening+closing
    private final int mDuration = 200;
        
    
    private boolean mPaused;
    private final ReentrantLock mPausedLock = new ReentrantLock();
    private final Condition mPausedCond = mPausedLock.newCondition();
    
    
    
    
    public MouthAnimThread(AnimationService anim)
    {
        mAnimService = anim;
    }
    
    @Override
    public void run()
    {
        boolean paused = isPaused();
                
        while(!interrupted())
        {
            //if it is, wait until it is resumed
            if(paused)
                waitForResume();

            //open and close
            try{
                mAnimService.setMouthPos(1.0, mDuration);
                sleep(mPause); 
                
                mAnimService.setMouthPos(0.0, mDuration);
                sleep(mPause); 
            }
            catch(InterruptedException ie)
            {
                //System.out.println("[MouthAnimThread] Animation Thread was interrupted");
            }
            
            //check: is thread paused now?
            paused = isPaused();
        }
    }
    
    public void setPaused(boolean paused)
    {
        try{
            mPausedLock.lock();
            
            mPaused = paused;
            mPausedCond.signalAll(); 
        }
        catch(Exception e)
        {
            
        }
        finally
        {
            if(mPausedLock.isHeldByCurrentThread())
                mPausedLock.unlock();
        }
        
        //System.out.println("[MouthAnimThread] paused: "+paused);        
    }
    
    
    public boolean isPaused()
    {
        boolean paused = false;
        
        try{
            mPausedLock.lock();
            
            paused = mPaused;
        }
        catch(Exception e)
        {
            //System.err.println("[MouthAnimThread] Error while checking mPaused: "+e.toString());
        }
        finally
        {
            if(mPausedLock.isHeldByCurrentThread())
                mPausedLock.unlock();
        }
        
        return paused;
    }
    
    public void waitForResume()
    {
        boolean paused = isPaused();
        
        //System.out.println("[MouthAnimThread] waiting for resume");
        
        
        while(paused)
        {
            try{
                //System.out.println("[MouthAnimThread] waiting for signal...");
                mPausedLock.lock();
                mPausedCond.await();
                
                paused = isPaused();
                //System.out.println("paused = "+paused);
            }
            catch(Exception e)
            { 
                //System.out.println("[MouthAnimThread] Error while waiting for resume: "+e.toString()+", "+e.getMessage());
            }
            finally
            {
                if(mPausedLock.isHeldByCurrentThread())
                    mPausedLock.unlock();
            }  
        }
        
        //System.out.println("[MouthAnimThread] animation resumed");
    }
    
}
