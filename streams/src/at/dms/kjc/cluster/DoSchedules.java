
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.cluster.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.Vector;
import java.util.List;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.raw.Util;
import at.dms.kjc.sir.lowering.*;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.io.*;
import java.lang.*;

import streamit.scheduler2.*;
import streamit.scheduler2.iriter.*;

/**
 *
 */

public class DoSchedules {

    private streamit.scheduler2.iriter.Iterator firstNode;
    private streamit.scheduler2.iriter.Iterator selfIter;

    public static void findSchedules(streamit.scheduler2.iriter.Iterator selfIter, streamit.scheduler2.iriter.Iterator firstNode, SIRStream stream) {
	DoSchedules tmp = new DoSchedules(selfIter, firstNode);
	tmp.recurseStream(stream);
    }

    DoSchedules(streamit.scheduler2.iriter.Iterator selfIter, streamit.scheduler2.iriter.Iterator firstNode) {
	this.selfIter = selfIter;
	this.firstNode = firstNode;
    }

    public void calcNode(streamit.scheduler2.iriter.Iterator nodeIter, SIROperator str) {
	    
	if (nodeIter.equals(firstNode)) return;
	
	System.out.println("DoSchedule Visiting node: "+str);
	
	try {

	    streamit.scheduler2.constrained.Scheduler scheduler =
		new streamit.scheduler2.constrained.Scheduler(selfIter);

	    streamit.scheduler2.SDEPData sdep = scheduler.computeSDEP(firstNode, nodeIter);
	    System.out.println("  Source Init & Steady Phases: "+sdep.getNumSrcInitPhases()+", "+sdep.getNumSrcSteadyPhases());
	    System.out.println("  Destn. Init & Steady Phases: "+sdep.getNumDstInitPhases()+", "+sdep.getNumDstSteadyPhases());
	    
	} catch (Exception ex) {
	    //System.out.println("!!!Exception: "+ex);
	    ex.printStackTrace();
	}
 
    }

    public void recurseStream(SIROperator str) {

        if (str instanceof SIRFilter) {
	    SIRFilter filter = (SIRFilter)str;

	    streamit.scheduler2.iriter.Iterator lastIter = 
		IterFactory.createIter(filter);	
	    
	    calcNode(lastIter, str);
        }

	/*
        if (str instanceof SIRPhasedFilter) {
	    SIRPhasedFilter filter = (SIRPhasedFilter)str;

	    streamit.scheduler2.iriter.Iterator lastIter = 
		IterFactory.createIter(filter);	
	    
	    calcNode(lastIter, str);
        }
	*/

        if (str instanceof SIRFeedbackLoop)
        {
            SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
	    recurseStream(fl.getSplitter());
	    recurseStream(fl.getJoiner());
            recurseStream(fl.getBody());
            recurseStream(fl.getLoop());
        }
        if (str instanceof SIRPipeline)
        {
            SIRPipeline pl = (SIRPipeline)str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                recurseStream(child);
            }
        }
        if (str instanceof SIRSplitJoin)
        {
            SIRSplitJoin sj = (SIRSplitJoin)str;            
	    recurseStream(sj.getSplitter());
	    recurseStream(sj.getJoiner());
	    Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                recurseStream(child);
            }
	}
    }
}


