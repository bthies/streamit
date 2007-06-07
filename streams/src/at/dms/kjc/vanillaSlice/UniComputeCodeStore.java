package at.dms.kjc.vanillaSlice;

import at.dms.kjc.common.ALocalVariable;
import java.util.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;

/**
 * Modest extension to {@link at.dms.kjc.backendSupport.ComputeCodeStore}. 
 * @author dimock
  */
public class UniComputeCodeStore extends ComputeCodeStore<UniProcessor> {
    
    /** Construct new ComputeCodeStore for a vanilla processor 
     * @param parent the processor that this ComputeCodeSpore is storing code for. 
     */
    public UniComputeCodeStore(UniProcessor parent) {
        super(parent);
    }

    /**
     * We always use an iteration bound, so override version in superclass. 
     */
    @Override
    protected void addSteadyLoop() {
        ALocalVariable bound = ALocalVariable.makeVar(CStdType.Integer, UniBackEndFactory.iterationBound);
        super.addSteadyLoop(bound);
    }
    
    /**
     * Set name of main routine to be unique per processor.
     * Allows code for multiple processors to be generated into the
     * same scope without having to worry about name clashes.
     */
    @Override
    protected void setMyMainName(String baseName) {
        myMainName = baseName + "_" + parent.getUniqueId();
    }
    
    @Override
    public String getMyMainName() {
        if (myMainName == null || myMainName.equals("")) {
            setMyMainName("_MAIN_");
        }
        return myMainName;
    }
}
