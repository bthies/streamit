
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
import java.util.HashMap;
import java.io.*;
import java.lang.*;

import streamit.scheduler2.*;
import streamit.scheduler2.iriter.*;

/**
 *
 */

public class LatencyConstraints {

    public static HashSet restrictedExecutionFilters = new HashSet();
    public static HashMap outgoingLatencyConstraints = new HashMap();

    public static boolean isRestricted(SIRFilter filter) {
	return restrictedExecutionFilters.contains(filter);
    }

    public static HashSet getOutgoingConstraints(SIRFilter filter) {

	if (outgoingLatencyConstraints.containsKey(filter)) {
	    return (HashSet)outgoingLatencyConstraints.get(filter);	    
	} else {
	    HashSet tmp = new HashSet();
	    outgoingLatencyConstraints.put(filter, tmp);
	    return tmp;
	}
    }

    public static void detectConstraints(streamit.scheduler2.iriter.Iterator topStreamIter,
			     SIRPortal portals[]) {

	System.out.println("Number of portals is: "+portals.length);

	for (int t = 0; t < portals.length; t++) {
	    
	    SIRPortalSender senders[] = portals[t].getSenders();
	    SIRStream receivers[] = portals[t].getReceivers();

	    HashSet visited_senders = new HashSet();
	    
	    int min_latency = 0;

	    System.out.println("\n    Portal: "+portals[t]);

	    for (int i = 0; i < senders.length; i++) {
		SIRStream sender = senders[i].getStream();

		if (!visited_senders.contains(sender)) {

		    System.out.print("        sender: ("+sender+")");
		    
		    try {
			int id = NodeEnumerator.getSIROperatorId(sender);
			System.out.print(" ID:"+id);
		    } catch (Exception ex) {
		    }

		    System.out.println();
		
		    visited_senders.add(sender);

		    for (int y = i; y < senders.length; y++) {
			if (senders[y].getStream().equals(sender)) {
			
			    int this_min = 0;

			    SIRLatency latency = senders[y].getLatency();
			
			    if (latency instanceof SIRLatencyMax) {
				this_min = ((SIRLatencyMax)latency).getMax();
			    }

			    if (latency instanceof SIRLatencyRange) {
				this_min = ((SIRLatencyRange)latency).getMin();
			    }

			    System.out.println("          detect Latency: "+
					       this_min);
			    
			    if (this_min < min_latency) min_latency = this_min;
			}
		    } 

		    System.out.println("          minimum Latency: "+min_latency);
		}
	    }

	    for (int i = 0; i < receivers.length; i++) {
		SIRStream receiver = receivers[i];
		
		try {

		    int id = NodeEnumerator.getSIROperatorId(receiver);
		    System.out.println("        receiver: ("+receiver+") ID:"+id);
		} catch (Exception ex) {

		    System.out.println("        receiver: ("+receiver+") ID: NOT FOUND");
		    
		}
	    }

	    System.out.println();

	    Iterator senders_i = visited_senders.iterator();

	    while (senders_i.hasNext()) {
		SIRStream sender = (SIRStream)senders_i.next();

		HashSet constraints = new HashSet();

		for (int i2 = 0; i2 < receivers.length; i2++) {
		    SIRStream receiver = receivers[i2];
		    
		    System.out.println("    sender: "+sender+" receiver: "+receiver);

		    SIRFilter f1 = (SIRFilter)sender;
		    SIRFilter f2 = (SIRFilter)receiver;

		    streamit.scheduler2.constrained.Scheduler cscheduler2 =
			new streamit.scheduler2.constrained.Scheduler(topStreamIter);

		    streamit.scheduler2.iriter.Iterator iter1 = IterFactory.createIter(f1);
		    streamit.scheduler2.iriter.Iterator iter2 = IterFactory.createIter(f2);

		    streamit.scheduler2.SDEPData sdep2;

		    try {
			sdep2 = cscheduler2.computeSDEP(iter1, iter2);

			System.out.println("      Source Init Phases: "+sdep2.getNumSrcInitPhases());
			System.out.println("      Destn. Init Phases: "+sdep2.getNumDstInitPhases());
			System.out.println("      Source Steady Phases: "+sdep2.getNumSrcSteadyPhases());
			System.out.println("      Destn. Steady Phases: "+sdep2.getNumDstSteadyPhases());
			
			int sourceSteady = sdep2.getNumSrcSteadyPhases();
			int destSteady = sdep2.getNumDstSteadyPhases();
	    
			for (int t2 = 0; t2 < 20; t2++) {
			    int phase = sdep2.getSrcPhase4DstPhase(t2);
			    int phaserev = sdep2.getDstPhase4SrcPhase(t2);
			    System.out.println("      sdep ["+t2+"] = "+phase+
					       " reverse_sdep["+t2+"] = "+phaserev);
			}

			if (min_latency < 0) {

			    int last_dep;
			    int iter;

			    // add receiver to set of restricted filters

			    restrictedExecutionFilters.add(receiver);

			    for (iter = 0;; iter++) {
				last_dep = sdep2.getDstPhase4SrcPhase(iter);

				if (last_dep > 1) {

				    break;
				}
			    }
			 
			    LatencyConstraint constraint = 
				new LatencyConstraint(iter-min_latency-1,
						      sourceSteady,
						      destSteady,
						      (SIRFilter)receiver);


			    // add constraint to the senders 
			    // list of constraints

			    constraints.add(constraint);

			    last_dep = 1;

			    int ss = sdep2.getNumSrcSteadyPhases();

			    for (int inc = 0; inc < ss; inc++) {
				int current = sdep2.getDstPhase4SrcPhase(iter + inc);
				if (current > last_dep) {

				    System.out.println("Can exec "+last_dep+"-"+(current-1)+" at source iteration nr. "+(iter + inc + (-min_latency))+" array:"+(current-1));

				    constraint.setDependencyData(inc, 
								 current-1); 

				    last_dep = current;
				} else {
				    
				    System.out.println("Can not advance dest. at source iteration nr. "+(iter + inc + (-min_latency))+" array:-1");

				    constraint.setDependencyData(inc, 
								 0); 
				}
			    }

			    constraint.output();
			}

			System.out.println();			

		    } catch (streamit.scheduler2.constrained.NoPathException ex) {
			
		    }
		}

		outgoingLatencyConstraints.put(sender, constraints);

	    }

	    System.out.println();
	}
    }

}
