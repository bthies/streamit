
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
 * Reprecents a constraint due to a latency constrained message.
 */

public class LatencyConstraint {

    // int sourceInit

    // for downstream messages this stores how many times source 
    // (upstream node) can execute before it sends the first credit.

    // for upstream messages this stores the initial credit that
    // the destination (upstream node) receives.

    private int sourceInit;

    // steady state execution counts

    private int sourceSteadyExec, destSteadyExec; 
    
    private SIRFilter receiver;
    int dependencyData[];

    LatencyConstraint(int sourceInit, 
		      int sourceSteadyExec,
		      int destSteadyExec,
		      SIRFilter receiver) {
	this.sourceInit = sourceInit;
	this.sourceSteadyExec = sourceSteadyExec;
	this.destSteadyExec = destSteadyExec;
	this.receiver = receiver;
	dependencyData = new int[sourceSteadyExec];
	for (int t = 0; t < sourceSteadyExec; t++) {
	    dependencyData[t] = 0;
	}
    }

    public void setDependencyData(int index, int value) {
	dependencyData[index] = value;
    }

    public int getDependencyData(int index) {
	return dependencyData[index];
    }

    public SIRFilter getReceiver() {
	return receiver;
    }

    public int getSourceInit() {
	return sourceInit;
    }

    public int getSourceSteadyExec() {
	return sourceSteadyExec;
    }

    public int getDestSteadyExec() {
	return destSteadyExec;
    }

    public void output() {
	System.out.println(" init: "+sourceInit+
			   " source: "+sourceSteadyExec+
			   " dest: "+destSteadyExec);
	System.out.print(" data: ");
	for (int t = 0; t < sourceSteadyExec; t++) {
	    System.out.print(dependencyData[t]+" ");
	
	}

	System.out.print(" receiver id: "+NodeEnumerator.getSIROperatorId(receiver));
	System.out.println();
    }
}

