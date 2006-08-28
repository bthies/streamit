package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
//mport at.dms.kjc.*;
//import at.dms.kjc.cluster.*;
import at.dms.kjc.sir.*;
//import at.dms.util.Utils;
//import java.util.Vector;
//import java.util.List;
//import at.dms.compiler.TabbedPrintWriter;
//import at.dms.kjc.raw.Util;
//import at.dms.kjc.sir.lowering.*;
import java.util.*;
//import java.io.*;
//import java.lang.*;

/**
 * the class finds splitters / joiners that need to be fused with filters.
 * <br/>
 * (Not exactly: AD.  allows manual fusing, also has something to do with 
 * creating partition numbers a.k.a. threads.)
 */

public class ClusterFusion implements FlatVisitor {

    /**
     * Maps SIROperator to int.  that's all I know AD.
     * Seems to be used to pass in info about nodes already in partitions
     * to prevent further fusing.
     * 
     * Partitions in this map seem to be numbered from 0, but
     * getPartition adds 1 to the retrieved number.
     */
    private static Map<SIROperator,Integer> partitionMap;

    /** FlatNode (slave) -&gt; FlatNode (node master).
     * 
     * Each fused node has a single master node and possibly multiple slave nodes
     */
    private static Map<FlatNode,FlatNode> nodeMaster = new HashMap<FlatNode,FlatNode>();

    /** FlatNode (slave) -&gt; FlatNode (local master).
     * 
     * each fused node has a local master this way it knows who is driving the execution
     */
    private static Map<FlatNode,FlatNode> localMaster = new HashMap<FlatNode,FlatNode>();

    /** a set of all slave nodes (second parameter's to calls to "fuseTo'). */
    private static Set<FlatNode> eliminatedNodes = new HashSet<FlatNode>();


    /**
     * Clobber 'partitionmap'. 
     * 
     * Seems to be used in {@link ClusterBackend} to record results of cachopts
     * and other partitioners.
     * 
     * @param pmap  a map from SIROperators to partition numbers.
     */
    
    public static void setPartitionMap(HashMap<SIROperator,Integer> pmap) {
        partitionMap = pmap;
    }

    /**
     * Record fusing a "slave" node into a partition represented by a "master" node (or it's master but not transitive).
     *
     * @param master_node
     * @param slave_node
     */
    // slave node is "eliminated"
    // slave node's "loaclMaster" is recorded as passed master.
    // slave node's "nodeMaster" is recorded (as passed master or passed master's master)
    // "NodeMaster" is updated for nodes fusedWith slave node.
    private static void fuseTo(FlatNode master_node, FlatNode slave_node) {

        if (ClusterBackend.debugPrint)
            System.err.println("ClusterFusion: Fusing "+slave_node.contents.getName()+" to "+master_node.contents.getName());
        localMaster.put(slave_node, master_node);

        if (isEliminated(master_node)) { master_node = (FlatNode)nodeMaster.get(master_node); } 
        assert !isEliminated(master_node);  // why not "while" above instead of "if"? so added assert AD.
        assert !isEliminated(slave_node) : "Attempting to fuse already-fused node as slave";
    
        Set<FlatNode> inherited_slaves = fusedWith(slave_node);
        Iterator<FlatNode> i = inherited_slaves.iterator();
        while (i.hasNext()) {
            nodeMaster.put(i.next(), master_node);
        }

        eliminatedNodes.add(slave_node);
        nodeMaster.put(slave_node, master_node);
    }


    /**
     * Has passed node been eliminated in favor of some other representative of fused region?
     * 
     * @param node  a FlatNode
     * @return true if this node has been fused to another node (by being second argument to 'fuseTo')
     */
    public static boolean isEliminated(FlatNode node) {

        return eliminatedNodes.contains(node);
    }

    /**
     * Pass a Flatnode and get back a set of nodes that the passed node is fused with. 
     * 
     * @param node f FlatNode
     * @return Set of all nodes passed node is fused with, not including passed node.A
     */
    public static Set<FlatNode> fusedWith(FlatNode node) {

        HashSet<FlatNode> res = new HashSet<FlatNode>();
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


    /**
     * Get node that the passed node was fused to.
     * 
     * (this is the original result of fusion, the returned node may not
     * be the representative node of a fused section if it has itself been fused
     * into yet another node).
     * 
     * @param node
     * @return a FlatNode or null if the passed node was never fused.
     */
    
    public static FlatNode getLocalMaster(FlatNode node) {
    
        if (!localMaster.containsKey(node)) return null;
        return (FlatNode)localMaster.get(node);
    }
    
    /**
     * Get node that the passed node was fused to.
     * <br/>this should be the transitive closure of @{link #getLocalMaster}
     * @param node
     * @return a FlatNode or null if the passed node was never fused.
     */
    
    public static FlatNode getMaster(FlatNode node) {
            return nodeMaster.get(node);
    }
   

    /**
     *   Cluster partitioning.
     *   
     *   Does nothing if partitionMap already assigns a node to a partition.
     *   Otherwise:
     *    (1) If splitter -- with sumOfWeights != 0 -- is preceeded by filter, fuse it to filter.
     *    (2) If joiner -- with sumOfWeights != 0 -- is followed by filter, splitter or joiner,
     *       fuse it to the following filter splitter or joiner.
     * 
     *   if (KjcOptions.fusion) {
     *       ClusterFusion.setPartitionMap(partitionMap);
     *       graphFlattener.top.accept(new ClusterFusion(), new HashSet(), true);
     *   }
     */

    public void visitNode(FlatNode node) 
    {
        SIROperator op = node.contents;
        Integer partition = (Integer)partitionMap.get(op);

        if (node.contents instanceof SIRFilter) {

            // filter

//            if (node.edges[0] != null && node.edges[0].contents instanceof SIRJoiner) {
//                fuseTo(node.edges[0], node);
//            }
        }

        if (node.contents instanceof SIRSplitter) {

            // splitter

            SIRSplitter splitter = (SIRSplitter)node.contents;
            if (splitter.getSumOfWeights() == 0) 
                return;

            if (partition == null) {
        
                if (node.incoming[0] != null && node.incoming[0].contents instanceof SIRFilter) {
            
                    fuseTo(node.incoming[0], node);
                }
            }
        }

        if (node.contents instanceof SIRJoiner) {

            // joiner

            SIRJoiner joiner = (SIRJoiner)node.contents;
            if (joiner.getSumOfWeights() == 0) 
                return;

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
     * 
     * WTF?  what is this thread referred to?  Line above would indicate that thread
     * is the parameter which is a Flatnode, but threads elsewhere are numbers, and this
     * method returns strings...
     *
     * Algorithm:
     *  If fused with anything else, return partition of its 'nodeMaster' else
     *  If a filter, return partition number for it + 1 as string.
     *  If a joiner followed by a filter, return the filter's partition.
     *    else do something with preceeding operators to get partition... (Janis?)
     *  If a splitter and at top of program, return "1"
     *    else if a splitter preceeded by a filter, return partition of filter.
     *    else do something with following operators to get partition... (Janis?)
     *    
     *    @param node ??
     *    @return ??
     */
    public static String getPartition(FlatNode node) {

        if (isEliminated(node)) {
    
            return getPartition((FlatNode)nodeMaster.get(node));
    
        } else {

            SIROperator op = node.contents;
    
            if (op instanceof SIRFilter) {
                int partition = (partitionMap.get(op)) + 1;
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
                HashMap<String,Integer> map = new HashMap<String,Integer>(); // String partition->Integer sum
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
                HashMap<String,Integer> map = new HashMap<String,Integer>(); // String partition->Integer sum
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

    
