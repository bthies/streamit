/**
 * 
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;
import at.dms.kjc.flatgraph.FlatNode;

/**
 * @author dimock
 *
 */
public class TapeClusterFused extends TapeCluster implements Tape {

    /** true if src is local master to dst*/
    final private boolean src_masters_dst;
    /** true if dst is local master to src */
    final private boolean dst_masters_src;
    
    TapeClusterFused(int source, int dest, CType type) {
        super(source,dest,type);
        FlatNode src_node = NodeEnumerator.getFlatNode(source);
        FlatNode src_master = ClusterFusion.getLocalMaster(src_node);
        FlatNode dst_node = NodeEnumerator.getFlatNode(dest);
        FlatNode dst_master = ClusterFusion.getLocalMaster(dst_node);
        src_masters_dst = dst_master != null && dst_master.equals(src_master);
        dst_masters_src = src_master != null && src_master.equals(dst_master);
    }

    /**
     * Consumer name for init code in ClusterCodeGeneration.
     * Do not use elsewhere.
     * Should not be needed for fused.
     */
    @Override
    public String getConsumerName() {
        assert false: "Should not need getProducerName for cluster-fused edge";
        return null;
    }
    
    /**
     * Consumer name for init code in ClusterCodeGeneration.
     * Do not use elsewhere.
     * Should not be needed for fused.
     */
    @Override
    public String getProducerName() {
        assert false: "Should not need getConsumerName for cluster-fused edge";
        return null;
    }

    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#dataDeclarationH()
     */
    @Override
    public String dataDeclarationH() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#dataDeclaration()
     */
    @Override
    public String dataDeclaration() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#downstreamDeclarationExtern()
     */

    /* (non-Javadoc)
     * downstreamDeclaration identical to TapeCluster.downstreamDeclaration
     * @see at.dms.kjc.cluster.TapeBase#downstreamDeclaration()
     */

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#upstreamDeclarationExtern()
     */
    @Override

    /* (non-Javadoc)
     * upstreamDeclaration identical to TapeCluster.upstreamDeclaration
     * @see at.dms.kjc.cluster.TapeBase#upstreamDeclaration()
     */

    protected void createPushRoutineBody(StringBuffer s, String dataName) {
        if (src_masters_dst) {
            super.createPushRoutineBody(s,dataName);
            return;
        }
        s.append(pop_buffer + "[" + pop_index + "++] = " 
                + dataName + ";\n");
    }
   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#topOfWorkIteration(at.dms.kjc.common.CodegenPrintWriter)
     */
    public String topOfWorkIteration() {
        return "";
    }
    

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#upstreamCleanup()
     */
    @Override
    public String upstreamCleanup() {
        return "";
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#downstreamCleanup()
     */
    @Override
    public String downstreamCleanup() {
        return "";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#pushPrefix()
     */
    @Override
    public String pushPrefix() {
        // TODO Auto-generated method stub
        return push_name+"(";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#pushSuffix()
     */
    @Override
    public String pushSuffix() {
        // TODO Auto-generated method stub
        return ")";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#popExpr()
     */
    @Override
    public String popExpr() {
        // TODO Auto-generated method stub
        return pop_name+"()";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#popNStmt(int)
     */
    @Override
    public String popNStmt(int N) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#peekPrefix()
     */
    @Override
    public String peekPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#peekSuffix()
     */
    @Override
    public String peekSuffix() {
        // TODO Auto-generated method stub
        return null;
    }

}
