package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.kjc.sir.SIRJoinType;
import at.dms.compiler.*;

/**
 * This gives the run-time system information about the joiner for
 * a feedback loop or split/join structure.  This includes the
 * split policy, the number of branches, and the ratios for a round-robin
 * joiner.
 */
public class LIRSetJoiner extends LIRNode 
{
    /**
     * The type of the joiner.
     */
    private SIRJoinType type;
    
    /**
     * The number of items that the joiner pops from.
     */
    private int ways;
    
    /**
     * For round-robin joiners, the number of items the joiner pops
     * from each input tape in one execution cycle.
     */
    private int[] weights;
    
    public LIRSetJoiner(JExpression streamContext, SIRJoinType type,
                        int ways, int[] weights)
    {
        super(streamContext);
        this.type = type;
        this.ways = ways;
        if (weights == null)
            this.weights = null;
        else
            {
                this.weights = new int[ways];
                for (int i = 0; i < ways; i++)
                    this.weights[i] = weights[i];
            }
    }

    public SIRJoinType getSplitType()
    {
        return type;
    }
    
    public int getWays()
    {
        return ways;
    }

    public void getWeights(int[] weights)
    {
        if (this.weights != null)
            for (int i = 0; i < ways; i++)
                weights[i] = this.weights[i];
    }
    
    public void accept(SLIRVisitor v) 
    {
        int[] weights = null;
        if (this.weights != null)
            {
                weights = new int[ways];
                getWeights(weights);
            }
        v.visitSetJoiner(this, getStreamContext(), type, ways, weights);
    }
}

                           
