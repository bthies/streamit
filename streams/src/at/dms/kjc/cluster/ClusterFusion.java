package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.cluster.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.Vector;
import java.util.List;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.raw.Util;
import at.dms.kjc.sir.lowering.*;
import java.util.*;
import java.io.*;
import java.lang.*;

/**
 * the class finds splitters / joiners that need to be fused 
 * with filters
 */

public class ClusterFusion
    extends at.dms.util.Utils implements FlatVisitor {

    private static HashMap partitionMap;

    // FlatNode (elimenated) -> FlatNode (master)
    private static HashMap _fusedWith = new HashMap(); 
    private static HashSet eliminatedNodes = new HashSet();


    public static void setPartitionMap(HashMap pmap) {
	partitionMap = pmap;
    }

    private static void fuseTo(FlatNode master_node, FlatNode eliminated_node) {

	eliminatedNodes.add(eliminated_node);
	_fusedWith.put(eliminated_node, master_node);
    }

    public static boolean isEliminated(FlatNode node) {

	return eliminatedNodes.contains(node);
    }

    public static Set fusedWith(FlatNode node) {

	HashSet res = new HashSet();
	FlatNode master;

	if (isEliminated(node)) {
	    master = (FlatNode)_fusedWith.get(node);
	    res.add(master);
	} else {
	    master = node;
	}

	Set keys = _fusedWith.keySet();
	Iterator iter = keys.iterator();

	while (iter.hasNext()) {
	    FlatNode elim = (FlatNode)iter.next();
	    if (_fusedWith.get(elim).equals(master) && !elim.equals(node)) {
		res.add(elim);
	    }
	}

	return res;
    }

    
    public void visitNode(FlatNode node) 
    {
	Integer partition = (Integer)partitionMap.get(node);

	if (node.contents instanceof SIRFilter) {}

	if (node.contents instanceof SIRSplitter) {

	    SIRSplitter splitter = (SIRSplitter)node.contents;
	    if (splitter.getSumOfWeights() == 0) return;

 	    if (partition == null) {
		System.out.println("Splitter "+splitter.getName()+" has not been assigned a partition!");
		
		if (node.incoming[0] != null && node.incoming[0].contents instanceof SIRFilter) {
		    
		    System.out.println("Splitter "+splitter.getName()+" is reading from a filter!");
		    
		    System.out.println("Will fuse "+splitter.getName()+" with "+node.incoming[0].contents.getName());
		
		    fuseTo(node.incoming[0], node);
		}
	    }
	}

	if (node.contents instanceof SIRJoiner) {

	    SIRJoiner joiner = (SIRJoiner)node.contents;
	    if (joiner.getSumOfWeights() == 0) return;

 	    if (partition == null) {
		System.out.println("Joiner "+joiner.getName()+" has not been assigned a partition!");

		if (node.edges[0] != null && node.edges[0].contents instanceof SIRFilter) {

		    System.out.println("Joiner "+joiner.getName()+" is writing to a filter!");

		    System.out.println("Will fuse "+joiner.getName()+" with "+node.edges[0].contents.getName());
		
		    fuseTo(node.edges[0], node);

		}

	    }
	}
    }
}

    
