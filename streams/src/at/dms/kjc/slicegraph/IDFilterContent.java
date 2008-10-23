package at.dms.kjc.slicegraph;

import at.dms.kjc.*;

public class IDFilterContent extends FilterContent {

    public IDFilterContent() {
        my_unique_ID = unique_ID++;
        name = "SliceID_" + my_unique_ID;
        peek = 1;
    }
    
    public void setInputType(CType type) {
        inputType = type;
    }
    
    public void setOutputType(CType type) {
        outputType = type;
    }
    
    /**
     * Returns push amount.
     */
    public int getPushInt() {
        return 1;
    }

    /**
     * Returns pop amount.
     */
    public int getPopInt() {
        return 1;
    }

    /**
     * Returns peek amount.
     */
    public int getPeekInt() {
        return 1;
    }

    /**
     * Returns push amount of init stage.
     * result may be garbage or error if !isTwoStage()
     */
    public int getPreworkPush() {
        return 0;
    }

    /**
     * Returns pop amount of init stage.
     * result may be garbage or error if !isTwoStage()
     */
    public int getPreworkPop() {
        return 0;
    }

    /**
     * Returns peek amount of init stage.
     * result may be garbage or error if !isTwoStage()
     */
    public int getPreworkPeek() {
        return 0;
    }
  
    /**
     * Create and return a slice with a single ID filter.
     */
    public static Slice createIDSlice() {
        InputSliceNode input = new InputSliceNode();
        OutputSliceNode output = new OutputSliceNode();
        IDFilterContent id = new IDFilterContent();
        FilterSliceNode filter = new FilterSliceNode(id);
        
        input.setNext(filter);
        filter.setPrevious(input);
        filter.setNext(output);
        output.setPrevious(filter);
        
        Slice slice = new Slice(input);
        
        slice.setTail(output);
        
        slice.finish();
        
        return slice;
    }
    
}
