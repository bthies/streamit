package streamit.scheduler2.minlatency;

import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;

public class Scheduler extends streamit.scheduler2.Scheduler
{
    final StreamInterface rootStream;
    StreamFactory factory = new StreamFactory ();
     
    public Scheduler(Iterator _root)
    {
        super (_root);
        
        rootStream = factory.newFrom(root, null); 
    }
    
    public void computeSchedule()
    {
        if (steadySchedule != null) return;
        
        rootStream.computeSchedule();
        
        initSchedule = rootStream.getInitSchedule();
        steadySchedule = rootStream.getSteadySchedule();
    }
}


