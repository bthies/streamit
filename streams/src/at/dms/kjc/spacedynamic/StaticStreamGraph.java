package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;

/**

*/

public class StaticStreamGraph 
{
    private StaticStreamGraph prev, next;
    private SIRStream topLevelSIR;
    private FlatNode topLevel;

    //the number of tiles assigned to this subgraph
    private int tilesAssigned;

    public StaticStreamGraph(StaticStreamGraph prev, 
			     SIRStream top) { 
	this.prev = prev;
	this.topLevelSIR = top;
	topLevel = null;
    }
	    
    //make assertions about construction when we make flat nodes!!!
    
    public void setNumTiles(int i) {
	this.tilesAssigned = i;
    }

    public int getNumTiles() {
	return this.tilesAssigned;
    }

    public void setNext(StaticStreamGraph next) {
	this.next = next;
    }
    
    public StaticStreamGraph getNext() {
	return next;
    }
    
    public StaticStreamGraph getPrev() {
	return prev;
    }
    
    public FlatNode getTopLevel() {
	return topLevel;
    }

    public SIRStream getTopLevelSIR() {
	return topLevelSIR;
    }

    public String toString() {
	if (topLevel != null)
	    return topLevel.toString();
	return topLevelSIR.toString();
    }
    
    public void check() {
	
	
    }
}
