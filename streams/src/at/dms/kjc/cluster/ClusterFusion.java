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

    // FlatNode (slave) -> FlatNode (node master)
    // Each fused node has a single master node and possibly multiple slave nodes
    private static HashMap nodeMaster = new HashMap();

    // FlatNode (slave) -> FlatNode (local master)
    // each fused node has a local master this way it knows who is driving the execution
    private static HashMap localMaster = new HashMap();

    // a set of all slave nodes
    private static HashSet eliminatedNodes = new HashSet();

    public static void setPartitionMap(HashMap pmap) {
	partitionMap = pmap;
    }

    private static void fuseTo(FlatNode master_node, FlatNode slave_node) {

	System.out.println("ClusterFusion: Fusing "+slave_node.contents.getName()+" to "+master_node.contents.getName());
	localMaster.put(slave_node, master_node);

	if (isEliminated(master_node)) { master_node = (FlatNode)nodeMaster.get(master_node); }	
	assert !isEliminated(slave_node); 
	
	Set inherited_slaves = fusedWith(slave_node);
	Iterator i = inherited_slaves.iterator();
	while (i.hasNext()) {
	    nodeMaster.put(i.next(), master_node);
	}

	eliminatedNodes.add(slave_node);
	nodeMaster.put(slave_node, master_node);
    }


    public static boolean isEliminated(FlatNode node) {

	return eliminatedNodes.contains(node);
    }

    public static Set fusedWith(FlatNode node) {

	HashSet res = new HashSet();
	FlatNode master;

	if (isEliminated(node)) {
	    master = (FlatNode)nodeMaster.get(node);
	    res.add(master);
	} else {
	    master = node;
	}

	Set keys = nodeMaster.keySet();
	Iterator iter = keys.iterator();

	while (iter.hasNext()) {
	    FlatNode elim = (FlatNode)iter.next();
	    if (nodeMaster.get(elim).equals(master) && !elim.equals(node)) {
		res.add(elim);
	    }
	}

	return res;
    }


    public static FlatNode getLocalMaster(FlatNode node) {
    
	if (!localMaster.containsKey(node)) return null;
	return (FlatNode)localMaster.get(node);
    }
    




    public void visitNode(FlatNode node) 
    {
	SIROperator op = node.contents;
	Integer partition = (Integer)partitionMap.get(op);

	if (node.contents instanceof SIRFilter) {

	    // filter


	    /*
	    if (node.edges[0] != null && node.edges[0].contents instanceof SIRJoiner) {
		
		fuseTo(node.edges[0], node);
		
	    }
	    */


	}

	if (node.contents instanceof SIRSplitter) {

	    // splitter

	    SIRSplitter splitter = (SIRSplitter)node.contents;
	    if (splitter.getSumOfWeights() == 0) return;

 	    if (partition == null) {
		
		if (node.incoming[0] != null && node.incoming[0].contents instanceof SIRFilter) {
		    
		    fuseTo(node.incoming[0], node);
		}
	    }
	}

	if (node.contents instanceof SIRJoiner) {

	    // joiner

	    SIRJoiner joiner = (SIRJoiner)node.contents;
	    if (joiner.getSumOfWeights() == 0) return;

 	    if (partition == null) {

		if (node.edges[0] != null && node.edges[0].contents instanceof SIRFilter) {

		    fuseTo(node.edges[0], node);
		}

		if (node.edges[0] != null && node.edges[0].contents instanceof SIRJoiner) {

		    fuseTo(node.edges[0], node);
		}

		if (node.edges[0] != null && node.edges[0].contents instanceof SIRSplitter) {

		    fuseTo(node.edges[0], node);
		}
	    }
	}
    }


    /**
     * Returns partition that <thread> should execute on.
     */
    public static String getPartition(FlatNode node) {

	if (isEliminated(node)) {
	
	    return getPartition((FlatNode)nodeMaster.get(node));
	
	} else {

	    SIROperator op = node.contents;
	
	    if (op instanceof SIRFilter) {
		int partition = ((Integer)partitionMap.get(op)).intValue() + 1;
		return new String(""+partition);
	    }


	    if (op instanceof SIRJoiner) {
		// joiner not fused to anything by cluster fusion

		if (node.edges[0] != null && node.edges[0].contents instanceof SIRFilter) {
		    String part = getPartition(node.edges[0]);
		    return part;
		} 
		    
		// integrate backwards to partition that is communicating
		// most with this one.
		SIRJoiner join = (SIRJoiner)op;
		HashMap map = new HashMap(); // String partition->Integer sum
		int[] weights = join.getWeights();
		for (int i=0; i<weights.length; i++) {
		    String part = getPartition(node.incoming[i]);
		    Integer _oldSum = (Integer)map.get(part);
		    int oldSum = 0;
		    if (_oldSum!=null) {
			oldSum = _oldSum.intValue();
		    }
		    map.put(part.intern(), new Integer(oldSum+weights[i]));
		}
		
		int max = -1;
		String result = null;
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
		    String part = (String)it.next();
		    int sum = ((Integer)map.get(part)).intValue();
		    if (sum>max) {
			max = sum;
			result = part;
		    }
		}
		assert result!=null;
		return result;
	    }


	    if (op instanceof SIRSplitter) {
		// splitter not fused to anything by cluster fusion	    

		if (node.incoming[0]==null) {
		    // if we hit the top (a null splitter), assign to partition 0
		    return "1";
		} 
	    
		// integrate backward if reading from a filter
		if (node.incoming[0] != null && node.incoming[0].contents instanceof SIRFilter) {
		    String part = getPartition(node.incoming[0]);
		    return part;
		}

		// integrate forwards to partition that is communicating
		// most with this one.

		SIRSplitter split = (SIRSplitter)op;
		HashMap map = new HashMap(); // String partition->Integer sum
		int[] weights = split.getWeights();
		for (int i=0; i<weights.length; i++) {
		    String part = getPartition(node.edges[i]);
		    Integer _oldSum = (Integer)map.get(part);
		    int oldSum = 0;
		    if (_oldSum!=null) {
			oldSum = _oldSum.intValue();
		    }
		    map.put(part.intern(), new Integer(oldSum+weights[i]));
		}
		
		int max = -1;
		String result = null;
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
		    String part = (String)it.next();
		    int sum = ((Integer)map.get(part)).intValue();
		    if (sum>max) {
			max = sum;
			result = part;
		    }
		}
		assert result!=null;
		return result;
	    }
	 
	    if (op instanceof SIRIdentity) {
		// if we find identity that wasn't assigned, integrate it
		// into its destination (arbitrarily -- could just as well
		// be the source)
		return getPartition(node.edges[0]);
	    }

	    return null;
	}
    }
}

    
