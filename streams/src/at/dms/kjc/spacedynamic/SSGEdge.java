package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.flatgraph.*;
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

public class SSGEdge 
{
    private StaticStreamGraph fromSSG, toSSG;
    public FlatNode outputNode, inputNode;
    /** the connection numbers, so we can rebuild the
	SSGEdges if the flatgraph changes **/
    private int from, to;

    public SSGEdge(StaticStreamGraph fromSSG, 
		   StaticStreamGraph toSSG,
		   int from, int to) 
    {
	this.fromSSG = fromSSG;
	this.toSSG = toSSG;
	this.from = from;
	this.to = to;
    }

    public int getInputNum() 
    {
	return to;
    }
    public int getOutputNum() 
    {
	return from;
    }
    public StaticStreamGraph getInput() 
    {
	return toSSG;
    }
    public StaticStreamGraph getOutput()
    {
	return fromSSG;
    }
}
