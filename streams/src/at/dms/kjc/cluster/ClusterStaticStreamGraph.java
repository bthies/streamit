package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import java.util.*;
import at.dms.kjc.CType;
import at.dms.kjc.common.CommonUtils;

public class ClusterStaticStreamGraph extends ScheduledStaticStreamGraph {
    /**
     * create a static stream graph with realTop as the first node.
     */
     public ClusterStaticStreamGraph(StreamGraph sg, FlatNode realTop) {
         super(sg,realTop);
     }

     /**
      *  Reset some internals to deal with nodes disappearing
      *  because of fusion.
      */
     public void cleanupForFused() {
         // info in initExecutionCounts, steadyExecutionCounts,
         // and executionCounts should be OK for use since 
         // not about to ask for executions of eliminated node.
         
         // don't eliminate since would have to adjust
         // outputSSGEdges
         // NB: eliminating an input or output would require
         // adjusting inputSSGEdges, outputSSGEdges and their
         // corresponding edges in other graphs.

         LinkedList<FlatNode> newFlatNodes = new LinkedList<FlatNode>();
         Set<FlatNode> unique = new HashSet<FlatNode>();
         for (FlatNode node : flatNodes) {
             if (ClusterFusion.isEliminated(node)) {
                 node = ClusterFusion.getMaster(node);
             }
             if (! unique.contains(node)) {
                 unique.add(node);
                 newFlatNodes.add(node);
             }
         }
         flatNodes = newFlatNodes;
         
         List<FlatNode> newOutputs = new LinkedList<FlatNode>();
         List<CType>newOutputTypes = new LinkedList<CType>();
         for (int i=0; i < outputs.length; i++) {
              if (! ClusterFusion.isEliminated(outputs[i])) {
                  newOutputs.add(outputs[i]);
                  newOutputTypes.add(outputTypes[i]);
              } else {
                  newOutputs.add(ClusterFusion.getMaster(outputs[i]));
                  newOutputTypes.add(CommonUtils.getOutputType(ClusterFusion.getMaster(outputs[i])));}
         }
         outputs = newOutputs.toArray(new FlatNode[]{});
         outputTypes = newOutputTypes.toArray(new CType[]{});
         
         List<FlatNode> newInputs = new LinkedList<FlatNode>();
         for (int i=0; i < inputs.length; i++) {
             if (! ClusterFusion.isEliminated(inputs[i])) {
                 newInputs.add(inputs[i]);
             }
             else {newInputs.add(ClusterFusion.getMaster(inputs[i]));}
        }
         inputs = newInputs.toArray(new FlatNode[]{});
         prevs = toMasters(prevs);
         nexts = toMasters(nexts);
         
         if (ClusterFusion.isEliminated(topLevel)) {
             topLevel = ClusterFusion.getMaster(topLevel);
         }
         setTopLevelSIR(new FlatGraphToSIR(topLevel).getTopLevelSIR());
         // not currently updating graphFlattener.
     }
     
     private HashMap<FlatNode,FlatNode> toMasters(HashMap<FlatNode,FlatNode> in) {
         HashMap<FlatNode,FlatNode> updated = new HashMap<FlatNode,FlatNode>();
             for (Map.Entry<FlatNode,FlatNode> n : in.entrySet()) {
                 FlatNode k = n.getKey();
                 FlatNode v = n.getValue();
                 if (ClusterFusion.isEliminated(k)) {
                     k = ClusterFusion.getMaster(k);
                 }
                 if (ClusterFusion.isEliminated(v)) {
                     v = ClusterFusion.getMaster(v);
                 }
                 updated.put(k,v);
             }
         return updated;
     }
}
